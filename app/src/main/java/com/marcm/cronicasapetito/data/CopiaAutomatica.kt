package com.marcm.cronicasapetito.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.ui.AjustesActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Copia automática: el mismo ZIP que «Guardar una copia», hecho solo, cada día
 * o cada semana, en una carpeta que elige quien usa la app.
 *
 * Es opcional y viene apagada. Nació de un cambio de móvil en el que la última
 * copia a mano tenía dos meses: todo lo apuntado después se perdió con el
 * móvil viejo. Sigue sin haber nube: la carpeta es del móvil (o de lo que el
 * propio móvil ofrezca en el selector), y la app solo escribe ahí.
 *
 * Se guardan las [CONSERVAR] últimas copias automáticas. Las que se hacen a
 * mano no se tocan nunca: llevan otro nombre.
 */
object CopiaAutomatica {

    enum class Frecuencia(val dias: Long) { DIARIA(1), SEMANAL(7) }

    data class Estado(
        val activa: Boolean,
        val carpeta: Uri?,
        val frecuencia: Frecuencia,
        /** Instante de la última copia buena; 0 si aún no hay ninguna. */
        val ultima: Long,
        /** True si la última vez que se intentó, falló. */
        val fallo: Boolean,
    )

    const val CONSERVAR = 4
    const val PREFIJO = "cronicas-apetito-auto-"

    private const val TAG = "CopiaAutomatica"
    private const val PREFS = "copia_automatica"
    private const val K_ACTIVA = "activa"
    private const val K_CARPETA = "carpeta"
    private const val K_FRECUENCIA = "frecuencia"
    private const val K_ULTIMA = "ultima"
    private const val K_FALLO = "fallo"
    private const val TRABAJO = "copia_automatica"
    private const val CANAL = "copia_automatica"
    private const val AVISO_ID = 3001

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun estado(context: Context): Estado {
        val p = prefs(context)
        return Estado(
            activa = p.getBoolean(K_ACTIVA, false),
            carpeta = p.getString(K_CARPETA, null)?.let(Uri::parse),
            frecuencia = p.getString(K_FRECUENCIA, null)
                ?.let { runCatching { Frecuencia.valueOf(it) }.getOrNull() }
                ?: Frecuencia.SEMANAL,
            ultima = p.getLong(K_ULTIMA, 0L),
            fallo = p.getBoolean(K_FALLO, false),
        )
    }

    /**
     * Enciende la copia con la carpeta recién elegida. El permiso sobre la
     * carpeta se pide persistente: sin eso caduca al reiniciar el móvil.
     */
    fun activar(context: Context, carpeta: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(carpeta, flags) }
        // Si había otra carpeta antes, se suelta: no tiene sentido acumular permisos.
        estado(context).carpeta
            ?.takeIf { it != carpeta }
            ?.let { vieja -> runCatching { context.contentResolver.releasePersistableUriPermission(vieja, flags) } }

