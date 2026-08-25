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

/**
 * Respuesta del gimnasio tal y como se guarda. Es un valor, no un texto: hasta
 * la v2.1 se guardaba «Sí»/«No» y eso ataba los datos al idioma — al traducir
 * la app, el contador semanal habría dejado de encontrar los registros viejos.
 *
 * [LEGACY_SI] sigue reconociéndose al leer por si alguna base se queda sin
 * migrar; nunca se escribe.
 */
object GymAnswer {
    const val YES = "yes"
    const val NO = "no"
    const val LEGACY_SI = "Sí"

    /** True si [content] es un «he ido», lo escribiera la versión que lo escribiera. */
    fun esAfirmativo(content: String): Boolean {
        val limpio = content.trim()
        return limpio.equals(YES, ignoreCase = true) || limpio.equals(LEGACY_SI, ignoreCase = true)
    }
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
