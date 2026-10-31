package com.pinnacle.smsorganizer.ui.compose

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.telephony.SmsManager
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinnacle.smsorganizer.ui.theme.PinnacleSMSOrganizerTheme

class ComposeSmsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse recipient and message body from intent
        var initialRecipient = ""
        var initialBody = ""

        intent?.data?.let { uri ->
            if (uri.scheme == "smsto" || uri.scheme == "sms") {
                initialRecipient = uri.schemeSpecificPart ?: ""
            }
        }

        if (intent?.hasExtra("address") == true) {
            initialRecipient = intent.getStringExtra("address") ?: initialRecipient
        }
        if (intent?.hasExtra(Intent.EXTRA_TEXT) == true) {
            initialBody = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        }
        if (intent?.hasExtra("sms_body") == true) {
            initialBody = intent.getStringExtra("sms_body") ?: initialBody
        }

        setContent {
            PinnacleSMSOrganizerTheme {
                var recipient by remember { mutableStateOf(initialRecipient) }
                var messageBody by remember { mutableStateOf(initialBody) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            title = { Text("New Message") }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { recipient = it },
                            label = { Text("To (Phone number)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = messageBody,
                            onValueChange = { messageBody = it },
                            label = { Text("Message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                sendSms(recipient, messageBody)
                            },
                            enabled = recipient.isNotBlank() && messageBody.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Send SMS")
                        }
                    }
                }
            }
        }
    }

    private fun sendSms(phoneNumber: String, text: String) {
        if (Telephony.Sms.getDefaultSmsPackage(this) != packageName) {
            Toast.makeText(this, "Set Pinnacle as the default SMS app before sending.", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS sending permission is not granted.", Toast.LENGTH_LONG).show()
            return
        }
        if (phoneNumber.isBlank() || text.isBlank()) return
        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val sentUri = contentResolver.insert(Telephony.Sms.Outbox.CONTENT_URI,
                android.content.ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, phoneNumber)
                    put(Telephony.Sms.BODY, text)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
                    put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(this@ComposeSmsActivity, phoneNumber))
                })
            val sentIntent = Intent(this, com.pinnacle.smsorganizer.data.receiver.SmsSendResultReceiver::class.java)
                .setAction(com.pinnacle.smsorganizer.data.receiver.SmsSendResultReceiver.ACTION_SENT)
                .setData(sentUri)
            val sentPendingIntent = android.app.PendingIntent.getBroadcast(
                this, (sentUri?.lastPathSegment?.hashCode() ?: System.currentTimeMillis().toInt()), sentIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val parts = smsManager.divideMessage(text)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, text, sentPendingIntent, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts,
                    ArrayList(List(parts.size) { sentPendingIntent }), null)
            }
            Toast.makeText(this, "SMS queued for sending.", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
