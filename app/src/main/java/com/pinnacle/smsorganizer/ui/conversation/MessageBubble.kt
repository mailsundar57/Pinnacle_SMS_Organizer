package com.pinnacle.smsorganizer.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinnacle.smsorganizer.data.model.SmsMessage
import com.pinnacle.smsorganizer.data.model.TransactionType
import com.pinnacle.smsorganizer.ui.theme.CategoryOtps
import com.pinnacle.smsorganizer.ui.theme.CategoryOtpsContainer
import com.pinnacle.smsorganizer.ui.theme.CategoryTransactions
import com.pinnacle.smsorganizer.ui.theme.CategoryTransactionsContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: SmsMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOutbound = message.isFromMe

    val bubbleShape = if (isOutbound) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val bubbleColor = if (isOutbound) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isOutbound) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = if (isOutbound) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(14.dp)
        ) {
            Column {
                // If message is an OTP: show prominent inline OTP action card
                message.otpInfo?.let { otp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CategoryOtpsContainer)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = CategoryOtps,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "One-Time Password",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CategoryOtps
                                    )
                                }
                                Text(
                                    text = otp.code,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFFD84315)
                                )
                            }

                            ElevatedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("OTP", otp.code)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "OTP ${otp.code} copied", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = CategoryOtps,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // If message is a transaction: show transaction details banner
                message.transactionInfo?.let { tx ->
                    if (tx.amount != null || tx.type != TransactionType.OTHER) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CategoryTransactionsContainer)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = CategoryTransactions,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${tx.type.name}: ${tx.currency ?: "₹"}${tx.amount ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CategoryTransactions
                                    )
                                    if (tx.accountNumber != null || tx.balance != null) {
                                        val sub = buildString {
                                            if (tx.accountNumber != null) append("A/C: ${tx.accountNumber} ")
                                            if (tx.balance != null) append("• Bal: ${tx.currency ?: "₹"}${tx.balance}")
                                        }
                                        Text(text = sub, fontSize = 11.sp, color = CategoryTransactions.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Main body text
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.date)),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
