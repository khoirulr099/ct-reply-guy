package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ReplyRepository(private val replyDao: ReplyDao) {
    val allHistory: Flow<List<ReplyHistory>> = replyDao.getAllHistory()

    suspend fun insert(history: ReplyHistory) = withContext(Dispatchers.IO) {
        replyDao.insertHistory(history)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        replyDao.deleteHistoryById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        replyDao.clearAllHistory()
    }
}
