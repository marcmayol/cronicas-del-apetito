package com.marcm.cronicasapetito.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.ui.Dictado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Anotar desde el reloj contestando al aviso, sin sacar el móvil.
 *
 * Los botones solo se ven en el reloj (`WearableExtender`): en el móvil el
 * aviso sigue igual, con su «Sí» que abre la pantalla completa.
 *  - Comida: el cuadro de respuesta de abajo («¿Qué has comido?»); se dicta
 *    en el reloj y se guarda como comida.
 *  - «Caminata»: abre la pantalla de caminata en el móvil (MealNotifier).
 * Todo se guarda con la hora en que se contesta.
 *
 * El dictado lo hace el reloj con su propio reconocedor, no esta app: a
 * diferencia del micro del móvil, no se puede asegurar que se quede dentro.
 */
class VozRelojReceiver : BroadcastReceiver() {

    companion object {
        const val CLAVE_TEXTO = "texto_reloj"
        private const val EXTRA_TIPO = "tipo"
        private const val TIPO_COMIDA = "comida"

        /** La comida por voz, para el `WearableExtender` del aviso. */
        fun comida(context: Context): NotificationCompat.Action {
            val entrada = RemoteInput.Builder(CLAVE_TEXTO)
                .setLabel(context.getString(R.string.watch_voice_food_prompt))
                .build()
            return NotificationCompat.Action.Builder(
                0, context.getString(R.string.watch_voice_food), pendiente(context, TIPO_COMIDA, 20),
            )
                .addRemoteInput(entrada)
                .setAllowGeneratedReplies(false)
                // En el cuadro de respuesta de abajo (micro, teclado, emoji), no
                // como botón: en el Pixel Watch un botón que pide texto enseña el
                // dictado pero no devuelve nada; el cuadro de abajo sí.
                .extend(NotificationCompat.Action.WearableExtender().setHintDisplayActionInline(true))
                .build()
        }

        private fun pendiente(context: Context, tipo: String, codigo: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, codigo,
                Intent(context, VozRelojReceiver::class.java).putExtra(EXTRA_TIPO, tipo),
                // MUTABLE a la fuerza: el sistema mete la respuesta en este intent.
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )

    }

    override fun onReceive(context: Context, intent: Intent) {
        val texto = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(CLAVE_TEXTO)?.toString().orEmpty()
        if (texto.isBlank()) return

        val pending = goAsync()
        val app = context.applicationContext as CronicasApp
        val repo = MealRepository(app.database.mealDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.addFood(Dictado.unir("", texto))
            } finally {
                MealNotifier.dismiss(context)
                pending.finish()
            }
        }
    }
}
