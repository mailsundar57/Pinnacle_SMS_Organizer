package com.pinnacle.smsorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_overrides")
data class CategoryOverrideEntity(
    @PrimaryKey
    val senderAddress: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)
