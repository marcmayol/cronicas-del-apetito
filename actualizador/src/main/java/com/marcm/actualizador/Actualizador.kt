package com.marcm.actualizador

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Punto de entrada del módulo. Orquesta comprobación, descarga, verificación e
 * instalación, y expone un [estado] observable para la UI. Tolerancia a fallos:
 * en modo AUTOMATICO cualquier error muere en silencio; solo el MANUAL informa.
 */
class Actualizador(
    private val app: Application,
    val config: ActualizadorConfig,
    private val cliente: ClienteManifiesto = ClienteManifiesto(HttpUrlConnectionTransport()),
    private val descargador: Descargador = Descargador(),
) {
    private val prefs = PrefsActualizador(app)

    private val _estado = MutableStateFlow<EstadoActualizacion>(EstadoActualizacion.Inactivo)
    val estado: StateFlow<EstadoActualizacion> = _estado.asStateFlow()

    /** Ajuste "buscar actualizaciones" (activado por defecto). */
    var buscarAutomaticamente: Boolean
        get() = prefs.buscarActivado
        set(v) { prefs.buscarActivado = v }

    init {
        instancia = this
        // Si un worker previo dejó una versión disponible, muéstrala ya en el banner.
        prefs.leerDisponible()?.let { info ->
            if (ComparadorVersion.hayNovedad(info.versionCode, config.versionCodeActual)) {
                _estado.value = EstadoActualizacion.Disponible(info)
            } else {
                prefs.guardarDisponible(null)
            }
        }
        EventosInstalacion.onResultado = { exito, mensaje ->
            if (exito) {
                prefs.guardarDisponible(null)
                _estado.value = EstadoActualizacion.AlDia
            } else {
                _estado.value = EstadoActualizacion.Error(TipoError.INSTALACION, mensaje)
            }
        }
    }

    /**
     * Comprueba el manifiesto. En AUTOMATICO respeta el ajuste y calla los errores;
     * en MANUAL siempre comprueba e informa del resultado ("al día" o error).
     */
    suspend fun comprobar(modo: Modo) {
        if (modo == Modo.AUTOMATICO && !prefs.buscarActivado) return
        _estado.value = EstadoActualizacion.Comprobando
        val manifiesto = try {
            withContext(Dispatchers.IO) { cliente.obtener(config.manifiestoUrl) }
        } catch (e: ManifiestoError) {
            _estado.value = estadoTrasError(modo, e)
            return
        } catch (e: Exception) {
            // Cualquier otro fallo inesperado también respeta la tolerancia a fallos.
            _estado.value = if (modo == Modo.MANUAL) {
                EstadoActualizacion.Error(TipoError.MANIFIESTO)
            } else {
                EstadoActualizacion.Inactivo
            }
            return
        }
        prefs.checkHoras = manifiesto.checkHoras
        val nuevo = estadoTrasManifiesto(modo, manifiesto, config.versionCodeActual)
        prefs.guardarDisponible((nuevo as? EstadoActualizacion.Disponible)?.info)
        _estado.value = nuevo
    }

    /** (Re)programa la comprobación periódica según el check_horas conocido. */
    fun programarPeriodica() {
        val horas = prefs.checkHoras.takeIf { it > 0 }?.toLong()
            ?: config.checkHorasPorDefecto.toLong()
        ComprobacionWorker.programar(app, horas)
    }

    /**
     * Ejecuta el flujo completo: permiso → descarga → verificación → instalación.
     * Si falta el permiso, manda al usuario a ajustes y espera a
     * [onPermisoQuizaConcedido]; el resto continúa en cuanto haya permiso.
     */
    fun actualizarAhora(scope: CoroutineScope) {
        val info = infoDisponible() ?: return

        if (!Instalador.puedeInstalar(app)) {
            _estado.value = EstadoActualizacion.PidiendoPermiso
            prefs.permisoPedidoUnaVez = true
            app.startActivity(
                Instalador.intentPermisoInstalacion(app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        scope.launch {
            val dir = File(app.cacheDir, "actualizaciones").apply { mkdirs() }
            val part = File(dir, "${info.versionCode}.apk.part")
            val apk = File(dir, "${info.versionCode}.apk")

            _estado.value = EstadoActualizacion.Descargando(0)
            val res = descargador.descargar(info.url, part) { pct ->
                _estado.value = EstadoActualizacion.Descargando(pct)
            }
            if (res !is ResultadoDescarga.Ok) {
                _estado.value = EstadoActualizacion.Error(TipoError.DESCARGA)
                return@launch
            }

            _estado.value = EstadoActualizacion.Verificando
            if (!VerificadorSha.verificarOBorrar(part, info.sha256)) {
                _estado.value = EstadoActualizacion.Error(TipoError.HASH)
                return@launch
            }
            apk.delete()
            part.renameTo(apk)

            _estado.value = EstadoActualizacion.Instalando
            try {
                Instalador.instalar(app, apk)
                // El resultado final llega por EventosInstalacion.
            } catch (e: Exception) {
                _estado.value = EstadoActualizacion.Error(TipoError.INSTALACION, e.message)
            }
        }
    }

    /** Llamar en onResume tras volver de la pantalla de permiso: reanuda si se concedió. */
    fun onPermisoQuizaConcedido(scope: CoroutineScope) {
        if (_estado.value == EstadoActualizacion.PidiendoPermiso && Instalador.puedeInstalar(app)) {
            actualizarAhora(scope)
        }
    }

    private fun infoDisponible(): InfoActualizacion? =
        (_estado.value as? EstadoActualizacion.Disponible)?.info ?: prefs.leerDisponible()

    companion object {
        @Volatile
        private var instancia: Actualizador? = null

        /** La usa el worker de segundo plano para alcanzar la fachada ya construida. */
        internal fun instancia(): Actualizador? = instancia
    }
}
