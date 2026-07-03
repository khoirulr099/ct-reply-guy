package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_history")
data class ReplyHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tweetContent: String,
    val replyContent: String,
    val modelUsed: String,
    val toneChosen: String,
    val timestamp: Long = System.currentTimeMillis()
)
