/*****************************************************************************
 ** Copyright (c) 2010 - 2012 Ushahidi Inc
 ** All rights reserved
 ** Contact: team@ushahidi.com
 ** Website: http://www.ushahidi.com
 **
 ** GNU Lesser General Public License Usage
 ** This file may be used under the terms of the GNU Lesser
 ** General Public License version 3 as published by the Free Software
 ** Foundation and appearing in the file LICENSE.LGPL included in the
 ** packaging of this file. Please review the following information to
 ** ensure the GNU Lesser General Public License version 3 requirements
 ** will be met: http://www.gnu.org/licenses/lgpl.html.
 **
 **
 ** If you have questions regarding the use of this file, please contact
 ** Ushahidi developers at team@ushahidi.com.
 **
 *****************************************************************************/

package org.addhen.smssync.services;

import static org.addhen.smssync.tasks.SyncType.MANUAL;
import static org.addhen.smssync.tasks.SyncType.REGULAR;
import static org.addhen.smssync.tasks.state.SyncState.ERROR;
import static org.addhen.smssync.tasks.state.SyncState.INITIAL;

import org.addhen.smssync.MainApplication;
import org.addhen.smssync.MessageType;
import org.addhen.smssync.R;
import org.addhen.smssync.tasks.SyncConfig;
import org.addhen.smssync.tasks.SyncTask;
import org.addhen.smssync.tasks.state.MessageSyncState;
import org.addhen.smssync.util.Logger;
import org.addhen.smssync.util.ServicesConstants;

import android.content.Intent;

import com.squareup.otto.Subscribe;

/**
 * This will sync pending messages as it's commanded by the user.
 * 
 * @author eyedol
 */
public class SyncPendingMessagesService extends SmsSyncServices {

    private static String CLASS_TAG = SyncPendingMessagesService.class
            .getSimpleName();

    private String messageUuid = "";

    private MessageSyncState mState = new MessageSyncState();

    private static SyncPendingMessagesService service;

    public SyncPendingMessagesService() {
        super(CLASS_TAG);
        service = this;
        MainApplication.bus.register(this);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.bus.register(this);
    }

    @Override
    protected void executeTask(Intent intent) {

        if (intent != null) {
            // get Id
            messageUuid = intent.getStringExtra(ServicesConstants.MESSAGE_UUID);
            Logger.log(CLASS_TAG, "messageUUid: " + messageUuid);

            Logger.log(CLASS_TAG, "executeTask() executing this task ");
            if (!isWorking()) {
                if (!SyncPendingMessagesService.isServiceWorking()) {
                    log("Sync started");
                    mState = new MessageSyncState(INITIAL, 0, 0, REGULAR, MessageType.PENDING,
                            null);
                    try {
                        SyncConfig config = new SyncConfig(3, false, messageUuid, REGULAR);
                        new SyncTask(this, MessageType.PENDING).execute(config);
                    } catch (Exception e) {
                        log("lNot syncing " + e.getMessage());
                        MainApplication.bus.post(mState.transition(ERROR, e));
                    }
                }
                else {
                    log("Sync is running now.");
                    MainApplication.bus.post(mState.transition(ERROR, null));
                }
            }
            else {
                log("Sync already running");
            }

        }

    }

    @Subscribe
    public void syncStateChanged(final MessageSyncState state) {
        mState = state;
        if (mState.isInitialState())
            return;

        if (state.isError()) {

            createNotification(R.string.status,
                    state.getNotification(getResources()), getPendingIntent());
        }

        if (state.isRunning()) {
            if (state.syncType == MANUAL) {
                updateSyncStatusNotification(state);
            }
        } else {
            log(state.isCanceled() ? getString(R.string.canceled) : getString(R.string.done));

            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public MessageSyncState getState() {
        return mState;

    }

    private void updateSyncStatusNotification(MessageSyncState state) {
        createNotification(R.string.status,
                state.getNotification(getResources()), getPendingIntent());

    }

    public boolean isWorking() {
        return getState().isRunning();
    }

    public static boolean isServiceWorking() {
        return service != null && service.isWorking();
    }

}
