package com.pinnacle.smsorganizer.domain

import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.TransactionType
import com.pinnacle.smsorganizer.domain.classifier.OtpExtractor
import com.pinnacle.smsorganizer.domain.classifier.SmsClassifier
import com.pinnacle.smsorganizer.domain.classifier.TransactionExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsClassifierTest {

    @Test
    fun testOtpClassification_BankOtp() {
        val message = "Your OTP for HDFC Bank NetBanking transaction is 492019. Valid for 10 mins. Do not share with anyone."
        val sender = "VK-HDFCBK"

        val otpInfo = OtpExtractor.extractOtp(message, sender, System.currentTimeMillis())
        assertNotNull("OTP info should be extracted", otpInfo)
        assertEquals("492019", otpInfo?.code)
        assertEquals(10, otpInfo?.expiryMinutes)
        assertEquals("HDFC Bank", otpInfo?.serviceName)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.OTPS, category)
    }

    @Test
    fun testOtpClassification_GoogleVerification() {
        val message = "G-837492 is your Google verification code."
        val sender = "GOOGLE"

        val otpInfo = OtpExtractor.extractOtp(message, sender, System.currentTimeMillis())
        assertNotNull("Google OTP should be detected", otpInfo)
        assertEquals("G-837492", otpInfo?.code)
        assertEquals("Google", otpInfo?.serviceName)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.OTPS, category)
    }

    @Test
    fun testOtpClassification_ShortCodeUber() {
        val message = "3920 is your Uber verification code. Never share this code."
        val sender = "UBER"

        val otpInfo = OtpExtractor.extractOtp(message, sender, System.currentTimeMillis())
        assertNotNull("Uber OTP should be detected", otpInfo)
        assertEquals("3920", otpInfo?.code)
        assertEquals("Uber", otpInfo?.serviceName)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.OTPS, category)
    }

    @Test
    fun testOtpExtraction_NotOtp_WhenTrackingOrOrder() {
        val message = "Your order #82910 has been shipped with tracking ID 982103."
        val sender = "AMAZON"

        val otpInfo = OtpExtractor.extractOtp(message, sender, System.currentTimeMillis())
        assertNull("Order tracking should not be detected as OTP", otpInfo)
    }

    @Test
    fun testTransactionClassification_Debit() {
        val message = "Rs. 2,450.00 debited from A/C XX4921 at STARBUCKS on 24-Oct. Avl Bal: Rs. 45,210.50"
        val sender = "VK-HDFCBK"

        val txInfo = TransactionExtractor.extractTransaction(message)
        assertNotNull("Transaction should be extracted", txInfo)
        assertEquals(2450.0, txInfo?.amount ?: 0.0, 0.01)
        assertEquals(TransactionType.DEBIT, txInfo?.type)
        assertEquals("XX4921", txInfo?.accountNumber)
        assertEquals(45210.5, txInfo?.balance ?: 0.0, 0.01)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.TRANSACTIONS, category)
    }

    @Test
    fun testTransactionClassification_CreditSalary() {
        val message = "INR 85,000.00 credited to account ending in 1092 as SALARY. Avl bal is INR 1,12,000.00"
        val sender = "AX-ICICIB"

        val txInfo = TransactionExtractor.extractTransaction(message)
        assertNotNull("Credit transaction should be extracted", txInfo)
        assertEquals(85000.0, txInfo?.amount ?: 0.0, 0.01)
        assertEquals(TransactionType.CREDIT, txInfo?.type)
        assertEquals("1092", txInfo?.accountNumber)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.TRANSACTIONS, category)
    }

    @Test
    fun testTransactionClassification_BillNotice() {
        val message = "Your credit card bill generated. Minimum amount due: Rs 1,500. Total amount due: Rs 12,400."
        val sender = "AX-SBICRD"

        val txInfo = TransactionExtractor.extractTransaction(message)
        assertNotNull("Bill should be extracted", txInfo)
        assertEquals(TransactionType.BILL, txInfo?.type)

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.TRANSACTIONS, category)
    }

    @Test
    fun testPromotionClassification_SaleOffer() {
        val message = "Flat 50% OFF on your favorite shoes! Use coupon code MEGA50 at checkout. Shop now!"
        val sender = "MYNTRA"

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.PROMOTIONS, category)
    }

    @Test
    fun testPromotionClassification_FlashSaleBogo() {
        val message = "Flash sale is live! Buy 1 get 1 free on all apparel. Hurry, offer ends tonight at midnight!"
        val sender = "AD-ZARA"

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.PROMOTIONS, category)
    }

    @Test
    fun testPersonalClassification_PhoneNumberAndChat() {
        val message = "Hey, are you free for dinner tonight? Let's catch up!"
        val sender = "+14155552671"

        assertTrue("Sender should be recognized as phone number", SmsClassifier.isPhoneNumber(sender))
        assertFalse("Sender should not be corporate", SmsClassifier.isCorporateSender(sender))

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.PERSONAL, category)
    }

    @Test
    fun testPersonalClassification_ShortResponse() {
        val message = "Sure, see you at 7pm!"
        val sender = "+919876543210"

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.PERSONAL, category)
    }

    @Test
    fun testOtherClassification_TelecomDataAlert() {
        val message = "You have consumed 50% of your daily high-speed data limit. Dial *121# to recharge."
        val sender = "AIRTEL"

        val category = SmsClassifier.classify(message, sender)
        assertEquals(SmsCategory.OTHER, category)
    }

    @Test
    fun testUserOverride_TakesPrecedence() {
        val promotionalMessage = "Flat 50% OFF on shoes! Shop now."
        val sender = "MYNTRA"

        // Without override, it's PROMOTIONS
        val normalCategory = SmsClassifier.classify(promotionalMessage, sender)
        assertEquals(SmsCategory.PROMOTIONS, normalCategory)

        // With user override to PERSONAL, it must return PERSONAL
        val overriddenCategory = SmsClassifier.classify(
            body = promotionalMessage,
            address = sender,
            userOverrideCategory = SmsCategory.PERSONAL
        )
        assertEquals(SmsCategory.PERSONAL, overriddenCategory)
    }
}
