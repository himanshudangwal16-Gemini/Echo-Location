package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM saved_rooms ORDER BY timestamp DESC")
    fun getAllSavedRooms(): Flow<List<SavedRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: SavedRoom): Long

    @Delete
    suspend fun deleteRoom(room: SavedRoom)

    @Query("DELETE FROM saved_rooms WHERE id = :id")
    suspend fun deleteRoomById(id: Int)
}
