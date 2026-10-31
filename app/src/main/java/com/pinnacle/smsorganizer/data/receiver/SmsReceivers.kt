package com.pinnacle.smsorganizer.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.pinnacle.smsorganizer.PinnacleSmsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver for android.provider.Telephony.SMS_DELIVER.
 * Only called when Pinnacle SMS Organizer is the system's DEFAULT SMS app.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!messages.isNullOrEmpty()) {
                // Refresh repository
                val app = context.applicationContext as? PinnacleSmsApp
                app?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        it.repository.refresh()
                    }
                }
            }
        }
    }
}

/**
 * Receiver for android.provider.Telephony.SMS_RECEIVED.
 * Called when a new SMS arrives, regardless of whether the app is default SMS app.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val app = context.applicationContext as? PinnacleSmsApp
            app?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.repository.refresh()
                }
            }
        }
    }
}
