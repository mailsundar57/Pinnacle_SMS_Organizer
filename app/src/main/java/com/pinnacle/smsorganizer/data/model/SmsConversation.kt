package com.pinnacle.smsorganizer.data.model

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val senderName: String? = null,
    val latestMessage: SmsMessage,
    val messageCount: Int = 1,
    val unreadCount: Int = 0,
    val isRead: Boolean = true,
    val category: SmsCategory = SmsCategory.OTHER,
    val isPinned: Boolean = false,
    val hasUserCategoryOverride: Boolean = false
) {
    val displayTitle: String
        get() = senderName?.takeIf { it.isNotBlank() } ?: address
}
