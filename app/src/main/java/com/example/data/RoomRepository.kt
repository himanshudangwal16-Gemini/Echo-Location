package com.example.data

import kotlinx.coroutines.flow.Flow

class RoomRepository(private val roomDao: RoomDao) {
    val allSavedRooms: Flow<List<SavedRoom>> = roomDao.getAllSavedRooms()

    suspend fun insertRoom(room: SavedRoom): Long {
        return roomDao.insertRoom(room)
    }

    suspend fun deleteRoom(room: SavedRoom) {
        roomDao.deleteRoom(room)
    }

    suspend fun deleteRoomById(id: Int) {
        roomDao.deleteRoomById(id)
    }
}
