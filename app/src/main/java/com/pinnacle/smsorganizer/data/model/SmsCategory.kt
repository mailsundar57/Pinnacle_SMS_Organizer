package com.pinnacle.smsorganizer.data.model

enum class SmsCategory(
    val title: String,
    val description: String
) {
    PERSONAL(
        title = "Personal",
        description = "Messages from contacts and individual phone numbers"
    ),
    TRANSACTIONS(
        title = "Transactions",
        description = "Bank debits, credits, bills, and account alerts"
    ),
    OTPS(
        title = "OTPs",
        description = "One-time passwords, verification codes, and security PINs"
    ),
    PROMOTIONS(
        title = "Promotions",
        description = "Discounts, offers, sales, coupons, and marketing"
    ),
    OTHER(
        title = "Other",
        description = "Service updates, telecom info, and uncategorized"
    );

    companion object {
        fun fromString(value: String?): SmsCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
