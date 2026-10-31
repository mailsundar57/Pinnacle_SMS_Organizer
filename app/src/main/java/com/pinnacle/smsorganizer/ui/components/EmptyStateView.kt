package com.pinnacle.smsorganizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pinnacle.smsorganizer.data.model.SmsCategory

@Composable
fun EmptyStateView(
    category: SmsCategory?,
    isSearchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (icon, title, subtitle) = when {
        isSearchActive -> Triple(
            Icons.Default.SearchOff,
            "No Results Found",
            "Try searching with a different sender name, phone number, or keyword"
        )
        category == SmsCategory.PERSONAL -> Triple(
            Icons.Default.Person,
            "No Personal Messages",
            "Conversations with friends, family, and direct phone numbers will appear here"
        )
        category == SmsCategory.TRANSACTIONS -> Triple(
            Icons.Default.Payments,
            "No Transactions Found",
            "Bank alerts, debit/credit notices, and card updates will appear here"
        )
        category == SmsCategory.OTPS -> Triple(
            Icons.Default.Lock,
            "No Active OTPs",
            "One-time passwords, 2FA codes, and login verification PINs will appear here"
        )
        category == SmsCategory.PROMOTIONS -> Triple(
            Icons.Default.LocalOffer,
            "No Promotional Messages",
            "Discount offers, sales announcements, and coupons will appear here"
        )
        category == SmsCategory.OTHER -> Triple(
            Icons.Default.Inbox,
            "No Other Messages",
            "Telecom updates, delivery notifications, and uncategorized texts will appear here"
        )
        else -> Triple(
            Icons.Default.Inbox,
            "Inbox is Empty",
            "Incoming SMS messages will be automatically organized here"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
