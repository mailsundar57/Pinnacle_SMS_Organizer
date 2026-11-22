package com.pinnacle.smsorganizer.data.receiver

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pinnacle.smsorganizer.MainActivity
import com.pinnacle.smsorganizer.PinnacleSmsApp
import com.pinnacle.smsorganizer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver for android.provider.Telephony.SMS_DELIVER.
 * Called exclusively by Android when Pinnacle SMS Organizer holds RoleManager.ROLE_SMS.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val address = messages.first().originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val receivedAt = messages.first().timestampMillis

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, receivedAt)
                    put(Telephony.Sms.DATE_SENT, receivedAt)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.SEEN, 0)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    put(
                        Telephony.Sms.THREAD_ID,
                        Telephony.Threads.getOrCreateThreadId(context, address)
                    )
                }
                context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                showNotification(context, address, body, receivedAt)
            } catch (_: SecurityException) {
                // Can occur if the user revoked SMS access while the app was backgrounded.
            } catch (_: IllegalArgumentException) {
                // Ignore malformed carrier broadcasts rather than crashing the receiver.
            } finally {
                try {
                    (context.applicationContext as? PinnacleSmsApp)?.repository?.refresh()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, address: String, body: String, id: Long) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "SMS messages",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val openApp = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
            // POST_NOTIFICATIONS can be denied independently of SMS permissions.
        }
    }

    private companion object {
        const val CHANNEL_ID = "incoming_sms"
    }
}

/**
 * Receiver for android.provider.Telephony.SMS_RECEIVED.
 * Refreshes the inbox live in Read-Only mode when Pinnacle is NOT the default SMS app.
 */
class SmsObservedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) {
            // SmsReceiver already handles SMS_DELIVER and refreshes when default.
            return
        }
        val app = context.applicationContext as? PinnacleSmsApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class SmsSendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SENT) return
        val status = if (resultCode == Activity.RESULT_OK) {
            Telephony.Sms.MESSAGE_TYPE_SENT
        } else {
            Telephony.Sms.MESSAGE_TYPE_FAILED
        }
        Toast.makeText(
            context,
            if (status == Telephony.Sms.MESSAGE_TYPE_SENT) "SMS sent" else "SMS failed to send",
            Toast.LENGTH_LONG
        ).show()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                intent.data?.let { uri ->
                    try {
                        context.contentResolver.update(
                            uri,
                            ContentValues().apply {
                                put(Telephony.Sms.TYPE, status)
                                put(Telephony.Sms.DATE, System.currentTimeMillis())
                            },
                            null,
                            null
                        )
                    } catch (_: SecurityException) {
                    }
                }
                (context.applicationContext as? PinnacleSmsApp)?.repository?.refresh()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SENT = "com.pinnacle.smsorganizer.SMS_SENT"
    }
}
