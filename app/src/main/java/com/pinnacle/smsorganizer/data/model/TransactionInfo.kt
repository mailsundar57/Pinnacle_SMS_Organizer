package com.pinnacle.smsorganizer.data.model

enum class TransactionType {
    DEBIT,
    CREDIT,
    BILL,
    REFUND,
    BALANCE_CHECK,
    OTHER
}

data class TransactionInfo(
    val amount: Double?,
    val currency: String?,
    val type: TransactionType,
    val accountNumber: String?,
    val merchant: String?,
    val balance: Double?
)