        prefs(context).edit()
            .putBoolean(K_ACTIVA, true)
            .putString(K_CARPETA, carpeta.toString())
            .putBoolean(K_FALLO, false)
            .apply()
        programar(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun desactivar(context: Context) {
        prefs(context).edit().putBoolean(K_ACTIVA, false).putBoolean(K_FALLO, false).apply()
        WorkManager.getInstance(context).cancelUniqueWork(TRABAJO)
        NotificationManagerCompat.from(context).cancel(AVISO_ID)
    }

    fun cambiarFrecuencia(context: Context, frecuencia: Frecuencia) {
        prefs(context).edit().putString(K_FRECUENCIA, frecuencia.name).apply()
        if (estado(context).activa) programar(context, ExistingPeriodicWorkPolicy.UPDATE)
    }

    /**
     * Al arrancar la app: si está encendida, que el trabajo exista. WorkManager
     * ya lo conserva entre reinicios; esto cubre el caso raro de que se pierda.
     */
    fun asegurar(context: Context) {
        if (estado(context).activa) programar(context, ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun programar(context: Context, politica: ExistingPeriodicWorkPolicy) {
        val dias = estado(context).frecuencia.dias
        val trabajo = PeriodicWorkRequestBuilder<Trabajo>(dias, TimeUnit.DAYS)
            // Sin esto, WorkManager hace la primera en el acto, justo cuando la
            // pantalla ya ha hecho la suya al encenderla: dos copias seguidas.
            .setInitialDelay(dias, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(TRABAJO, politica, trabajo)
    }

    /**
     * Hace una copia ahora mismo. La usa el trabajo periódico y también la
     * pantalla de Ajustes, justo al encenderla, para que se vea que funciona.
     * Devuelve el resultado, o null si no se pudo escribir.
     */
    suspend fun copiarAhora(context: Context): RespaldoDatos.Resultado? {
        val resultado = runCatching { escribir(context) }
            .onFailure { Log.e(TAG, "falló la copia automática", it) }
            .getOrNull()
        val p = prefs(context).edit()
        if (resultado != null) {
            p.putLong(K_ULTIMA, System.currentTimeMillis()).putBoolean(K_FALLO, false)
            NotificationManagerCompat.from(context).cancel(AVISO_ID)
        } else {
            p.putBoolean(K_FALLO, true)
        }
        p.apply()
        return resultado
    }

    private suspend fun escribir(context: Context): RespaldoDatos.Resultado {
        val carpeta = estado(context).carpeta ?: error("sin carpeta")
        val dir = DocumentFile.fromTreeUri(context, carpeta)
        check(dir != null && dir.canWrite()) { "la carpeta ya no se puede escribir" }

        val nombre = PREFIJO + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".zip"
        // Si en el mismo minuto ya hay una (dos pulsaciones seguidas), se rehace.
        dir.findFile(nombre)?.delete()
        val archivo = dir.createFile("application/zip", nombre) ?: error("no se pudo crear $nombre")

        val repositorio = MealRepository((context.applicationContext as CronicasApp).database.mealDao())
        val resultado = try {
            val salida = context.contentResolver.openOutputStream(archivo.uri)
                ?: error("no se pudo abrir $nombre")
            salida.use { RespaldoDatos.exportar(repositorio.getAll(), it) }
        } catch (e: Exception) {
            // Un ZIP a medias no es una copia: mejor que no esté.
            archivo.delete()
            throw e
        }

        val nombres = dir.listFiles().mapNotNull { it.name }
        for (sobra in sobrantes(nombres)) dir.findFile(sobra)?.delete()
        return resultado
    }

    /**
     * De los nombres de una carpeta, qué copias automáticas sobran: todas menos
     * las [conservar] más nuevas. El nombre lleva la fecha en orden de lectura,
     * así que ordenar por nombre es ordenar por fecha. Lo demás no se toca.
     */
    fun sobrantes(nombres: List<String>, conservar: Int = CONSERVAR): List<String> =
        nombres
            .filter { it.startsWith(PREFIJO) && it.endsWith(".zip") }
            .sortedDescending()
            .drop(conservar)

    /**
     * Si una copia programada falla (carpeta borrada, tarjeta sacada…), se avisa:
     * una copia que deja de hacerse sin decir nada es justo lo que esto evita.
     */
    private fun avisarFallo(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CANAL,
                context.getString(R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        val abrir = PendingIntent.getActivity(
            context, 30,
            Intent(context, AjustesActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.autobackup_failed_title))
            .setContentText(context.getString(R.string.autobackup_failed_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.autobackup_failed_text))
            )
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(AVISO_ID, aviso) }
    }

    class Trabajo(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            val ctx = applicationContext
            if (!estado(ctx).activa) return Result.success()
            if (copiarAhora(ctx) == null) avisarFallo(ctx)
            // Sin reintentos: la siguiente toca en el siguiente periodo, y el
            // aviso ya ha dicho lo que pasa.
            return Result.success()
        }
    }
}
