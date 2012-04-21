package org.ovirt.engine.ui.common.widget.uicommon.popup.vm;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.storage_domains;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Event;
import org.ovirt.engine.core.compat.EventArgs;
import org.ovirt.engine.core.compat.IEventListener;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.EntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.DiskSizeColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VmDiskPopupWidget extends AbstractModelBoundPopupWidget<DiskModel> {

    interface Driver extends SimpleBeanEditorDriver<DiskModel, VmDiskPopupWidget> {
        Driver driver = GWT.create(Driver.class);
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, VmDiskPopupWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    @Path("size.entity")
    EntityModelTextBoxEditor sizeEditor;

    @UiField
    @Path("alias.entity")
    EntityModelTextBoxEditor aliasEditor;

    @UiField
    @Path("description.entity")
    EntityModelTextBoxEditor descriptionEditor;

    @UiField(provided = true)
    @Path("dataCenter.selectedItem")
    ListModelListBoxEditor<Object> datacenterEditor;

    @UiField(provided = true)
    @Path("storageDomain.selectedItem")
    ListModelListBoxEditor<Object> storageDomainEditor;

    @UiField(provided = true)
    @Path("quota.selectedItem")
    ListModelListBoxEditor<Object> quotaEditor;

    @UiField(provided = true)
    @Path("interface.selectedItem")
    ListModelListBoxEditor<Object> interfaceEditor;

    @UiField(provided = true)
    @Path("volumeType.selectedItem")
    ListModelListBoxEditor<Object> volumeTypeEditor;

    @UiField(provided = true)
    @Path("wipeAfterDelete.entity")
    EntityModelCheckBoxEditor wipeAfterDeleteEditor;

    @UiField(provided = true)
    @Path("isBootable.entity")
    EntityModelCheckBoxEditor isBootableEditor;

    @UiField(provided = true)
    @Path("isPlugged.entity")
    EntityModelCheckBoxEditor isPluggedEditor;

    @UiField(provided = true)
    @Path("attachDisk.entity")
    EntityModelCheckBoxEditor attachEditor;

    @UiField
    VerticalPanel createDiskPanel;

    @UiField
    VerticalPanel attachDiskPanel;

    @UiField
    HorizontalPanel topPanel;

    @UiField(provided = true)
    @Ignore
    EntityModelCellTable<ListModel> diskTable;

    @UiField
    Label message;

    public VmDiskPopupWidget(CommonApplicationConstants constants) {
        initManualWidgets();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
        initDiskTable(constants);
        Driver.driver.initialize(this);
    }

    // TODO: Localize
    private void localize(CommonApplicationConstants constants) {
        aliasEditor.setLabel(constants.aliasVmDiskPopup());
        sizeEditor.setLabel(constants.sizeVmDiskPopup());
        descriptionEditor.setLabel(constants.descriptionVmDiskPopup());
        datacenterEditor.setLabel(constants.dcVmDiskPopup());
        storageDomainEditor.setLabel(constants.storageDomainVmDiskPopup());
        quotaEditor.setLabel(constants.quotaVmDiskPopup());
        interfaceEditor.setLabel(constants.interfaceVmDiskPopup());
        volumeTypeEditor.setLabel(constants.formatVmDiskPopup());
        wipeAfterDeleteEditor.setLabel(constants.wipeAfterDeleteVmDiskPopup());
        isBootableEditor.setLabel(constants.isBootableVmDiskPopup());
        attachEditor.setLabel(constants.attachDiskVmDiskPopup());
        isPluggedEditor.setLabel(constants.activateVmDiskPopup());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initManualWidgets() {
        storageDomainEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((storage_domains) object).getstorage_name();
            }
        });

        quotaEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((Quota) object).getQuotaName();
            }
        });

        interfaceEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return object.toString();
            }
        });

        datacenterEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                return ((storage_pool) object).getname();
            }
        });

        volumeTypeEditor = new ListModelListBoxEditor<Object>(new EnumRenderer());

        wipeAfterDeleteEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isBootableEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        isPluggedEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        attachEditor = new EntityModelCheckBoxEditor(Align.RIGHT);

        diskTable = new EntityModelCellTable<ListModel>(true);
    }

    private void initDiskTable(CommonApplicationConstants constants) {
        TextColumnWithTooltip<EntityModel> aliasColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel object) {
                return ((DiskModel) (object.getEntity())).getDiskImage().getDiskAlias();
            }
        };
        diskTable.addColumn(aliasColumn, constants.aliasVmDiskTable());

        DiskSizeColumn<EntityModel> sizeColumn = new DiskSizeColumn<EntityModel>() {
            @Override
            protected Long getRawValue(EntityModel object) {
                return ((DiskModel) (object.getEntity())).getDiskImage().getsize();
            }
        };
        diskTable.addColumn(sizeColumn, constants.provisionedSizeVmDiskTable());

        DiskSizeColumn<EntityModel> actualSizeColumn = new DiskSizeColumn<EntityModel>() {
            @Override
            protected Long getRawValue(EntityModel object) {
                return ((DiskModel) (object.getEntity())).getDiskImage().getactual_size();
            }
        };
        diskTable.addColumn(actualSizeColumn, constants.sizeVmDiskTable());

        TextColumnWithTooltip<EntityModel> storageDomainColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel object) {
                return ((DiskModel) (object.getEntity())).getDiskImage().getStoragesNames().get(0);
            }
        };
        diskTable.addColumn(storageDomainColumn, constants.storageDomainVmDiskTable());

        diskTable.setWidth("100%", true); //$NON-NLS-1$
        diskTable.setHeight("100%"); //$NON-NLS-1$
    }

    @Override
    public void focusInput() {
        sizeEditor.setFocus(true);
    }

    @Override
    public void edit(DiskModel disk) {
        Driver.driver.edit(disk);

        disk.getAttachDisk().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isAttach = (Boolean) ((EntityModel) sender).getEntity();
                createDiskPanel.setVisible(!isAttach);
                attachDiskPanel.setVisible(isAttach);
            }
        });

        disk.getIsInVm().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean isInVm = (Boolean) ((EntityModel) sender).getEntity();
                topPanel.setVisible(isInVm);
                aliasEditor.setFocus(!isInVm);
            }
        });

        diskTable.edit(disk.getAttachableDisks());
    }

    @Override
    public DiskModel flush() {
        return Driver.driver.flush();
    }

}
