package org.ovirt.engine.ui.webadmin.widget.action;

import org.ovirt.engine.ui.common.widget.action.UiCommandButtonDefinition;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;

import com.google.gwt.event.shared.EventBus;

public abstract class WebAdminButtonDefinition<T> extends UiCommandButtonDefinition<T> {

    public WebAdminButtonDefinition(String title,
            boolean implInWebAdmin,
            boolean implInUserPortal,
            boolean availableOnlyFromContext,
            boolean subTitledAction,
            String toolTip) {
        super(getEventBus(), title, implInWebAdmin, implInUserPortal,
                availableOnlyFromContext, subTitledAction, toolTip);
    }

    public WebAdminButtonDefinition(String title, boolean availableOnlyFromContext) {
        super(getEventBus(), title, availableOnlyFromContext);
    }

    public WebAdminButtonDefinition(String title) {
        super(getEventBus(), title);
    }

    static EventBus getEventBus() {
        return ClientGinjectorProvider.instance().getEventBus();
    }

}
