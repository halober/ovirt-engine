package org.ovirt.engine.core.bll.validator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainDynamic;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class StorageDomainValidator {

    private static final double QCOW_OVERHEAD_FACTOR = 1.1;
    private static final long INITIAL_BLOCK_ALLOCATION_SIZE = 1024L * 1024L * 1024L;
    private static final long EMPTY_QCOW_HEADER_SIZE = 1024L * 1024L;

    private StorageDomain storageDomain;

    public StorageDomainValidator(StorageDomain domain) {
        storageDomain = domain;
    }

    public ValidationResult isDomainExistAndActive() {
        if (storageDomain == null) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_NOT_EXIST);
        }
        if (storageDomain.getStatus() != StorageDomainStatus.Active) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2,
                    String.format("$%1$s %2$s", "status", storageDomain.getStatus().name()));
        }
        return ValidationResult.VALID;
    }

    public ValidationResult domainIsValidDestination() {
        if (storageDomain.getStorageDomainType().isIsoOrImportExportDomain()) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_TYPE_ILLEGAL);
        }
        return ValidationResult.VALID;
    }

    public ValidationResult isDomainWithinThresholds() {
        StorageDomainDynamic dynamic = storageDomain.getStorageDynamicData();
        if (dynamic != null && dynamic.getfreeDiskInGB() < getLowDiskSpaceThreshold()) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN,
                    storageName());
        }
        return ValidationResult.VALID;
    }

    private String storageName() {
        return String.format("$%1$s %2$s", "storageName", storageDomain.getStorageName());
    }

    /**
     * @deprecated
     * This validation is replaced by hadSpaceForClonedDisks,hadSpaceForClonedDisk, hasSpaceForNewDisks and
     * hasSpaceForNewDisk, according to the situation.
     */
    @Deprecated
    public ValidationResult isDomainHasSpaceForRequest(final long requestedSize) {
        return isDomainHasSpaceForRequest(requestedSize, true);
    }

    /**
     * @deprecated
     * This validation is replaced by hadSpaceForClonedDisks,hadSpaceForClonedDisk, hasSpaceForNewDisks and
     * hasSpaceForNewDisk, according to the situation.
     */
    @Deprecated
    public ValidationResult isDomainHasSpaceForRequest(final long requestedSize, final boolean useThresHold) {
        long size = useThresHold ? getLowDiskSpaceThreshold() : 0L;
        if (storageDomain.getAvailableDiskSize() != null &&
                storageDomain.getAvailableDiskSize() - requestedSize < size) {
            return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN,
                    storageName());
        }
        return ValidationResult.VALID;
    }

    public ValidationResult isDomainHasNoImages(VdcBllMessages message, boolean allowOvfDisk) {
        int numOfAllowedDisks = 0;
        if (allowOvfDisk) {
            numOfAllowedDisks = getDbFacade().getStorageDomainOvfInfoDao().getAllForDomain(storageDomain.getId()).size();
        }

        if (getDbFacade().getDiskImageDao().getAllSnapshotsForStorageDomain(storageDomain.getId()).size() > numOfAllowedDisks) {
            return new ValidationResult(message);
        }

        return ValidationResult.VALID;
    }

    private DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }

    private static Integer getLowDiskSpaceThreshold() {
        return Config.<Integer> getValue(ConfigValues.FreeSpaceCriticalLowInGB);
    }

    /**
     * Verify there's enough space in the storage domain for creating new DiskImages.
     * Some space should be allocated on the storage domain according to the volumes type and format, and allocation policy,
     * according to the following table:
     *
     *      | File Domain                             | Block Domain
     * -----|-----------------------------------------|-------------
     * qcow | 1M (header size)                        | 1G
     * -----|-----------------------------------------|-------------
     * raw  | preallocated: disk capacity (getSize()) | disk capacity
     *      | thin (sparse): 1M                       | (there is no raw sparse on
     *      |                                         | block domains)
     *
     */
    private double getTotalSizeForNewDisks(Collection<DiskImage> diskImages) {
        double totalSizeForDisks = 0.0;
        if (diskImages != null) {
            for (DiskImage diskImage : diskImages) {
                double sizeForDisk = diskImage.getSize();

                if (diskImage.getVolumeFormat() == VolumeFormat.COW) {
                    if (storageDomain.getStorageType().isFileDomain()) {
                        sizeForDisk = EMPTY_QCOW_HEADER_SIZE;
                    } else {
                        sizeForDisk = INITIAL_BLOCK_ALLOCATION_SIZE;
                    }
                } else if (diskImage.getVolumeType() == VolumeType.Sparse) {
                    sizeForDisk = EMPTY_QCOW_HEADER_SIZE;
                }
                totalSizeForDisks += sizeForDisk;
            }
        }
        return totalSizeForDisks;
    }

    /**
     * Verify there's enough space in the storage domain for creating cloned DiskImages.
     * Space should be allocated according to the volumes type and format, and allocation policy,
     * according to the following table:
     *
     *      | File Domain                             | Block Domain
     * -----|-----------------------------------------|-------------
     * qcow | preallocated : 1.1 * disk capacity      |1.1 * min(used ,capacity)
     *      | sparse: 1.1 * min(used ,capacity)       |
     * -----|-----------------------------------------|-------------
     * raw  | preallocated: disk capacity             |disk capacity
     *      | sparse: min(used,capacity)              |
     *
     * */
    private double getTotalSizeForClonedDisks(Collection<DiskImage> diskImages) {
        double totalSizeForDisks = 0.0;
        if (diskImages != null) {
            for (DiskImage diskImage : diskImages) {
                double diskCapacity = diskImage.getSize();
                double sizeForDisk = diskCapacity;

                if ((storageDomain.getStorageType().isFileDomain() && diskImage.getVolumeType() == VolumeType.Sparse) ||
                        storageDomain.getStorageType().isBlockDomain() && diskImage.getVolumeFormat() == VolumeFormat.COW) {
                    double usedSapce = diskImage.getActualDiskWithSnapshotsSizeInBytes();
                    sizeForDisk = Math.min(diskCapacity, usedSapce);
                }

                if (diskImage.getVolumeFormat() == VolumeFormat.COW) {
                    sizeForDisk = Math.ceil(QCOW_OVERHEAD_FACTOR * sizeForDisk);
                }
                totalSizeForDisks += sizeForDisk;
            }
        }
        return totalSizeForDisks;
    }

    public ValidationResult hasSpaceForNewDisks(Collection<DiskImage> diskImages) {
        double availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForDisks = getTotalSizeForNewDisks(diskImages);

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    public ValidationResult hasSpaceForClonedDisks(Collection<DiskImage> diskImages) {
        double availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForDisks = getTotalSizeForClonedDisks(diskImages);

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    public ValidationResult hasSpaceForAllDisks(Collection<DiskImage> newDiskImages, Collection<DiskImage> clonedDiskImages) {
        double availableSize = storageDomain.getAvailableDiskSizeInBytes();
        double totalSizeForNewDisks = getTotalSizeForNewDisks(newDiskImages);
        double totalSizeForClonedDisks = getTotalSizeForClonedDisks(clonedDiskImages);
        double totalSizeForDisks = totalSizeForNewDisks + totalSizeForClonedDisks;

        return validateRequiredSpace(availableSize, totalSizeForDisks);
    }

    public ValidationResult hasSpaceForClonedDisk(DiskImage diskImage) {
        return hasSpaceForClonedDisks(Collections.singleton(diskImage));
    }

    public ValidationResult hasSpaceForNewDisk(DiskImage diskImage) {
        return hasSpaceForNewDisks(Collections.singleton(diskImage));
    }

    private ValidationResult validateRequiredSpace(double availableSize, double requiredSize) {
        if (availableSize >= requiredSize) {
            return ValidationResult.VALID;
        }

        return new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_DISK_SPACE_LOW_ON_STORAGE_DOMAIN,
                storageName());
    }

    /**
     * @deprecated
     * This validation is replaced by hadSpaceForClonedDisks,hadSpaceForClonedDisk, hasSpaceForNewDisks and
     * hasSpaceForNewDisk, according to the situation.
     */
    @Deprecated
    public static Map<StorageDomain, Integer> getSpaceRequirementsForStorageDomains(Collection<DiskImage> images,
            Map<Guid, StorageDomain> storageDomains, Map<Guid, DiskImage> imageToDestinationDomainMap) {
        Map<DiskImage, StorageDomain> spaceMap = new HashMap<DiskImage, StorageDomain>();
        for (DiskImage image : images) {
            Guid storageId = imageToDestinationDomainMap.get(image.getId()).getStorageIds().get(0);
            StorageDomain domain = storageDomains.get(storageId);
            if (domain == null) {
                domain = DbFacade.getInstance().getStorageDomainDao().get(storageId);
            }
            spaceMap.put(image, domain);
        }
        return StorageDomainValidator.getSpaceRequirementsForStorageDomains(spaceMap);
    }

    /**
     * @deprecated
     * This validation is replaced by hadSpaceForClonedDisks,hadSpaceForClonedDisk, hasSpaceForNewDisks and
     * hasSpaceForNewDisk, according to the situation.
     */
    @Deprecated
    public static Map<StorageDomain, Integer> getSpaceRequirementsForStorageDomains(Map<DiskImage, StorageDomain> imageToDomainMap) {
        Map<StorageDomain, Integer> map = new HashMap<StorageDomain, Integer>();
        if (!imageToDomainMap.isEmpty()) {
            for (Map.Entry<DiskImage, StorageDomain> entry : imageToDomainMap.entrySet()) {
                StorageDomain domain = entry.getValue();
                int size = (int) entry.getKey().getActualSize();
                if (map.containsKey(domain)) {
                    map.put(domain, map.get(domain) + size);
                } else {
                    map.put(domain, size);
                }
            }
        }
        return map;
    }

    /**
     * Calculates the requires space for removing a set of disk snapshots consequently.
     * The calculation takes into account that the required space (for each snapshots merge)
     * couldn't be larger than either of disk's virtual size and sum of the actual sizes.
     * I.e. the minimum between values.
     *
     * @param diskImages
     * @return the required space
     */
    public ValidationResult hasSpaceForRemovingDiskSnapshots(Collection<DiskImage> diskImages) {
        // Sums the actual sizes and finds the max virtual size
        long sumOfActualSizes = 0L;
        long maxVirtualSize = 0L;
        for (DiskImage image : diskImages) {
            sumOfActualSizes += (long) image.getActualSize();
            maxVirtualSize = Math.max(maxVirtualSize, image.getSize());
        }

        return isDomainHasSpaceForRequest(Math.min(maxVirtualSize, sumOfActualSizes), false);
    }
}
