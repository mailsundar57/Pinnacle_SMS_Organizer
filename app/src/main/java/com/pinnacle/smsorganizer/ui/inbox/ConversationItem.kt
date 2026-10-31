package com.pinnacle.smsorganizer.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinnacle.smsorganizer.data.model.SmsConversation
import com.pinnacle.smsorganizer.ui.components.CategoryBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: SmsConversation,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    onTogglePin: () -> Unit,
    onChangeCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .background(
                if (!conversation.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with initials
        AvatarCircle(
            name = conversation.displayTitle,
            category = conversation.category
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Title, Snippet, Category
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    Text(
                        text = conversation.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatMessageDate(conversation.latestMessage.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!conversation.isRead) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conversation.latestMessage.body.replace("\n", " "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (!conversation.isRead) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (!conversation.isRead) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    category = conversation.category,
                    onClick = onChangeCategory
                )

                if (conversation.hasUserCategoryOverride) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(User custom)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!conversation.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // More options dropdown menu
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (conversation.isRead) "Mark as Unread" else "Mark as Read") },
                    onClick = {
                        showMenu = false
                        onToggleRead()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.isPinned) "Unpin" else "Pin to Top") },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Change Category") },
                    onClick = {
                        showMenu = false
                        onChangeCategory()
                    }
                )
            }
        }
    }
}

@Composable
fun AvatarCircle(
    name: String,
    category: com.pinnacle.smsorganizer.data.model.SmsCategory,
    modifier: Modifier = Modifier
) {
    val initial = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val color = when (category) {
        com.pinnacle.smsorganizer.data.model.SmsCategory.PERSONAL -> Color(0xFF1E88E5)
        com.pinnacle.smsorganizer.data.model.SmsCategory.TRANSACTIONS -> Color(0xFF2E7D32)
        com.pinnacle.smsorganizer.data.model.SmsCategory.OTPS -> Color(0xFFEF6C00)
        com.pinnacle.smsorganizer.data.model.SmsCategory.PROMOTIONS -> Color(0xFF7B1FA2)
        com.pinnacle.smsorganizer.data.model.SmsCategory.OTHER -> Color(0xFF546E7A)
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

fun formatMessageDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val dayMillis = 24 * 60 * 60 * 1000L

    return when {
        diff < dayMillis -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        diff < 2 * dayMillis -> "Yesterday"
        diff < 7 * dayMillis -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
