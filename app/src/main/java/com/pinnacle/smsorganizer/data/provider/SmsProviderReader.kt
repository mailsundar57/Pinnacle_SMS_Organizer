package com.pinnacle.smsorganizer.data.provider

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class RawSms(
    val id: Long,
    val threadId: Long,
    val address: String,
    val date: Long,
    val body: String,
    val isRead: Boolean,
    val isFromMe: Boolean
)

open class SmsProviderReader(private val context: Context?) {

    open fun hasReadSmsPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    open fun isDefaultSmsApp(): Boolean {
        val ctx = context ?: return false
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(ctx)
        return ctx.packageName.equals(defaultPackage, ignoreCase = true)
    }

    open fun queryAllMessages(): List<RawSms> {
        val ctx = context ?: return emptyList()
        if (!hasReadSmsPermission()) {
            return emptyList()
        }

        val messages = mutableListOf<RawSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            val cursor: Cursor? = ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val threadId = if (threadIdx != -1) it.getLong(threadIdx) else 0L
                    val address = if (addrIdx != -1) it.getString(addrIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val read = if (readIdx != -1) it.getInt(readIdx) == 1 else true
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    val isFromMe = (type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX)

                    messages.add(
                        RawSms(
                            id = id,
                            threadId = threadId,
                            address = address,
                            date = date,
                            body = body,
                            isRead = read,
                            isFromMe = isFromMe
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Guard against SecurityException or cursor query failures
        }

        return messages
    }

    open fun queryMessagesForThread(threadId: Long): List<RawSms> {
        val ctx = context ?: return emptyList()
        if (!hasReadSmsPermission()) {
            return emptyList()
        }

        val messages = mutableListOf<RawSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        try {
            val cursor: Cursor? = ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val tId = if (threadIdx != -1) it.getLong(threadIdx) else threadId
                    val address = if (addrIdx != -1) it.getString(addrIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val read = if (readIdx != -1) it.getInt(readIdx) == 1 else true
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    val isFromMe = (type == Telephony.Sms.MESSAGE_TYPE_SENT || type == Telephony.Sms.MESSAGE_TYPE_OUTBOX)

                    messages.add(
                        RawSms(
                            id = id,
                            threadId = tId,
                            address = address,
                            date = date,
                            body = body,
                            isRead = read,
                            isFromMe = isFromMe
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Guard against SecurityException
        }

        return messages
    }

    /**
     * Attempts to update read state in the Android system Telephony SMS provider.
     * Returns true if successfully updated, or false if not permitted (non-default SMS app).
     */
    open fun markAsReadInSystem(threadId: Long, isRead: Boolean): Boolean {
        val ctx = context ?: return false
        if (!isDefaultSmsApp()) {
            return false
        }

        return try {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, if (isRead) 1 else 0)
            }
            val selection = "${Telephony.Sms.THREAD_ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            val rows = ctx.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                selection,
                selectionArgs
            )
            rows > 0
        } catch (e: Exception) {
            false
        }
    }
}
