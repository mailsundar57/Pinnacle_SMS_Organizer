package com.pinnacle.smsorganizer.data.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

open class ContactReader(private val context: Context?) {

    private val contactNameCache = mutableMapOf<String, String>()

    open fun getContactName(phoneNumber: String): String? {
        val ctx = context ?: return null
        if (phoneNumber.isBlank()) return null
        contactNameCache[phoneNumber]?.let { return it }

        if (ContextCompat.checkSelfPermission(
                ctx,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor: Cursor? = ctx.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            contactNameCache[phoneNumber] = name
                            return name
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            // Guard against SecurityException or SQLite cursor exceptions
            null
        }
    }

    fun clearCache() {
        contactNameCache.clear()
    }
}
