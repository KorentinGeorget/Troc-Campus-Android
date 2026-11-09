package com.korentingeorget.troccampus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnonceDao {
    @Query("SELECT * FROM annonces ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<AnnonceEntity>>

    @Query("SELECT * FROM annonces WHERE type = :type ORDER BY pinned DESC, createdAt DESC")
    fun byType(type: String): Flow<List<AnnonceEntity>>

    @Query("SELECT * FROM annonces WHERE id = :id")
    suspend fun get(id: Long): AnnonceEntity?

    @Query("SELECT COUNT(*) FROM annonces")
    fun count(): Flow<Int>

    @Query("SELECT * FROM annonces WHERE synced = 0 ORDER BY createdAt")
    suspend fun unsynced(): List<AnnonceEntity>

    @Query("SELECT COUNT(*) FROM annonces WHERE synced = 0")
    fun unsyncedCount(): Flow<Int>

    @Query("UPDATE annonces SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Upsert
    suspend fun upsert(entity: AnnonceEntity): Long

    @Delete
    suspend fun delete(entity: AnnonceEntity)

    @Query("DELETE FROM annonces WHERE id = :id")
    suspend fun deleteById(id: Long)
}
