package com.marcm.cronicasapetito.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object EntryKind {
    const val FOOD = "food"
    const val WALK = "walk"
    const val MOOD = "mood"
    const val GYM = "gym"
}

@Entity(tableName = "meal_entries")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val content: String,
    @ColumnInfo(name = "kind", defaultValue = EntryKind.FOOD) val kind: String = EntryKind.FOOD,
    @ColumnInfo(name = "minutes") val minutes: Int? = null,
    @ColumnInfo(name = "photoPath") val photoPath: String? = null
)
