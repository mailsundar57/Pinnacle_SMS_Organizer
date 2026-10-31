package com.pinnacle.smsorganizer.domain.classifier

import com.pinnacle.smsorganizer.data.model.TransactionInfo
import com.pinnacle.smsorganizer.data.model.TransactionType
import java.util.regex.Pattern

object TransactionExtractor {

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?i)(?:rs\.?|inr|\$|usd|eur|€|gbp|£)\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    private val AMOUNT_SUFFIX_PATTERN = Pattern.compile(
        """([\d,]+(?:\.\d{1,2})?)\s*(?:inr|usd|eur)"""
    )

    private val ACCOUNT_PATTERN = Pattern.compile(
        """(?i)(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:ending\s*)?(?:in\s*|with\s*)?([xX*]*\d{3,4})"""
    )

    private val BALANCE_PATTERN = Pattern.compile(
        """(?i)(?:avl\s*bal|available\s*balance|bal|balance)(?:\s*[:=-])?\s*(?:is\s*)?(?:rs\.?|inr|\$|usd|€|£)?\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    private val MERCHANT_PATTERN = Pattern.compile(
        """(?i)(?:at|to|info)\s+([A-Za-z0-9\s\.\*]{3,24})(?:[\.,\s]|on)"""
    )

    fun extractTransaction(body: String): TransactionInfo? {
        val lower = body.lowercase()

        val isDebit = lower.contains("debited") ||
                lower.contains("spent") ||
                lower.contains("paid") ||
                lower.contains("withdrawn") ||
                lower.contains("payment of")

        val isCredit = lower.contains("credited") ||
                lower.contains("deposited") ||
                lower.contains("salary") ||
                lower.contains("received")

        val isRefund = lower.contains("refund") || lower.contains("reversal")

        val isBill = lower.contains("bill generated") ||
                lower.contains("minimum amount due") ||
                lower.contains("total amount due") ||
                lower.contains("payment due")

        val isBalCheck = lower.contains("available balance") ||
                lower.contains("avl bal") ||
                lower.contains("balance is")

        val type = when {
            isRefund -> TransactionType.REFUND
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            isBill -> TransactionType.BILL
            isBalCheck -> TransactionType.BALANCE_CHECK
            else -> TransactionType.OTHER
        }

        // Amount parsing
        var amount: Double? = null
        var currency: String? = null

        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        if (amountMatcher.find()) {
            val raw = amountMatcher.group(1)?.replace(",", "")
            amount = raw?.toDoubleOrNull()
            currency = if (body.contains("$")) "$" else if (body.contains("€")) "€" else if (body.contains("£")) "£" else "₹"
        } else {
            val suffixMatcher = AMOUNT_SUFFIX_PATTERN.matcher(body)
            if (suffixMatcher.find()) {
                val raw = suffixMatcher.group(1)?.replace(",", "")
                amount = raw?.toDoubleOrNull()
                currency = "₹"
            }
        }

        if (amount == null && type == TransactionType.OTHER && !isBalCheck) {
            return null
        }

        // Account number
        var account: String? = null
        val acctMatcher = ACCOUNT_PATTERN.matcher(body)
        if (acctMatcher.find()) {
            account = acctMatcher.group(1)
        }

        // Balance
        var balance: Double? = null
        val balMatcher = BALANCE_PATTERN.matcher(body)
        if (balMatcher.find()) {
            val rawBal = balMatcher.group(1)?.replace(",", "")
            balance = rawBal?.toDoubleOrNull()
        }

        // Merchant
        var merchant: String? = null
        val merchMatcher = MERCHANT_PATTERN.matcher(body)
        if (merchMatcher.find()) {
            val rawMerch = merchMatcher.group(1)?.trim()
            if (rawMerch != null && !rawMerch.startsWith("a/c", ignoreCase = true) && !rawMerch.startsWith("rs", ignoreCase = true)) {
                merchant = rawMerch
            }
        }

        return TransactionInfo(
            amount = amount,
            currency = currency ?: "₹",
            type = type,
            accountNumber = account,
            merchant = merchant,
            balance = balance
        )
    }
}
