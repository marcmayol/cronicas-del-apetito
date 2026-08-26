package com.marcm.cronicasapetito.ui

import android.content.Context
import android.content.Intent
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealEntry

/** Id del registro que se está corrigiendo; ausente al anotar uno nuevo. */
const val EXTRA_EDITAR_ID = "editar_id"

/**
 * Cada tipo se corrige en la misma pantalla en la que se anota, con sus campos
 * ya rellenos. Es lo contrario de tener una pantalla de edición aparte: la que
 * ya conoce el usuario, y una sola forma de hacer las cosas.
 */
fun intentDeEdicion(context: Context, entry: MealEntry): Intent? = when (entry.kind) {
    EntryKind.WALK, EntryKind.MOOD ->
        Intent(context, WalkMoodActivity::class.java).apply {
            putExtra(EXTRA_EDITAR_ID, entry.id)
            putExtra(WalkMoodActivity.EXTRA_SOLO_ANIMO, entry.kind == EntryKind.MOOD)
            putExtra(WalkMoodActivity.EXTRA_START_AT_MINUTES, entry.kind == EntryKind.WALK)
        }

    EntryKind.FOOD -> Intent(context, EntryActivity::class.java).apply {
        putExtra(EXTRA_EDITAR_ID, entry.id)
    }

    // El gimnasio es un sí/no: no hay pantalla que abrir, se cambia en el mismo
    // diálogo de dos botones con el que se anotó. Lo resuelve quien llama.
    else -> null
}
