package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Acción "Nada" de la notificación de comida: solo descarta el aviso.
 * Significa "ni he comido ni he caminado", no registra nada en el historial.
 */
class NothingLogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MealNotifier.dismiss(context)
    }
}
