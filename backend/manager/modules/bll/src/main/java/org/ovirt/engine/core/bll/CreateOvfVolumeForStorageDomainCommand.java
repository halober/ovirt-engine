package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.ovirt.engine.core.bll.storage.StorageDomainCommandBase;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfo;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfoStatus;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dao.StorageDomainOvfInfoDao;
import org.ovirt.engine.core.utils.ovf.OvfInfoFileConstants;

@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class CreateOvfVolumeForStorageDomainCommand<T extends StorageDomainParametersBase> extends StorageDomainCommandBase<T> {
    public CreateOvfVolumeForStorageDomainCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        setStorageDomainId(getParameters().getStorageDomainId());
        setStoragePoolId(getParameters().getStoragePoolId());
    }

    public CreateOvfVolumeForStorageDomainCommand(Guid commandId) {
        super(commandId);
    }

    @Override

    protected void executeCommand() {
        DiskImage createdDisk = createDisk(getStorageDomainId());
        AddDiskParameters diskParameters = new AddDiskParameters(null, createDisk(getStorageDomainId()));
        diskParameters.setStorageDomainId(getStorageDomainId());
        diskParameters.setParentCommand(getParameters().getParentCommand());
        diskParameters.setParentParameters(getParameters().getParentParameters());
        diskParameters.setShouldRemainIllegalOnFailedExecution(true);
        VdcReturnValueBase vdcReturnValueBase =
                runInternalActionWithTasksContext(VdcActionType.AddDisk, diskParameters);
        Guid createdId = (Guid)vdcReturnValueBase.getActionReturnValue();

        if (createdId != null) {
            addStorageDomainOvfInfoToDb(createdId);
        }

        if (!vdcReturnValueBase.getSucceeded()) {
            addCustomValue("DiskAlias", createdDisk.getDiskAlias());
            if (createdId != null) {
                AuditLogDirector.log(this, AuditLogType.CREATE_OVF_STORE_FOR_STORAGE_DOMAIN_FAILED);
            } else {
                AuditLogDirector.log(this, AuditLogType.CREATE_OVF_STORE_FOR_STORAGE_DOMAIN_INITIATE_FAILED);
            }
            setSucceeded(false);
        }

        getReturnValue().getInternalVdsmTaskIdList().addAll(vdcReturnValueBase.getInternalVdsmTaskIdList());
        setSucceeded(true);
    }

    public DiskImage createDisk(Guid domainId) {
        DiskImage mNewCreatedDiskImage = new DiskImage();
        mNewCreatedDiskImage.setDiskInterface(DiskInterface.IDE);
        mNewCreatedDiskImage.setWipeAfterDelete(false);
        mNewCreatedDiskImage.setDiskAlias(OvfInfoFileConstants.OvfStoreDescriptionLabel);
        mNewCreatedDiskImage.setDiskDescription(OvfInfoFileConstants.OvfStoreDescriptionLabel);
        mNewCreatedDiskImage.setShareable(true);
        mNewCreatedDiskImage.setStorageIds(new ArrayList<>(Arrays.asList(domainId)));
        mNewCreatedDiskImage.setSize(SizeConverter.BYTES_IN_MB * 128);
        mNewCreatedDiskImage.setvolumeFormat(VolumeFormat.RAW);
        mNewCreatedDiskImage.setVolumeType(VolumeType.Preallocated);
        mNewCreatedDiskImage.setDescription("OVF store for domain " + domainId);
        Date creationDate = new Date();
        mNewCreatedDiskImage.setCreationDate(creationDate);
        mNewCreatedDiskImage.setLastModified(creationDate);
        return mNewCreatedDiskImage;
    }

    private void addStorageDomainOvfInfoToDb(Guid diskId) {
        StorageDomainOvfInfo storageDomainOvfInfo =
                new StorageDomainOvfInfo(getStorageDomainId(), null, diskId, StorageDomainOvfInfoStatus.DISABLED, null);
        getStorageDomainOvfInfoDao().save(storageDomainOvfInfo);
    }

    protected StorageDomainOvfInfoDao getStorageDomainOvfInfoDao() {
        return getDbFacade().getStorageDomainOvfInfoDao();
    }
}
