package com.pinnacle.smsorganizer.domain.usecase

import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.data.repository.SmsRepository
import kotlinx.coroutines.flow.Flow

class GetConversationsUseCase(
    private val repository: SmsRepository
) {
    operator fun invoke(
        categoryFilter: SmsCategory? = null,
        searchQuery: String? = null
    ): Flow<List<SmsConversation>> {
        return repository.getConversations(categoryFilter, searchQuery)
    }
}

class GetMessagesForThreadUseCase(
    private val repository: SmsRepository
) {
    operator fun invoke(threadId: Long) = repository.getMessagesForThread(threadId)
}

class OverrideCategoryUseCase(
    private val repository: SmsRepository
) {
    suspend fun setOverride(senderAddress: String, category: SmsCategory) {
        repository.overrideCategory(senderAddress, category)
    }

    suspend fun removeOverride(senderAddress: String) {
        repository.removeCategoryOverride(senderAddress)
    }
}

class MarkConversationReadUseCase(
    private val repository: SmsRepository
) {
    suspend operator fun invoke(threadId: Long, isRead: Boolean) {
        repository.markConversationRead(threadId, isRead)
    }
}

class TogglePinUseCase(
    private val repository: SmsRepository
) {
    suspend operator fun invoke(threadId: Long, isPinned: Boolean) {
        repository.togglePinConversation(threadId, isPinned)
    }
}
