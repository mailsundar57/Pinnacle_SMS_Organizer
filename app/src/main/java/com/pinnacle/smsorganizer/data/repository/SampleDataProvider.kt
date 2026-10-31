package com.pinnacle.smsorganizer.data.repository

import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.data.model.SmsMessage
import com.pinnacle.smsorganizer.domain.classifier.OtpExtractor
import com.pinnacle.smsorganizer.domain.classifier.SmsClassifier
import com.pinnacle.smsorganizer.domain.classifier.TransactionExtractor

object SampleDataProvider {

    fun getSampleMessages(): List<SmsMessage> {
        val now = System.currentTimeMillis()
        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        val rawList = listOf(
            // --- OTPS ---
            Triple(
                101L,
                "GOOGLE",
                "G-837492 is your Google verification code. Never share your code with anyone."
            ) to (now - 2 * minute),

            Triple(
                102L,
                "UBER",
                "3920 is your Uber verification code. Do not share your OTP with anyone."
            ) to (now - 8 * minute),

            Triple(
                103L,
                "AD-AMAZON",
                "948210 is your Amazon OTP for order verification. Valid for 10 mins. Never share your OTP."
            ) to (now - 25 * minute),

            Triple(
                104L,
                "SWIGGY",
                "Your Swiggy login code is 5521. Valid for 5 minutes. Enjoy your meal!"
            ) to (now - 3 * hour),

            // --- TRANSACTIONS ---
            Triple(
                201L,
                "VK-HDFCBK",
                "Rs. 2,450.00 debited from A/C XX4921 at STARBUCKS on 30-Oct. Avl Bal: Rs. 45,210.50"
            ) to (now - 45 * minute),

            Triple(
                202L,
                "AX-ICICIB",
                "INR 85,000.00 credited to account ending in 1092 as SALARY. Avl bal is INR 1,12,000.00"
            ) to (now - 1 * day),

            Triple(
                203L,
                "AX-SBICRD",
                "Your credit card bill generated for card ending 9012. Minimum amount due: Rs 1,500. Total amount due: Rs 14,250. Due date 15-Nov."
            ) to (now - 2 * day),

            // --- PERSONAL ---
            Triple(
                301L,
                "+14155552671",
                "Hey! Are you still joining us for dinner tonight at 7?"
            ) to (now - 15 * minute),

            Triple(
                302L,
                "+12025550192",
                "Don't forget to take your vitamins and call when you get home. Love you!"
            ) to (now - 2 * hour),

            Triple(
                303L,
                "+13125550143",
                "Sent you the pitch deck draft over email. Let me know what you think when you have a minute."
            ) to (now - 5 * hour),

            // --- PROMOTIONS ---
            Triple(
                401L,
                "MYNTRA",
                "Flat 50% OFF on all sneakers and apparel! Use coupon code MEGA50 at checkout. Shop now!"
            ) to (now - 4 * hour),

            Triple(
                402L,
                "AD-DOMINO",
                "BOGO Free! Buy 1 regular pizza get 1 free today only. Use code PIZZALOVE. Order now!"
            ) to (now - 7 * hour),

            Triple(
                403L,
                "ZOMATO",
                "Craving Biryani? Get 40% discount up to Rs 120 on your favorite restaurants right now!"
            ) to (now - 1 * day),

            // --- OTHER ---
            Triple(
                501L,
                "AIRTEL",
                "You have consumed 50% of your daily high-speed data limit. To recharge, visit airtel.in or dial *121#."
            ) to (now - 6 * hour),

            Triple(
                502L,
                "BLUEDART",
                "Your package with tracking ID 89234810 is out for delivery with our delivery executive."
            ) to (now - 10 * hour),

            Triple(
                503L,
                "IRCTC",
                "PNR 4829104820: Train 12626 departs at 20:30 from Platform 4. Coach B2, Seat 45 confirmed."
            ) to (now - 2 * day)
        )

        var idCounter = 1L
        return rawList.map { (trip, time) ->
            val (threadId, address, body) = trip
            val category = SmsClassifier.classify(body, address, time)
            val otpInfo = OtpExtractor.extractOtp(body, address, time)
            val txInfo = TransactionExtractor.extractTransaction(body)

            val senderName = when (address) {
                "+14155552671" -> "Sarah Jenkins"
                "+12025550192" -> "Mom"
                "+13125550143" -> "Alex Chen"
                "VK-HDFCBK" -> "HDFC Bank"
                "AX-ICICIB" -> "ICICI Bank"
                "AX-SBICRD" -> "SBI Credit Card"
                "GOOGLE" -> "Google"
                "UBER" -> "Uber"
                "AD-AMAZON" -> "Amazon"
                "SWIGGY" -> "Swiggy"
                "MYNTRA" -> "Myntra"
                "AD-DOMINO" -> "Domino's Pizza"
                "ZOMATO" -> "Zomato"
                "AIRTEL" -> "Airtel Care"
                "BLUEDART" -> "Blue Dart Express"
                "IRCTC" -> "IRCTC Info"
                else -> address
            }

            SmsMessage(
                id = idCounter++,
                threadId = threadId,
                address = address,
                senderName = senderName,
                body = body,
                date = time,
                isRead = (idCounter % 3L != 0L), // Some unread
                isFromMe = false,
                category = category,
                otpInfo = otpInfo,
                transactionInfo = txInfo
            )
        }
    }

    fun getSampleThreadConversation(threadId: Long): List<SmsMessage> {
        val all = getSampleMessages()
        val baseMessage = all.firstOrNull { it.threadId == threadId } ?: return emptyList()

        if (threadId == 301L) {
            // Conversational back and forth
            val time = baseMessage.date
            return listOf(
                SmsMessage(
                    id = 3011L,
                    threadId = 301L,
                    address = baseMessage.address,
                    senderName = baseMessage.senderName,
                    body = "Hey! Are you still joining us for dinner tonight at 7?",
                    date = time - 10 * 60 * 1000L,
                    isRead = true,
                    isFromMe = false,
                    category = SmsCategory.PERSONAL
                ),
                SmsMessage(
                    id = 3012L,
                    threadId = 301L,
                    address = baseMessage.address,
                    senderName = baseMessage.senderName,
                    body = "Yes! I'll be there around 7:15. Should I bring anything?",
                    date = time - 5 * 60 * 1000L,
                    isRead = true,
                    isFromMe = true,
                    category = SmsCategory.PERSONAL
                ),
                SmsMessage(
                    id = 3013L,
                    threadId = 301L,
                    address = baseMessage.address,
                    senderName = baseMessage.senderName,
                    body = "Just bring yourself! Reserved a cozy table at Piccolo.",
                    date = time,
                    isRead = false,
                    isFromMe = false,
                    category = SmsCategory.PERSONAL
                )
            )
        }

        return listOf(baseMessage)
    }
}
