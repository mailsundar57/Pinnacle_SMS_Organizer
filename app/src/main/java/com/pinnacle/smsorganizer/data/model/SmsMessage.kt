package com.pinnacle.smsorganizer.data.model

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val senderName: String? = null,
    val body: String,
    val date: Long,
    val isRead: Boolean = true,
    val isFromMe: Boolean = false,
    val category: SmsCategory = SmsCategory.OTHER,
    val otpInfo: OtpInfo? = null,
    val transactionInfo: TransactionInfo? = null
)
