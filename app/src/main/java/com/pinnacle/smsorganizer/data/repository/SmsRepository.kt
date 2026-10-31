package com.pinnacle.smsorganizer.data.repository

import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.data.model.SmsMessage
import kotlinx.coroutines.flow.Flow

interface SmsRepository {

    fun getConversations(
        categoryFilter: SmsCategory? = null,
        searchQuery: String? = null
    ): Flow<List<SmsConversation>>

    fun getMessagesForThread(threadId: Long): Flow<List<SmsMessage>>

    fun getOtps(): Flow<List<SmsMessage>>

    fun observeCategoryOverrides(): Flow<Map<String, SmsCategory>>

    suspend fun overrideCategory(senderAddress: String, category: SmsCategory)

    suspend fun removeCategoryOverride(senderAddress: String)

    suspend fun markConversationRead(threadId: Long, isRead: Boolean)

    suspend fun togglePinConversation(threadId: Long, isPinned: Boolean)

    fun hasSmsPermission(): Boolean

    fun isDefaultSmsApp(): Boolean

    fun isDemoModeEnabled(): Flow<Boolean>

    fun setDemoMode(enabled: Boolean)

    suspend fun refresh()
}
