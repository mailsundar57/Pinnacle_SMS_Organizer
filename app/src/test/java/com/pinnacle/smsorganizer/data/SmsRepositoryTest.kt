package com.pinnacle.smsorganizer.data

import com.pinnacle.smsorganizer.data.local.CategoryOverrideDao
import com.pinnacle.smsorganizer.data.local.CategoryOverrideEntity
import com.pinnacle.smsorganizer.data.local.ThreadStateDao
import com.pinnacle.smsorganizer.data.local.ThreadStateEntity
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.provider.ContactReader
import com.pinnacle.smsorganizer.data.provider.RawSms
import com.pinnacle.smsorganizer.data.provider.SmsProviderReader
import com.pinnacle.smsorganizer.data.repository.SmsRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCategoryOverrideDao : CategoryOverrideDao {
    private val overrides = MutableStateFlow<List<CategoryOverrideEntity>>(emptyList())

    override fun observeAllOverrides(): Flow<List<CategoryOverrideEntity>> = overrides

    override suspend fun getAllOverrides(): List<CategoryOverrideEntity> = overrides.value

    override suspend fun getOverrideForSender(senderAddress: String): CategoryOverrideEntity? {
        return overrides.value.firstOrNull { it.senderAddress.equals(senderAddress, ignoreCase = true) }
    }

    override suspend fun setOverride(override: CategoryOverrideEntity) {
        val current = overrides.value.filterNot { it.senderAddress.equals(override.senderAddress, ignoreCase = true) }
        overrides.value = current + override
    }

    override suspend fun removeOverride(senderAddress: String) {
        overrides.value = overrides.value.filterNot { it.senderAddress.equals(senderAddress, ignoreCase = true) }
    }

    override suspend fun clearAllOverrides() {
        overrides.value = emptyList()
    }
}

class FakeThreadStateDao : ThreadStateDao {
    private val states = MutableStateFlow<List<ThreadStateEntity>>(emptyList())

    override fun observeAllThreadStates(): Flow<List<ThreadStateEntity>> = states

    override suspend fun getAllThreadStates(): List<ThreadStateEntity> = states.value

    override suspend fun getThreadState(threadId: Long): ThreadStateEntity? {
        return states.value.firstOrNull { it.threadId == threadId }
    }

    override suspend fun setThreadState(state: ThreadStateEntity) {
        val current = states.value.filterNot { it.threadId == state.threadId }
        states.value = current + state
    }

    override suspend fun updateReadStatus(threadId: Long, isRead: Boolean, updatedAt: Long): Int {
        val existing = getThreadState(threadId)
        val newState = ThreadStateEntity(threadId, isReadOverride = isRead, isPinned = existing?.isPinned ?: false, updatedAt = updatedAt)
        setThreadState(newState)
        return 1
    }

    override suspend fun updatePinnedStatus(threadId: Long, isPinned: Boolean, updatedAt: Long): Int {
        val existing = getThreadState(threadId)
        val newState = ThreadStateEntity(threadId, isReadOverride = existing?.isReadOverride, isPinned = isPinned, updatedAt = updatedAt)
        setThreadState(newState)
        return 1
    }

    override suspend fun removeThreadState(threadId: Long) {
        states.value = states.value.filterNot { it.threadId == threadId }
    }
}

class FakeSmsProviderReader(
    private var hasPerm: Boolean = true,
    private var isDefault: Boolean = true,
    private val rawMessages: MutableList<RawSms> = mutableListOf()
) : SmsProviderReader(null) {

    override fun hasReadSmsPermission(): Boolean = hasPerm
    override fun isDefaultSmsApp(): Boolean = isDefault
    override fun queryAllMessages(): List<RawSms> = if (hasPerm) rawMessages else emptyList()
    override fun queryMessagesForThread(threadId: Long): List<RawSms> =
        if (hasPerm) rawMessages.filter { it.threadId == threadId } else emptyList()

    override fun markAsReadInSystem(threadId: Long, isRead: Boolean): Boolean {
        if (!isDefault) return false
        val idx = rawMessages.indexOfFirst { it.threadId == threadId }
        if (idx != -1) {
            rawMessages[idx] = rawMessages[idx].copy(isRead = isRead)
            return true
        }
        return false
    }

    fun setMessages(messages: List<RawSms>) {
        rawMessages.clear()
        rawMessages.addAll(messages)
    }

    fun setPermission(granted: Boolean) {
        hasPerm = granted
    }
}

class FakeContactReader : ContactReader(null) {
    private val contacts = mutableMapOf<String, String>()

    fun addContact(phone: String, name: String) {
        contacts[phone] = name
    }

    override fun getContactName(phoneNumber: String): String? = contacts[phoneNumber]
}

