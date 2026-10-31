package com.pinnacle.smsorganizer.domain.classifier

import com.pinnacle.smsorganizer.data.model.OtpInfo
import java.util.regex.Pattern

object OtpExtractor {

    // Common OTP keywords in English
    private val OTP_KEYWORDS = listOf(
        "otp",
        "one time password",
        "one-time password",
        "verification code",
        "security code",
        "login code",
        "confirmation code",
        "authorization code",
        "auth code",
        "secret code",
        "passcode",
        "valid for",
        "do not share",
        "never share"
    )

    // Regex to match OTP codes:
    // 1. Matches 4 to 8 consecutive digits
    // 2. Or Google style 'G-123456'
    // Negative lookarounds ensure it's not a year (1990-2030), not part of a phone number or account number.
    private val CODE_PATTERN_PREFIXED = Pattern.compile(
        """(?i)\b(?:is|code|otp|pin|passcode)\s*(?:is|:|-)?\s*([0-9]{4,8})\b"""
    )

    private val GOOGLE_CODE_PATTERN = Pattern.compile(
        """\b(G-\d{6})\b"""
    )

    private val GENERIC_CODE_PATTERN = Pattern.compile(
        """(?<!\d|\$|Rs\.?|INR|\+)(?:^|\s)([0-9]{4,8})(?!\d|\w)"""
    )

    // Pattern to extract expiry in minutes
    private val EXPIRY_MINUTES_PATTERN = Pattern.compile(
        """(?i)(?:valid(?:ity)?|expires?|within)\s*(?:for\s*)?(?:in\s*)?(\d{1,2})\s*(?:mins?|minutes?)"""
    )

    // Negative triggers: if the message is clearly not an OTP (e.g. tracking number, phone balance only)
    private val NOT_OTP_TRIGGERS = listOf(
        "tracking id",
        "order id",
        "pnr",
        "transaction id",
        "awb",
        "ticket no"
    )

    /**
     * Extracts OTP information from the message body.
     * Returns OtpInfo if an OTP is detected, or null otherwise.
     */
    fun extractOtp(body: String, address: String, timestamp: Long): OtpInfo? {
        val lowerBody = body.lowercase()

        // 1. Check if message contains OTP-related keywords
        val hasOtpKeyword = OTP_KEYWORDS.any { lowerBody.contains(it) }
        if (!hasOtpKeyword && !GOOGLE_CODE_PATTERN.matcher(body).find()) {
            return null
        }

        // 2. Reject messages that are purely tracking/order updates without security context
        val isExplicitOtpContext = lowerBody.contains("otp") ||
                lowerBody.contains("verification") ||
                lowerBody.contains("passcode") ||
                lowerBody.contains("security code") ||
                lowerBody.contains("do not share")

        if (!isExplicitOtpContext && NOT_OTP_TRIGGERS.any { lowerBody.contains(it) }) {
            return null
        }

        // 3. Extract the actual OTP code
        var extractedCode: String? = null

        // Check Google style G-123456
        val googleMatcher = GOOGLE_CODE_PATTERN.matcher(body)
        if (googleMatcher.find()) {
            extractedCode = googleMatcher.group(1)
        }

        // Check standard prefixed code (e.g. "is 491023", "OTP: 9182", "code: 382910")
        if (extractedCode == null) {
            val prefixMatcher = CODE_PATTERN_PREFIXED.matcher(body)
            if (prefixMatcher.find()) {
                val candidate = prefixMatcher.group(1)
                if (candidate != null && isValidCode(candidate, lowerBody)) {
                    extractedCode = candidate
                }
            }
        }

        // Check generic 4-8 digit standalone number
        if (extractedCode == null) {
            val genericMatcher = GENERIC_CODE_PATTERN.matcher(body)
            while (genericMatcher.find()) {
                val candidate = genericMatcher.group(1)?.trim()
                if (candidate != null && isValidCode(candidate, lowerBody)) {
                    extractedCode = candidate
                    break
                }
            }
        }

        if (extractedCode == null) {
            return null
        }

        // 4. Extract expiry minutes if mentioned
        var expiryMinutes: Int? = null
        val expiryMatcher = EXPIRY_MINUTES_PATTERN.matcher(body)
        if (expiryMatcher.find()) {
            expiryMinutes = expiryMatcher.group(1)?.toIntOrNull()
        }

        // 5. Determine service name
        val serviceName = resolveServiceName(address, body)

        return OtpInfo(
            code = extractedCode,
            serviceName = serviceName,
            expiryMinutes = expiryMinutes,
            receivedAt = timestamp
        )
    }

    private fun isValidCode(candidate: String, lowerBody: String): Boolean {
        // Disqualify standard 4-digit years like 2024, 2025, 2026 if context shows date
        if (candidate.length == 4) {
            val intVal = candidate.toIntOrNull() ?: 0
            if (intVal in 2020..2030 && (lowerBody.contains("date") || lowerBody.contains("year") || lowerBody.contains("on "))) {
                return false
            }
        }
        // Disqualify if it's right next to currency terms
        val index = lowerBody.indexOf(candidate)
        if (index > 0) {
            val prefix = lowerBody.substring((index - 10).coerceAtLeast(0), index)
            if (prefix.contains("rs") || prefix.contains("inr") || prefix.contains("$") || prefix.contains("amount")) {
                return false
            }
        }
        return true
    }

    private fun resolveServiceName(address: String, body: String): String {
        val lowerBody = body.lowercase()
        val knownServices = listOf(
            "google" to "Google",
            "microsoft" to "Microsoft",
            "amazon" to "Amazon",
            "apple" to "Apple",
            "uber" to "Uber",
            "ola" to "Ola",
            "swiggy" to "Swiggy",
            "zomato" to "Zomato",
            "netflix" to "Netflix",
            "spotify" to "Spotify",
            "whatsapp" to "WhatsApp",
            "telegram" to "Telegram",
            "facebook" to "Facebook",
            "instagram" to "Instagram",
            "twitter" to "X (Twitter)",
            "hdfc" to "HDFC Bank",
            "sbi" to "State Bank of India",
            "icici" to "ICICI Bank",
            "axis" to "Axis Bank",
            "paypal" to "PayPal",
            "paytm" to "Paytm",
            "phonepe" to "PhonePe",
            "gpay" to "Google Pay"
        )

        for ((keyword, name) in knownServices) {
            if (lowerBody.contains(keyword) || address.lowercase().contains(keyword)) {
                return name
            }
        }

        // Extract alpha letters from header like "VM-AMAZON" or "VK-HDFCBK"
        val parts = address.split("-")
        return if (parts.size > 1 && parts[1].isNotBlank()) {
            parts[1].uppercase()
        } else {
            address
        }
    }
}
