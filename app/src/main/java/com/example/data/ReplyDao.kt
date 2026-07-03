package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyDao {
    @Query("SELECT * FROM reply_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ReplyHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReplyHistory)

    @Query("DELETE FROM reply_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM reply_history")
    suspend fun clearAllHistory()
}
