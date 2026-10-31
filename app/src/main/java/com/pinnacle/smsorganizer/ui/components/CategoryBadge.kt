package com.pinnacle.smsorganizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.ui.theme.CategoryOther
import com.pinnacle.smsorganizer.ui.theme.CategoryOtherContainer
import com.pinnacle.smsorganizer.ui.theme.CategoryOtps
import com.pinnacle.smsorganizer.ui.theme.CategoryOtpsContainer
import com.pinnacle.smsorganizer.ui.theme.CategoryPersonal
import com.pinnacle.smsorganizer.ui.theme.CategoryPersonalContainer
import com.pinnacle.smsorganizer.ui.theme.CategoryPromotions
import com.pinnacle.smsorganizer.ui.theme.CategoryPromotionsContainer
import com.pinnacle.smsorganizer.ui.theme.CategoryTransactions
import com.pinnacle.smsorganizer.ui.theme.CategoryTransactionsContainer

@Composable
fun CategoryBadge(
    category: SmsCategory,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor) = when (category) {
        SmsCategory.PERSONAL -> CategoryPersonalContainer to CategoryPersonal
        SmsCategory.TRANSACTIONS -> CategoryTransactionsContainer to CategoryTransactions
        SmsCategory.OTPS -> CategoryOtpsContainer to CategoryOtps
        SmsCategory.PROMOTIONS -> CategoryPromotionsContainer to CategoryPromotions
        SmsCategory.OTHER -> CategoryOtherContainer to CategoryOther
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category.title,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
