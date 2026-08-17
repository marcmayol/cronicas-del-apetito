package com.marcm.cronicasapetito.data

import java.time.LocalDate

/**
 * Lo que se sabe de un día sin abrirlo. Es lo único que necesitan la vista
 * Semana (una fila por día), la vista Mes (una casilla por día) y las cabeceras
 * de lo que se comparte.
 *
 * Ojo: aquí no se inventa nada que no esté en la base de datos. Los cuatro tipos
 * guardan exactamente esto y nada más — no hay calorías, ni peso, ni escala de
 * ánimo.
 */
data class ResumenDia(
    val dia: LocalDate,
    val comidas: Int = 0,
    val minutosCaminados: Int = 0,
    val notasAnimo: Int = 0,
    /** null = no se preguntó / no se respondió ese día. */
    val gimnasio: Boolean? = null,
) {
    val vacio: Boolean
        get() = comidas == 0 && minutosCaminados == 0 && notasAnimo == 0 && gimnasio == null

    /** Tipos presentes ese día, en orden fijo: los glifos del calendario. */
    val tiposPresentes: List<String>
        get() = buildList {
            if (comidas > 0) add(EntryKind.FOOD)
            if (minutosCaminados > 0) add(EntryKind.WALK)
            if (notasAnimo > 0) add(EntryKind.MOOD)
            if (gimnasio == true) add(EntryKind.GYM)
        }
}

/** El mismo recuento para un periodo entero (una semana, un mes, un rango). */
data class ResumenPeriodo(
    val comidas: Int = 0,
    val minutosCaminados: Int = 0,
    val notasAnimo: Int = 0,
    val diasGimnasio: Int = 0,
    val registros: Int = 0,
)

object Resumenes {

    /**
     * Agrupa una lista de registros por día natural. Función pura: no toca Room
     * ni Compose, así que los bordes (cambio de mes, día vacío, gimnasio con
     * «No») se pueden probar con tests normales.
     *
     * El gimnasio guarda «Sí» o «No» como contenido; un «No» cuenta como día
     * respondido (gimnasio = false), no como día sin dato.
     */
    fun porDia(entries: List<MealEntry>): Map<LocalDate, ResumenDia> {
        val acumulado = mutableMapOf<LocalDate, ResumenDia>()
        for (entry in entries) {
            val dia = Periodos.fechaDe(entry.timestampMillis)
            val previo = acumulado[dia] ?: ResumenDia(dia)
            acumulado[dia] = when (entry.kind) {
                EntryKind.FOOD -> previo.copy(comidas = previo.comidas + 1)
                EntryKind.WALK -> previo.copy(
                    minutosCaminados = previo.minutosCaminados + (entry.minutes ?: 0)
                )
                EntryKind.MOOD -> previo.copy(notasAnimo = previo.notasAnimo + 1)
                EntryKind.GYM -> previo.copy(gimnasio = (previo.gimnasio ?: false) || entry.fueAlGimnasio)
                else -> previo
            }
        }
        return acumulado
    }

    /** Resumen de los días indicados; los que no tienen registros suman 0. */
    fun delPeriodo(resumenes: Map<LocalDate, ResumenDia>, dias: List<LocalDate>): ResumenPeriodo {
        var r = ResumenPeriodo()
        for (dia in dias) {
            val d = resumenes[dia] ?: continue
            r = r.copy(
                comidas = r.comidas + d.comidas,
                minutosCaminados = r.minutosCaminados + d.minutosCaminados,
                notasAnimo = r.notasAnimo + d.notasAnimo,
                diasGimnasio = r.diasGimnasio + if (d.gimnasio == true) 1 else 0,
            )
        }
        return r
    }
}

/** «Sí» es la respuesta afirmativa que guarda [MealRepository.addGym]. */
val MealEntry.fueAlGimnasio: Boolean
    get() = kind == EntryKind.GYM && content.trim().equals("Sí", ignoreCase = true)
