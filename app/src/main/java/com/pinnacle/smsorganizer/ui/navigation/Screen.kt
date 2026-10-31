package com.pinnacle.smsorganizer.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Inbox : Screen("inbox")

    data object Conversation : Screen("conversation/{threadId}/{address}") {
        fun createRoute(threadId: Long, address: String): String {
            val encodedAddress = Uri.encode(address)
            return "conversation/$threadId/$encodedAddress"
        }
    }
}
