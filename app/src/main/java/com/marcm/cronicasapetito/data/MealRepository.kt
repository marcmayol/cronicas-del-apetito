package com.marcm.cronicasapetito.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MealRepository(private val dao: MealEntryDao) {
    fun observeAll(): Flow<List<MealEntry>> = dao.observeAll()

    suspend fun addFood(
        content: String,
        timestampMillis: Long = System.currentTimeMillis(),
        photoPath: String? = null
    ): Long =
        dao.insert(
            MealEntry(
                timestampMillis = timestampMillis,
                content = content,
                kind = EntryKind.FOOD,
                photoPath = photoPath
            )
        )

    suspend fun addWalk(minutes: Int, timestampMillis: Long = System.currentTimeMillis()): Long =
        dao.insert(
            MealEntry(
                timestampMillis = timestampMillis,
                content = "$minutes min",
                kind = EntryKind.WALK,
                minutes = minutes
            )
        )

    suspend fun addMood(content: String, timestampMillis: Long = System.currentTimeMillis()): Long =
        dao.insert(MealEntry(timestampMillis = timestampMillis, content = content, kind = EntryKind.MOOD))

    suspend fun addGym(went: Boolean, timestampMillis: Long = System.currentTimeMillis()): Long =
        dao.insert(
            MealEntry(
                timestampMillis = timestampMillis,
                content = if (went) "Sí" else "No",
                kind = EntryKind.GYM
            )
        )

    suspend fun getInRange(from: Long, to: Long): List<MealEntry> = dao.getInRange(from, to)
    suspend fun getAll(): List<MealEntry> = dao.getAll()

    /** True si ya existe algún registro de gimnasio en el día natural de [now]. */
    suspend fun hasGymEntryToday(now: Long = System.currentTimeMillis()): Boolean {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 24L * 60 * 60 * 1000 - 1
        return dao.countByKindInRange(EntryKind.GYM, start, end) > 0
    }

    /** True si [now] cae en sábado o domingo. */
    fun isWeekend(now: Long = System.currentTimeMillis()): Boolean {
        val dow = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    /** Nº de veces que se ha respondido "Sí" al gimnasio en la semana (lun-dom) de [now]. */
    suspend fun gymYesCountThisWeek(now: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val start = cal.timeInMillis
        val end = start + 7L * 24 * 60 * 60 * 1000 - 1
        return dao.countByKindAndContentInRange(EntryKind.GYM, "Sí", start, end)
    }

    /**
     * Decide si toca preguntar por el gimnasio en [now]. No preguntamos si:
     * - es fin de semana (sábado/domingo),
     * - ya hay un registro de gimnasio hoy,
     * - ya se ha ido al gimnasio (respondido "Sí") 2 veces esta semana.
     */
    suspend fun shouldAskGym(now: Long = System.currentTimeMillis()): Boolean {
        if (isWeekend(now)) return false
        if (hasGymEntryToday(now)) return false
        if (gymYesCountThisWeek(now) >= 2) return false
        return true
    }
}
