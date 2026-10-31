package com.pinnacle.smsorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryOverrideDao {

    @Query("SELECT * FROM category_overrides")
    fun observeAllOverrides(): Flow<List<CategoryOverrideEntity>>

    @Query("SELECT * FROM category_overrides")
    suspend fun getAllOverrides(): List<CategoryOverrideEntity>

    @Query("SELECT * FROM category_overrides WHERE UPPER(senderAddress) = UPPER(:senderAddress) LIMIT 1")
    suspend fun getOverrideForSender(senderAddress: String): CategoryOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOverride(override: CategoryOverrideEntity)

    @Query("DELETE FROM category_overrides WHERE UPPER(senderAddress) = UPPER(:senderAddress)")
    suspend fun removeOverride(senderAddress: String)

    @Query("DELETE FROM category_overrides")
    suspend fun clearAllOverrides()
}
