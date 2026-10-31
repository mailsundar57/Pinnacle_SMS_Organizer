package com.pinnacle.smsorganizer.data.repository

import com.pinnacle.smsorganizer.data.local.CategoryOverrideDao
import com.pinnacle.smsorganizer.data.local.CategoryOverrideEntity
import com.pinnacle.smsorganizer.data.local.ThreadStateDao
import com.pinnacle.smsorganizer.data.local.ThreadStateEntity
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.data.model.SmsMessage
import com.pinnacle.smsorganizer.data.provider.ContactReader
import com.pinnacle.smsorganizer.data.provider.SmsProviderReader
import com.pinnacle.smsorganizer.domain.classifier.OtpExtractor
import com.pinnacle.smsorganizer.domain.classifier.SmsClassifier
import com.pinnacle.smsorganizer.domain.classifier.TransactionExtractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SmsRepositoryImpl(
    private val smsProviderReader: SmsProviderReader,
    private val contactReader: ContactReader,
    private val categoryOverrideDao: CategoryOverrideDao,
    private val threadStateDao: ThreadStateDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SmsRepository {

    private val demoModeFlow = MutableStateFlow(false)
    private val refreshTrigger = MutableStateFlow(0L)

    override fun hasSmsPermission(): Boolean {
        return smsProviderReader.hasReadSmsPermission() && smsProviderReader.isDefaultSmsApp()
    }

    override fun isDefaultSmsApp(): Boolean {
        return smsProviderReader.isDefaultSmsApp()
    }

    override fun isDemoModeEnabled(): Flow<Boolean> = demoModeFlow

    override fun setDemoMode(enabled: Boolean) {
        demoModeFlow.value = enabled
    }

    override suspend fun refresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun observeCategoryOverrides(): Flow<Map<String, SmsCategory>> {
        return categoryOverrideDao.observeAllOverrides().map { entities ->
            entities.associate { it.senderAddress.uppercase() to SmsCategory.fromString(it.category) }
        }
    }

    override fun getConversations(
        categoryFilter: SmsCategory?,
        searchQuery: String?
    ): Flow<List<SmsConversation>> {
        return combine(
            categoryOverrideDao.observeAllOverrides(),
            threadStateDao.observeAllThreadStates(),
            demoModeFlow,
            refreshTrigger
        ) { overrides, threadStates, isDemo, _ ->
            val overrideMap = overrides.associate {
                it.senderAddress.trim().uppercase() to SmsCategory.fromString(it.category)
            }
            val threadStateMap = threadStates.associateBy { it.threadId }

            // Decide whether to use real SMS or sample data
            val rawMessages = if (isDemo || !hasSmsPermission() || !isDefaultSmsApp()) {
                SampleDataProvider.getSampleMessages()
            } else {
                fetchRealMessages(overrideMap)
            }

            // Group into conversations
            val groupedByThread = rawMessages.groupBy { it.threadId }

            val conversations = groupedByThread.map { (threadId, messages) ->
                val sortedMessages = messages.sortedByDescending { it.date }
                val latest = sortedMessages.first()
                val threadState = threadStateMap[threadId]

                val isRead = threadState?.isReadOverride ?: sortedMessages.all { it.isRead }
                val unreadCount = if (isRead) 0 else sortedMessages.count { !it.isRead }
                val isPinned = threadState?.isPinned ?: false

                val overrideCategory = overrideMap[latest.address.trim().uppercase()]
                val finalCategory = overrideCategory ?: latest.category

                SmsConversation(
                    threadId = threadId,
                    address = latest.address,
                    senderName = latest.senderName,
                    latestMessage = latest.copy(category = finalCategory),
                    messageCount = messages.size,
                    unreadCount = unreadCount,
                    isRead = isRead,
                    category = finalCategory,
                    isPinned = isPinned,
                    hasUserCategoryOverride = overrideCategory != null
                )
            }

            // Apply category filter
            val filteredByCategory = if (categoryFilter != null) {
                conversations.filter { it.category == categoryFilter }
            } else {
                conversations
            }

            // Apply search query filter
            val filteredBySearch = if (!searchQuery.isNullOrBlank()) {
                val q = searchQuery.trim().lowercase()
                filteredByCategory.filter { conv ->
                    conv.address.lowercase().contains(q) ||
                            (conv.senderName?.lowercase()?.contains(q) == true) ||
                            conv.latestMessage.body.lowercase().contains(q)
                }
            } else {
                filteredByCategory
            }

            // Pinned conversations first, then sorted by latest message date descending
            filteredBySearch.sortedWith(
                compareByDescending<SmsConversation> { it.isPinned }
                    .thenByDescending { it.latestMessage.date }
            )
        }.flowOn(ioDispatcher)
    }

    override fun getMessagesForThread(threadId: Long): Flow<List<SmsMessage>> {
        return combine(
            categoryOverrideDao.observeAllOverrides(),
            threadStateDao.observeAllThreadStates(),
            demoModeFlow,
            refreshTrigger
        ) { overrides, threadStates, isDemo, _ ->
            val overrideMap = overrides.associate {
                it.senderAddress.trim().uppercase() to SmsCategory.fromString(it.category)
            }
            val threadState = threadStates.firstOrNull { it.threadId == threadId }

            val messages = if (isDemo || !hasSmsPermission() || !isDefaultSmsApp()) {
                SampleDataProvider.getSampleThreadConversation(threadId)
            } else {
                fetchRealThreadMessages(threadId, overrideMap)
            }

            val isThreadReadOverride = threadState?.isReadOverride

            messages.map { msg ->
                val override = overrideMap[msg.address.trim().uppercase()]
                val cat = override ?: msg.category
                val isRead = isThreadReadOverride ?: msg.isRead
                msg.copy(category = cat, isRead = isRead)
            }.sortedBy { it.date }
        }.flowOn(ioDispatcher)
    }

    override fun getOtps(): Flow<List<SmsMessage>> {
        return getConversations(categoryFilter = SmsCategory.OTPS).map { conversations ->
            conversations.map { it.latestMessage }
                .filter { it.otpInfo != null }
                .sortedByDescending { it.date }
        }.flowOn(ioDispatcher)
    }

    override suspend fun overrideCategory(senderAddress: String, category: SmsCategory) {
        withContext(ioDispatcher) {
            categoryOverrideDao.setOverride(
                CategoryOverrideEntity(
                    senderAddress = senderAddress.trim(),
                    category = category.name
                )
            )
        }
    }

    override suspend fun removeCategoryOverride(senderAddress: String) {
        withContext(ioDispatcher) {
            categoryOverrideDao.removeOverride(senderAddress.trim())
        }
    }

    override suspend fun markConversationRead(threadId: Long, isRead: Boolean) {
        withContext(ioDispatcher) {
            // Attempt system sync if default SMS app
            if (isDefaultSmsApp()) {
                smsProviderReader.markAsReadInSystem(threadId, isRead)
            }

            // Always persist locally in Room so the user's action takes immediate effect
            val existing = threadStateDao.getThreadState(threadId)
            threadStateDao.setThreadState(
                ThreadStateEntity(
                    threadId = threadId,
                    isReadOverride = isRead,
                    isPinned = existing?.isPinned ?: false
                )
            )
        }
    }

    override suspend fun togglePinConversation(threadId: Long, isPinned: Boolean) {
        withContext(ioDispatcher) {
            val existing = threadStateDao.getThreadState(threadId)
            threadStateDao.setThreadState(
                ThreadStateEntity(
                    threadId = threadId,
                    isReadOverride = existing?.isReadOverride,
                    isPinned = isPinned
                )
            )
        }
    }

    private fun fetchRealMessages(overrideMap: Map<String, SmsCategory>): List<SmsMessage> {
        val rawList = smsProviderReader.queryAllMessages()
        return rawList.map { raw ->
            val senderName = if (SmsClassifier.isPhoneNumber(raw.address)) {
                contactReader.getContactName(raw.address)
            } else {
                null
            }

            val override = overrideMap[raw.address.trim().uppercase()]
            val category = SmsClassifier.classify(
                body = raw.body,
                address = raw.address,
                timestamp = raw.date,
                userOverrideCategory = override
            )
            val otpInfo = OtpExtractor.extractOtp(raw.body, raw.address, raw.date)
            val txInfo = TransactionExtractor.extractTransaction(raw.body)

            SmsMessage(
                id = raw.id,
                threadId = raw.threadId,
                address = raw.address,
                senderName = senderName,
                body = raw.body,
                date = raw.date,
                isRead = raw.isRead,
                isFromMe = raw.isFromMe,
                category = category,
                otpInfo = otpInfo,
                transactionInfo = txInfo
            )
        }
    }

    private fun fetchRealThreadMessages(
        threadId: Long,
        overrideMap: Map<String, SmsCategory>
    ): List<SmsMessage> {
        val rawList = smsProviderReader.queryMessagesForThread(threadId)
        return rawList.map { raw ->
            val senderName = if (SmsClassifier.isPhoneNumber(raw.address)) {
                contactReader.getContactName(raw.address)
            } else {
                null
            }

            val override = overrideMap[raw.address.trim().uppercase()]
            val category = SmsClassifier.classify(
                body = raw.body,
                address = raw.address,
                timestamp = raw.date,
                userOverrideCategory = override
            )
            val otpInfo = OtpExtractor.extractOtp(raw.body, raw.address, raw.date)
            val txInfo = TransactionExtractor.extractTransaction(raw.body)

            SmsMessage(
                id = raw.id,
                threadId = raw.threadId,
                address = raw.address,
                senderName = senderName,
                body = raw.body,
                date = raw.date,
                isRead = raw.isRead,
                isFromMe = raw.isFromMe,
                category = category,
                otpInfo = otpInfo,
                transactionInfo = txInfo
            )
        }
    }
}
