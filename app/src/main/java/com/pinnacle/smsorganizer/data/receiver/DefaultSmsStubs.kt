package com.pinnacle.smsorganizer.data.receiver

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Android requires this manifest route for SMS-role eligibility.
 * MMS delivery is intentionally unsupported; no message data is processed here.
 */
class RequiredSmsRoleWapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Deliberately ignored: this app supports SMS only.
    }
}

/**
 * Required service for default SMS app compliance on Android.
 * Handles RESPOND_VIA_MESSAGE (e.g. quick SMS responses to incoming phone calls).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
