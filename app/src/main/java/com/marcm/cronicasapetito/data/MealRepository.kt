package com.marcm.cronicasapetito.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MealRepository(private val dao: MealEntryDao) {
    fun observeAll(): Flow<List<MealEntry>> = dao.observeAll()

    fun observeInRange(from: Long, to: Long): Flow<List<MealEntry>> = dao.observeInRange(from, to)

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

    /** Día de la semana de [now] en las constantes de [Calendar.DAY_OF_WEEK]. */
    fun diaDeLaSemana(now: Long = System.currentTimeMillis()): Int =
        Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)

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
     * - hoy no es uno de los [dias] elegidos en Ajustes,
     * - ya hay un registro de gimnasio hoy,
     * - ya se ha alcanzado el [objetivoSemanal] de veces esta semana
     *   (con 0 no hay tope: pregunta siempre que toque).
     */
    suspend fun shouldAskGym(
        dias: Set<Int>,
        objetivoSemanal: Int,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (diaDeLaSemana(now) !in dias) return false
        if (hasGymEntryToday(now)) return false
        if (objetivoSemanal > 0 && gymYesCountThisWeek(now) >= objetivoSemanal) return false
        return true
    }
}
