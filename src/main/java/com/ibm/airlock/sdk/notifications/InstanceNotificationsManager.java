package com.ibm.airlock.sdk.notifications;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.engine.AirlockContextManager;
import com.ibm.airlock.common.notifications.NotificationsManager;

/**
 * Created by Denis Voloshin on 29/01/2018.
 */
public class InstanceNotificationsManager extends NotificationsManager {
    public InstanceNotificationsManager(Context context, PersistenceHandler ph, String appVersion, AirlockContextManager airlockScriptScope) {
        super(context, ph, appVersion,airlockScriptScope);
    }

    @Override
    public void scheduleNotificationAlarm(long dueDate){

    }
}
