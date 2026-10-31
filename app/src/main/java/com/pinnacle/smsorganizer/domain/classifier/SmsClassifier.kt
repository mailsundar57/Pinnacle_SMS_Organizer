package com.pinnacle.smsorganizer.domain.classifier

import com.pinnacle.smsorganizer.data.model.SmsCategory

object SmsClassifier {

    private val TRANSACTION_KEYWORDS = listOf(
        "debited",
        "credited",
        "deposited",
        "spent",
        "withdrawn",
        "salary credited",
        "a/c balance",
        "avl bal",
        "available balance",
        "payment of",
        "card ending",
        "upi ref",
        "neft",
        "imps",
        "rtgs",
        "atm withdrawal",
        "bill generated",
        "minimum amount due",
        "total amount due",
        "statement balance"
    )

    private val PROMO_KEYWORDS = listOf(
        "off on",
        "% off",
        "flat rs",
        "flat $",
        "flat 50%",
        "flat 20%",
        "flat 30%",
        "flat 40%",
        "discount",
        "coupon code",
        "promo code",
        "sale is live",
        "sale ends",
        "flash sale",
        "limited period offer",
        "buy 1 get 1",
        "buy 2 get 1",
        "bogo",
        "shop now",
        "cashback offer",
        "click here to claim",
        "free delivery",
        "hurry! offer",
        "hurry, offer",
        "unbeatable price",
        "exclusive offer",
        "win cash",
        "win exciting",
        "claim your voucher",
        "save up to"
    )

    private val CONVERSATIONAL_KEYWORDS = listOf(
        "hey",
        "hello",
        "call me",
        "call back",
        "how are you",
        "see you",
        "where are you",
        "good morning",
        "good night",
        "happy birthday",
        "congrats",
        "congratulations",
        "are you coming",
        "meet you",
        "thanks",
        "thank you",
        "okay",
        "sure thing",
        "let's catch up",
        "love you",
        "dinner",
        "lunch"
    )

    /**
     * Determines the category for an SMS message deterministically.
     *
     * @param body Message text
     * @param address Sender address or phone number
     * @param timestamp Date in epoch millis
     * @param userOverrideCategory Optional category assigned manually by the user
     */
    fun classify(
        body: String,
        address: String,
        timestamp: Long = System.currentTimeMillis(),
        userOverrideCategory: SmsCategory? = null
    ): SmsCategory {
        // 1. User manual override always wins
        if (userOverrideCategory != null) {
            return userOverrideCategory
        }

        val lowerBody = body.lowercase()
        val trimmedAddress = address.trim()

        // 2. Check for OTP / Security verification codes
        val otpInfo = OtpExtractor.extractOtp(body, trimmedAddress, timestamp)
        if (otpInfo != null) {
            return SmsCategory.OTPS
        }

        // 3. Check for Financial Transactions
        val hasTransactionKeyword = TRANSACTION_KEYWORDS.any { lowerBody.contains(it) }
        val transactionInfo = TransactionExtractor.extractTransaction(body)
        if (hasTransactionKeyword || (transactionInfo != null && transactionInfo.amount != null)) {
            return SmsCategory.TRANSACTIONS
        }

        // 4. Check for Promotional & Marketing Content
        val hasPromoKeyword = PROMO_KEYWORDS.any { lowerBody.contains(it) }
        if (hasPromoKeyword) {
            return SmsCategory.PROMOTIONS
        }

        // 5. Check for Personal Message
        // A message is personal if it's sent from a phone number (e.g. +1..., 10 digits)
        // without an alphanumeric business sender ID (e.g. "VK-AMAZON", "AD-HDFCBK")
        val isPersonalPhone = isPhoneNumber(trimmedAddress)
        val hasConversationalContent = CONVERSATIONAL_KEYWORDS.any { lowerBody.contains(it) }

        if (isPersonalPhone && !isCorporateSender(trimmedAddress)) {
            return SmsCategory.PERSONAL
        }

        if (hasConversationalContent && !isCorporateSender(trimmedAddress)) {
            return SmsCategory.PERSONAL
        }

        // 6. Default fallback: Service messages, telecom alerts, or uncategorized
        return SmsCategory.OTHER
    }

    /**
     * Checks if the sender address is a standard phone number.
     */
    fun isPhoneNumber(address: String): Boolean {
        val sanitized = address.replace(Regex("[\\s\\-\\(\\)]"), "")
        // Matches +1234567890, 9876543210, etc. (at least 7 digits, mostly numbers)
        return sanitized.matches(Regex("""^\+?[0-9]{7,15}$"""))
    }

    /**
     * Checks if the sender looks like a corporate SMS header (e.g. "VK-HDFCBK", "AD-AIRTEL", "VM-GOOGLE").
     */
    fun isCorporateSender(address: String): Boolean {
        // Contains hyphens with letters, or non-digit alphabetic sender IDs like "GOOGLE", "BANKALERT"
        if (address.contains("-")) {
            val parts = address.split("-")
            if (parts.any { it.matches(Regex("""[A-Za-z]{2,}""")) }) {
                return true
            }
        }
        val lettersOnly = address.filter { it.isLetter() }
        return lettersOnly.length >= 4 && !address.startsWith("+")
    }
}
