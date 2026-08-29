package com.marcm.cronicasapetito

import android.app.Application
import com.marcm.cronicasapetito.data.AppDatabase
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.GymNotifier
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealNotifier
import com.marcm.cronicasapetito.ui.Fechas
import com.marcm.cronicasapetito.ui.PuenteActualizador
import com.marcm.cronicasapetito.ui.TemaApp
import com.marcm.cronicasapetito.variante.crearPuenteActualizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CronicasApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    /** Cómo se actualiza esta variante. En Play, no se actualiza sola. */
    val actualizaciones: PuenteActualizador by lazy { crearPuenteActualizador(this) }

    override fun onCreate() {
        super.onCreate()
        // Los formatos de fecha salen de recursos: hay que darles el contexto
        // antes de que cualquier pantalla los pida.
        Fechas.init(this)
        // Antes de que se pinte nada: el tema elegido decide los colores.
        TemaApp.init(this)
        MealNotifier.ensureChannel(this)
        GymNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)
        GymAlarmScheduler.scheduleNext(this)

        // Las fotos de los registros borrados se quedan en el disco a propósito
        // —mientras se pueda deshacer, el archivo tiene que existir—, así que
        // las que ya no apunta nadie se recogen aquí, en el siguiente arranque.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                MealRepository(database.mealDao())
                    .limpiarFotosHuerfanas(java.io.File(filesDir, "photos"))
            }
        }
    }
}