@OptIn(ExperimentalCoroutinesApi::class)
class SmsRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var categoryOverrideDao: FakeCategoryOverrideDao
    private lateinit var threadStateDao: FakeThreadStateDao
    private lateinit var fakeSmsReader: FakeSmsProviderReader
    private lateinit var fakeContactReader: FakeContactReader
    private lateinit var repository: SmsRepositoryImpl

    @Before
    fun setup() {
        categoryOverrideDao = FakeCategoryOverrideDao()
        threadStateDao = FakeThreadStateDao()
        fakeSmsReader = FakeSmsProviderReader()
        fakeContactReader = FakeContactReader()

        repository = SmsRepositoryImpl(
            smsProviderReader = fakeSmsReader,
            contactReader = fakeContactReader,
            categoryOverrideDao = categoryOverrideDao,
            threadStateDao = threadStateDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testGetConversations_WithClassification() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 101, "GOOGLE", now - 1000, "G-123456 is your Google verification code", false, false),
            RawSms(2, 201, "HDFCBK", now - 2000, "Rs. 500 debited from A/C XX1234 at CAFE", true, false),
            RawSms(3, 301, "+1234567890", now - 3000, "Hey, are you coming to lunch?", true, false),
            RawSms(4, 401, "DOMINOS", now - 4000, "Flat 50% off on all pizzas! Shop now.", true, false)
        )
        fakeSmsReader.setMessages(testData)
        fakeContactReader.addContact("+1234567890", "Alice")

        val conversations = repository.getConversations().first()
        assertEquals(4, conversations.size)

        val otpConv = conversations.first { it.threadId == 101L }
        assertEquals(SmsCategory.OTPS, otpConv.category)

        val txConv = conversations.first { it.threadId == 201L }
        assertEquals(SmsCategory.TRANSACTIONS, txConv.category)

        val personalConv = conversations.first { it.threadId == 301L }
        assertEquals(SmsCategory.PERSONAL, personalConv.category)
        assertEquals("Alice", personalConv.senderName)

        val promoConv = conversations.first { it.threadId == 401L }
        assertEquals(SmsCategory.PROMOTIONS, promoConv.category)
    }

    @Test
    fun testUserCategoryOverride_OverridesClassifier() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 401, "DOMINOS", now, "Flat 50% off on pizza! Order now.", true, false)
        )
        fakeSmsReader.setMessages(testData)

        // Default classification: PROMOTIONS
        val initialConv = repository.getConversations().first().first()
        assertEquals(SmsCategory.PROMOTIONS, initialConv.category)

        // User overrides DOMINOS to PERSONAL
        repository.overrideCategory("DOMINOS", SmsCategory.PERSONAL)

        val updatedConv = repository.getConversations().first().first()
        assertEquals(SmsCategory.PERSONAL, updatedConv.category)
        assertTrue(updatedConv.hasUserCategoryOverride)
    }

    @Test
    fun testMarkConversationRead_UpdatesLocalState() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 101, "GOOGLE", now, "G-123456 is your code", isRead = false, isFromMe = false)
        )
        fakeSmsReader.setMessages(testData)

        val initialConv = repository.getConversations().first().first()
        assertFalse(initialConv.isRead)

        // Mark as read
        repository.markConversationRead(101L, isRead = true)

        val updatedConv = repository.getConversations().first().first()
        assertTrue(updatedConv.isRead)
        assertEquals(0, updatedConv.unreadCount)
    }

    @Test
    fun testPinConversation_AppearsFirst() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 101, "GOOGLE", now, "Code 1", true, false),
            RawSms(2, 201, "HDFCBK", now - 5000, "Debit 2", true, false)
        )
        fakeSmsReader.setMessages(testData)

        // Initially 101 is newer so it's first
        var conversations = repository.getConversations().first()
        assertEquals(101L, conversations[0].threadId)

        // Pin 201
        repository.togglePinConversation(201L, isPinned = true)

        conversations = repository.getConversations().first()
        assertEquals(201L, conversations[0].threadId)
        assertTrue(conversations[0].isPinned)
    }

    @Test
    fun testCategoryFilter_ReturnsOnlyMatching() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 101, "GOOGLE", now, "G-123456 is your OTP code", false, false),
            RawSms(2, 201, "HDFCBK", now, "Rs 100 debited", true, false)
        )
        fakeSmsReader.setMessages(testData)

        val otpConversations = repository.getConversations(categoryFilter = SmsCategory.OTPS).first()
        assertEquals(1, otpConversations.size)
        assertEquals(101L, otpConversations[0].threadId)

        val txConversations = repository.getConversations(categoryFilter = SmsCategory.TRANSACTIONS).first()
        assertEquals(1, txConversations.size)
        assertEquals(201L, txConversations[0].threadId)
    }

    @Test
    fun testSearchQuery_MatchesSenderAndBody() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val testData = listOf(
            RawSms(1, 101, "GOOGLE", now, "G-123456 is your code", false, false),
            RawSms(2, 201, "HDFCBK", now, "Coffee purchase Rs 200", true, false)
        )
        fakeSmsReader.setMessages(testData)

        val searchResult1 = repository.getConversations(searchQuery = "coffee").first()
        assertEquals(1, searchResult1.size)
        assertEquals(201L, searchResult1[0].threadId)

        val searchResult2 = repository.getConversations(searchQuery = "google").first()
        assertEquals(1, searchResult2.size)
        assertEquals(101L, searchResult2[0].threadId)
    }

    @Test
    fun testFallbackToSampleData_WhenPermissionDenied() = runTest(testDispatcher) {
        fakeSmsReader.setPermission(false)

        // Should return sample messages
        val conversations = repository.getConversations().first()
        assertTrue("Should return sample conversations when permission is false", conversations.isNotEmpty())
    }
}
