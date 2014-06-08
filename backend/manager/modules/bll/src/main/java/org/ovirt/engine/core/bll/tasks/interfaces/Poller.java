package org.ovirt.engine.core.bll.tasks.interfaces;

import java.util.ArrayList;
import java.util.Map;

import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.compat.Guid;

public interface Poller {

    Map<Guid, AsyncTaskStatus> getAllTasksStatuses(Guid storagePoolID);
    ArrayList<AsyncTaskCreationInfo> getAllTasksInfo(Guid storagePoolID);

}