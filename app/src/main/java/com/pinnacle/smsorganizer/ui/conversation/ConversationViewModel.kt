package com.pinnacle.smsorganizer.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsMessage
import com.pinnacle.smsorganizer.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationUiState(
    val isLoading: Boolean = true,
    val threadId: Long = 0L,
    val address: String = "",
    val senderName: String? = null,
    val category: SmsCategory = SmsCategory.OTHER,
    val messages: List<SmsMessage> = emptyList(),
    val isRead: Boolean = true,
    val showCategoryDialog: Boolean = false
)

class ConversationViewModel(
    private val threadId: Long,
    private val address: String,
    private val repository: SmsRepository
) : ViewModel() {

    private val _showCategoryDialog = MutableStateFlow(false)

    val uiState: StateFlow<ConversationUiState> = combine(
        repository.getMessagesForThread(threadId),
        _showCategoryDialog
    ) { messages, showDialog ->
        val latest = messages.lastOrNull()
        val cat = latest?.category ?: SmsCategory.OTHER
        val name = latest?.senderName
        val isRead = messages.all { it.isRead }

        ConversationUiState(
            isLoading = false,
            threadId = threadId,
            address = address,
            senderName = name,
            category = cat,
            messages = messages,
            isRead = isRead,
            showCategoryDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationUiState(
            threadId = threadId,
            address = address
        )
    )

    init {
        // Automatically mark thread as read when user opens conversation
        markAsRead()
    }

    fun markAsRead() {
        viewModelScope.launch {
            repository.markConversationRead(threadId, true)
        }
    }

    fun toggleReadStatus() {
        val current = uiState.value.isRead
        viewModelScope.launch {
            repository.markConversationRead(threadId, !current)
        }
    }

    fun openCategoryDialog() {
        _showCategoryDialog.value = true
    }

    fun dismissCategoryDialog() {
        _showCategoryDialog.value = false
    }

    fun changeCategory(category: SmsCategory) {
        viewModelScope.launch {
            repository.overrideCategory(address, category)
            _showCategoryDialog.value = false
        }
    }

    class Factory(
        private val threadId: Long,
        private val address: String,
        private val repository: SmsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationViewModel(threadId, address, repository) as T
        }
    }
}
