package com.pinnacle.smsorganizer.data.receiver

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Required broadcast receiver for default SMS app compliance on Android.
 * Receives WAP_PUSH_DELIVER for incoming MMS messages.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS push received; stub implementation for default SMS app role compliance
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
