package com.pinnacle.smsorganizer

import android.app.Application
import com.pinnacle.smsorganizer.data.local.SmsDatabase
import com.pinnacle.smsorganizer.data.provider.ContactReader
import com.pinnacle.smsorganizer.data.provider.SmsProviderReader
import com.pinnacle.smsorganizer.data.repository.SmsRepository
import com.pinnacle.smsorganizer.data.repository.SmsRepositoryImpl

class PinnacleSmsApp : Application() {

    lateinit var database: SmsDatabase
        private set

    lateinit var repository: SmsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = SmsDatabase.getInstance(this)
        val smsProviderReader = SmsProviderReader(this)
        val contactReader = ContactReader(this)

        repository = SmsRepositoryImpl(
            smsProviderReader = smsProviderReader,
            contactReader = contactReader,
            categoryOverrideDao = database.categoryOverrideDao(),
            threadStateDao = database.threadStateDao()
        )
    }
}
