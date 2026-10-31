package com.pinnacle.smsorganizer.data.model

data class OtpInfo(
    val code: String,
    val serviceName: String?,
    val expiryMinutes: Int?,
    val receivedAt: Long
) {
    /**
     * Checks if the OTP is likely expired based on expiryMinutes and current time.
     * Default assumption: OTP is fresh if within 10 minutes when no explicit expiry is parsed.
     */
    fun isExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val validityPeriodMillis = (expiryMinutes ?: 10) * 60 * 1000L
        return (currentTimeMillis - receivedAt) > validityPeriodMillis
    }

    fun remainingValiditySeconds(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        val validityPeriodMillis = (expiryMinutes ?: 10) * 60 * 1000L
        val remaining = (receivedAt + validityPeriodMillis) - currentTimeMillis
        return if (remaining > 0) remaining / 1000 else 0
    }
}
