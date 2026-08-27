package com.marcm.cronicasapetito

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.marcm.actualizador.Modo
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.notifications.MealNotifier
import com.marcm.cronicasapetito.ui.BienvenidaActivity
import com.marcm.cronicasapetito.ui.CronicasTheme
import com.marcm.cronicasapetito.ui.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sin acción adicional, ya reprogramamos al abrir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = MealRepository((application as CronicasApp).database.mealDao())

        // La primera vez se pasa por la bienvenida, que ya pide el permiso y
        // deja los recordatorios montados. Quien venga actualizando no la ve:
        // se le marca como vista en cuanto se detecta que ya tiene registros.
        lifecycleScope.launch { abrirBienvenidaSiToca(repo) }

        MealNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)

        val actualizador = (application as CronicasApp).actualizador

        // Comprobación al abrir: en segundo plano, con un pequeño retardo. Silenciosa.
        lifecycleScope.launch {
            delay(3000)
            actualizador.comprobar(Modo.AUTOMATICO)
        }

        setContent {
            CronicasTheme {
                MainScreen(repository = repo, actualizador = actualizador)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Si el usuario volvió de conceder el permiso de instalación, reanuda el flujo.
        (application as CronicasApp).actualizador.onPermisoQuizaConcedido()
    }

    /**
     * Decide si toca la bienvenida. Tener registros es la señal de que se viene
     * de una versión anterior: entonces se da por vista y no se interrumpe a
     * quien ya lleva meses usando la app.
     */
    private suspend fun abrirBienvenidaSiToca(repo: MealRepository) {
        if (PrefsRecordatorios.bienvenidaVista(this)) {
            ensureNotificationPermission()
            return
        }
        val yaTieneRegistros = withContext(Dispatchers.IO) { repo.getAll().isNotEmpty() }
        if (yaTieneRegistros) {
            PrefsRecordatorios.setBienvenidaVista(this, true)
            ensureNotificationPermission()
        } else {
            startActivity(Intent(this, BienvenidaActivity::class.java))
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

}
