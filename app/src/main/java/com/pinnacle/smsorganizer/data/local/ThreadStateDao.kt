package com.pinnacle.smsorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadStateDao {

    @Query("SELECT * FROM thread_states")
    fun observeAllThreadStates(): Flow<List<ThreadStateEntity>>

    @Query("SELECT * FROM thread_states")
    suspend fun getAllThreadStates(): List<ThreadStateEntity>

    @Query("SELECT * FROM thread_states WHERE threadId = :threadId LIMIT 1")
    suspend fun getThreadState(threadId: Long): ThreadStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setThreadState(state: ThreadStateEntity)

    @Query("UPDATE thread_states SET isReadOverride = :isRead, updatedAt = :updatedAt WHERE threadId = :threadId")
    suspend fun updateReadStatus(threadId: Long, isRead: Boolean, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE thread_states SET isPinned = :isPinned, updatedAt = :updatedAt WHERE threadId = :threadId")
    suspend fun updatePinnedStatus(threadId: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM thread_states WHERE threadId = :threadId")
    suspend fun removeThreadState(threadId: Long)
}
