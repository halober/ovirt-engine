package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.queries.IdQueryParameters;

public class GetRngDeviceQuery <P extends IdQueryParameters> extends QueriesCommandBase<P> {

    public GetRngDeviceQuery(P parameters) {
        super(parameters);
    }

    public GetRngDeviceQuery(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    @Override
    protected void executeQueryCommand() {
        final List<VmDevice> vmDevices = getDbFacade().getVmDeviceDao().getVmDeviceByVmIdAndType(getParameters().getId(),
                VmDeviceGeneralType.RNG);

        if (vmDevices != null && !vmDevices.isEmpty()) {
            VmDevice dev = vmDevices.get(0);
            setReturnValue(Collections.singletonList(new VmRngDevice(dev)));
        } else {
            setReturnValue(Collections.emptyList());
        }
    }

}