package com.marcm.cronicasapetito.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    @Insert
    suspend fun insert(entry: MealEntry): Long

    @Query("SELECT * FROM meal_entries ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE timestampMillis BETWEEN :from AND :to ORDER BY timestampMillis ASC")
    suspend fun getInRange(from: Long, to: Long): List<MealEntry>

    @Query("SELECT * FROM meal_entries ORDER BY timestampMillis ASC")
    suspend fun getAll(): List<MealEntry>

    @Query("SELECT COUNT(*) FROM meal_entries WHERE kind = :kind AND timestampMillis BETWEEN :from AND :to")
    suspend fun countByKindInRange(kind: String, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM meal_entries WHERE kind = :kind AND content = :content AND timestampMillis BETWEEN :from AND :to")
    suspend fun countByKindAndContentInRange(kind: String, content: String, from: Long, to: Long): Int
}
