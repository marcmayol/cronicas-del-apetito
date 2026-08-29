package com.marcm.cronicasapetito.ui

import android.app.Application
import androidx.compose.runtime.Composable

/**
 * Lo único que el resto de la app sabe sobre actualizarse.
 *
 * Existe porque la app se distribuye por dos caminos con reglas opuestas:
 *
 *  - **fuera** (DracApps, marcmayol.com): se actualiza sola, comprobando un
 *    manifiesto y instalando el APK con PackageInstaller.
 *  - **play**: Google **prohíbe** justo eso. Su política de Device and Network
 *    Abuse impide que una app se actualice por una vía que no sea Play, y
 *    REQUEST_INSTALL_PACKAGES no se puede usar para auto-actualizarse. No es
 *    algo que se apruebe explicándolo bien: es motivo de retirada.
 *
 * Por eso el módulo :actualizador solo se enlaza en la variante «fuera» —así su
 * permiso ni siquiera entra en el manifiesto de la de Play— y todo lo que lo
 * toca vive detrás de esta interfaz.
 */
interface PuenteActualizador {

    /** Si esta variante se actualiza por su cuenta. En Play, siempre false. */
    val hayCanalPropio: Boolean

    /** Aviso de «hay una versión nueva» sobre la lista. */
    @Composable
    fun Banner()

    /** Bloque de Ajustes con el estado y la búsqueda manual. */
    @Composable
    fun SeccionAjustes()

    /** Comprobación silenciosa al abrir la app. */
    suspend fun comprobarAlAbrir()

    /** Al volver de la pantalla de permisos del sistema. */
    fun alVolverAlFrente()
}

/**
 * Sin canal propio: en Play la app se actualiza como cualquier otra, desde la
 * tienda, y aquí no hay nada que enseñar ni que comprobar.
 */
object SinCanalPropio : PuenteActualizador {
    override val hayCanalPropio = false

    @Composable
    override fun Banner() = Unit

    @Composable
    override fun SeccionAjustes() = Unit

    override suspend fun comprobarAlAbrir() = Unit

    override fun alVolverAlFrente() = Unit
}
