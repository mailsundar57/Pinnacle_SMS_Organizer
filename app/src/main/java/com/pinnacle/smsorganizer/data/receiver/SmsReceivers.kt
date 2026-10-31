package com.pinnacle.smsorganizer.data.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.widget.Toast
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pinnacle.smsorganizer.MainActivity
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
                val address = messages.first().originatingAddress.orEmpty()
                val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                val receivedAt = messages.first().timestampMillis
                try {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, address)
                        put(Telephony.Sms.BODY, body)
                        put(Telephony.Sms.DATE, receivedAt)
                        put(Telephony.Sms.DATE_SENT, receivedAt)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                        put(Telephony.Sms.THREAD_ID,
                            Telephony.Threads.getOrCreateThreadId(context, address))
                    }
                    context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    showNotification(context, address, body, receivedAt)
                } catch (_: SecurityException) {
                    // This can happen if the user revoked SMS access while the app was backgrounded.
                } catch (_: IllegalArgumentException) {
                    // Ignore malformed carrier broadcasts rather than crashing the receiver.
                } finally {
                    val app = context.applicationContext as? PinnacleSmsApp
                    app?.let { application ->
                        CoroutineScope(Dispatchers.IO).launch { application.repository.refresh() }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, address: String, body: String, id: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "SMS messages", NotificationManager.IMPORTANCE_HIGH
            ))
        }
        val openApp = PendingIntent.getActivity(
            context, id.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.pinnacle.smsorganizer.R.drawable.ic_launcher_foreground)
            .setContentTitle(address.ifBlank { "New message" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ can deny notifications independently of SMS access.
        }
    }

    private companion object { const val CHANNEL_ID = "incoming_sms" }
}

class SmsSendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SENT) return
        val status = if (resultCode == android.app.Activity.RESULT_OK) {
            Telephony.Sms.MESSAGE_TYPE_SENT
        } else {
            Telephony.Sms.MESSAGE_TYPE_FAILED
        }
        intent.data?.let { uri ->
            try {
                context.contentResolver.update(uri, ContentValues().apply {
                    put(Telephony.Sms.TYPE, status)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                }, null, null)
            } catch (_: SecurityException) { }
        }
        Toast.makeText(context,
            if (status == Telephony.Sms.MESSAGE_TYPE_SENT) "SMS sent" else "SMS failed to send",
            Toast.LENGTH_LONG).show()
        (context.applicationContext as? PinnacleSmsApp)?.let { app ->
            CoroutineScope(Dispatchers.IO).launch { app.repository.refresh() }
        }
    }

    companion object { const val ACTION_SENT = "com.pinnacle.smsorganizer.SMS_SENT" }
}

