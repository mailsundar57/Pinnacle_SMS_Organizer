package com.pinnacle.smsorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thread_states")
data class ThreadStateEntity(
    @PrimaryKey
    val threadId: Long,
    val isReadOverride: Boolean? = null,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
