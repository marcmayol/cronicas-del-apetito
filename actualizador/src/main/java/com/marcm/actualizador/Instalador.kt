package com.marcm.actualizador

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Instalación del APK ya descargado y verificado, vía PackageInstaller por sesiones.
 *
 * Dos niveles de fricción:
 *  1) Permiso de "instalar apps desconocidas" (REQUEST_INSTALL_PACKAGES +
 *     canRequestPackageInstalls). Si falta, hay que mandar al usuario a ajustes
 *     con [intentPermisoInstalacion] (una sola vez; ese "una vez" lo controla la fachada).
 *  2) Confirmación de la instalación: si esta app es el "installer of record" de su
 *     propio paquete y la API lo permite (31+), la sesión pide NO requerir acción del
 *     usuario y la actualización se aplica en silencio. Si no lo es (p. ej. la primera
 *     vez, cuando la instaló adb o el sistema), PackageInstaller devuelve
 *     STATUS_PENDING_USER_ACTION y mostramos la confirmación del sistema. Traducción:
 *     la 1ª auto-actualización pide confirmar; a partir de ahí, silenciosas (31+).
 */
object Instalador {

    /** ¿Tiene la app permiso para instalar APKs de orígenes desconocidos? */
    fun puedeInstalar(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Intent a la pantalla de ajustes del permiso, apuntando a NUESTRA app. */
    fun intentPermisoInstalacion(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    /**
     * Crea una sesión, escribe el APK y hace commit. El resultado (confirmación
     * pendiente, éxito o fallo) llega a [InstallResultReceiver] vía el IntentSender.
     */
    fun instalar(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && somosInstalador(context)) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { entrada ->
                session.openWrite("base.apk", 0, apk.length()).use { salida ->
                    entrada.copyTo(salida)
                    session.fsync(salida)
                }
            }
            session.commit(pendingIntent(context, sessionId).intentSender)
        }
    }

    /** ¿Somos nosotros el instalador registrado de nuestro propio paquete? (API 30+) */
    private fun somosInstalador(context: Context): Boolean = try {
        context.packageManager
            .getInstallSourceInfo(context.packageName)
            .installingPackageName == context.packageName
    } catch (e: Exception) {
        false
    }

    private fun pendingIntent(context: Context, sessionId: Int): PendingIntent {
        val intent = Intent(context, InstallResultReceiver::class.java).apply {
            action = InstallResultReceiver.ACCION
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // El sistema rellena extras en el intent de estado: debe ser MUTABLE.
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, sessionId, intent, flags)
    }
}
