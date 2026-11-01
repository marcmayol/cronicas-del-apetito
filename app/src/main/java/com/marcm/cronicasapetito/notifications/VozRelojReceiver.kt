package com.marcm.cronicasapetito.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.ui.Dictado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Anotar desde el reloj contestando al aviso con la voz.
 *
 * Los botones solo se ven en el reloj (`WearableExtender`): en el móvil el
 * aviso sigue igual, con su «Sí» que abre la pantalla completa. En el reloj,
 * en cambio, abrir el móvil es justo lo que se quiere evitar, así que se dicta
 * ahí mismo y se guarda con la hora en que se contesta.
 *
 * El dictado lo hace el reloj con su propio reconocedor, no esta app: a
 * diferencia del micro del móvil, no se puede asegurar que se quede dentro.
 */
class VozRelojReceiver : BroadcastReceiver() {

    companion object {
        const val CLAVE_TEXTO = "texto_dictado"
        private const val EXTRA_TIPO = "tipo"

        /** Las dos acciones de voz, listas para el `WearableExtender`. */
        fun acciones(context: Context): List<NotificationCompat.Action> {
            val conAnimo = PrefsRecordatorios.animoActivo(context)
            return buildList {
                add(accion(context, EntryKind.FOOD, R.string.watch_voice_food, R.string.watch_voice_food_prompt, 20))
                if (conAnimo) {
                    add(accion(context, EntryKind.MOOD, R.string.watch_voice_mood, R.string.watch_voice_mood_prompt, 21))
                }
            }
        }

        private fun accion(
            context: Context,
            tipo: String,
            titulo: Int,
            pregunta: Int,
            codigo: Int,
        ): NotificationCompat.Action {
            val intent = Intent(context, VozRelojReceiver::class.java).putExtra(EXTRA_TIPO, tipo)
            // MUTABLE a la fuerza: el sistema mete el texto dictado en este intent.
            val pi = PendingIntent.getBroadcast(
                context, codigo, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val entrada = RemoteInput.Builder(CLAVE_TEXTO)
                .setLabel(context.getString(pregunta))
                .build()
            return NotificationCompat.Action.Builder(0, context.getString(titulo), pi)
                .addRemoteInput(entrada)
                .setAllowGeneratedReplies(false)
                .build()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val texto = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(CLAVE_TEXTO)?.toString()
            ?.let { Dictado.unir("", it) }
            .orEmpty()
        if (texto.isBlank()) return

        val tipo = intent.getStringExtra(EXTRA_TIPO) ?: EntryKind.FOOD
        val pending = goAsync()
        val app = context.applicationContext as CronicasApp
        val repo = MealRepository(app.database.mealDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (tipo == EntryKind.MOOD) repo.addMood(texto) else repo.addFood(texto)
            } finally {
                MealNotifier.dismiss(context)
                pending.finish()
            }
        }
    }
}
