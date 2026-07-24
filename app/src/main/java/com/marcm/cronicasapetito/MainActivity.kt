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
import com.marcm.cronicasapetito.notifications.MealNotifier
import com.marcm.cronicasapetito.ui.CronicasTheme
import com.marcm.cronicasapetito.ui.MainScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sin acción adicional, ya reprogramamos al abrir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ensureNotificationPermission()
        MealNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)

        val repo = MealRepository((application as CronicasApp).database.mealDao())
        val actualizador = (application as CronicasApp).actualizador

        // Comprobación al abrir: en segundo plano, con un pequeño retardo. Silenciosa.
        lifecycleScope.launch {
            delay(3000)
            actualizador.comprobar(Modo.AUTOMATICO)
        }

        setContent {
            CronicasTheme {
                MainScreen(
                    repository = repo,
                    actualizador = actualizador,
                    onRequestExactAlarmPermission = { openExactAlarmSettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Si el usuario volvió de conceder el permiso de instalación, reanuda el flujo.
        (application as CronicasApp).actualizador.onPermisoQuizaConcedido()
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

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
