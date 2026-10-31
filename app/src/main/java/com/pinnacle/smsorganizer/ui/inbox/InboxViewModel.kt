package com.pinnacle.smsorganizer.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.data.model.SmsMessage
import com.pinnacle.smsorganizer.data.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val isLoading: Boolean = true,
    val hasSmsPermission: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    val isDemoMode: Boolean = false,
    val selectedCategory: SmsCategory? = null, // null for "All"
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val conversations: List<SmsConversation> = emptyList(),
    val recentOtps: List<SmsMessage> = emptyList(),
    val tabUnreadCounts: Map<SmsCategory?, Int> = emptyMap(),
    val senderForOverride: Pair<String, SmsCategory>? = null // (address, currentCategory)
)

class InboxViewModel(
    private val repository: SmsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<SmsCategory?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _senderForOverride = MutableStateFlow<Pair<String, SmsCategory>?>(null)

    // Base all-conversations flow to compute unread counts across all tabs
    private val allConversationsFlow = repository.getConversations(categoryFilter = null, searchQuery = null)

    private data class RepoData(
        val allConversations: List<SmsConversation>,
        val otps: List<SmsMessage>,
        val isDemo: Boolean
    )

    private val repoDataFlow = combine(
        allConversationsFlow,
        repository.getOtps(),
        repository.isDemoModeEnabled()
    ) { allConv, otps, isDemo ->
        RepoData(allConv, otps, isDemo)
    }

    val uiState: StateFlow<InboxUiState> = combine(
        _selectedCategory,
        _searchQuery,
        _isSearchActive,
        _senderForOverride,
        repoDataFlow
    ) { selectedCat, query, isSearch, overridePair, repoData ->
        val hasPerm = repository.hasSmsPermission()
        val isDefault = repository.isDefaultSmsApp()
        val allConversations = repoData.allConversations
        val otps = repoData.otps
        val isDemo = repoData.isDemo

        // Unread counts per category
        val unreadCounts = mutableMapOf<SmsCategory?, Int>()
        unreadCounts[null] = allConversations.sumOf { it.unreadCount }
        SmsCategory.entries.forEach { cat ->
            unreadCounts[cat] = allConversations
                .filter { it.category == cat }
                .sumOf { it.unreadCount }
        }

        // Filter conversations for the current tab & search
        val filtered = allConversations.filter { conv ->
            val matchesCategory = (selectedCat == null || conv.category == selectedCat)
            val matchesSearch = if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                conv.address.lowercase().contains(q) ||
                        (conv.senderName?.lowercase()?.contains(q) == true) ||
                        conv.latestMessage.body.lowercase().contains(q)
            } else {
                true
            }
            matchesCategory && matchesSearch
        }

        InboxUiState(
            isLoading = false,
            hasSmsPermission = hasPerm,
            isDefaultSmsApp = isDefault,
            isDemoMode = isDemo,
            selectedCategory = selectedCat,
            searchQuery = query,
            isSearchActive = isSearch,
            conversations = filtered,
            recentOtps = otps,
            tabUnreadCounts = unreadCounts,
            senderForOverride = overridePair
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState(
            hasSmsPermission = repository.hasSmsPermission(),
            isDefaultSmsApp = repository.isDefaultSmsApp()
        )
    )

    fun selectCategory(category: SmsCategory?) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun enableDemoMode() {
        repository.setDemoMode(true)
    }

    fun toggleDemoMode() {
        val current = uiState.value.isDemoMode
        repository.setDemoMode(!current)
    }

    fun markConversationRead(threadId: Long, isRead: Boolean) {
        viewModelScope.launch {
            repository.markConversationRead(threadId, isRead)
        }
    }

    fun togglePin(threadId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinConversation(threadId, !isPinned)
        }
    }

    fun openCategoryOverride(senderAddress: String, currentCategory: SmsCategory) {
        _senderForOverride.value = senderAddress to currentCategory
    }

    fun dismissCategoryOverride() {
        _senderForOverride.value = null
    }

    fun applyCategoryOverride(category: SmsCategory) {
        val pair = _senderForOverride.value ?: return
        viewModelScope.launch {
            repository.overrideCategory(pair.first, category)
            _senderForOverride.value = null
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    class Factory(private val repository: SmsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InboxViewModel(repository) as T
        }
    }
}
