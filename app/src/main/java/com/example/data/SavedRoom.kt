package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_rooms")
data class SavedRoom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val roomType: String, // 'corridor', 'living_room', 't_junction', 'open_space'
    val obstacleCount: Int,
    val notes: String = "",
    val maxDistance: Float = 5.0f,
    val avgConfidence: Float = 0.95f
)
