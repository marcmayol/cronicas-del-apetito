package com.marcm.cronicasapetito.variante

import android.app.Application
import com.marcm.cronicasapetito.ui.PuenteActualizador
import com.marcm.cronicasapetito.ui.SinCanalPropio

/**
 * Variante de Google Play.
 *
 * Aquí no se enlaza el módulo :actualizador, así que su permiso
 * REQUEST_INSTALL_PACKAGES no llega ni al manifiesto. La app se actualiza desde
 * la tienda, como cualquier otra: intentar hacerlo por nuestra cuenta va contra
 * la política de Device and Network Abuse.
 */
fun crearPuenteActualizador(app: Application): PuenteActualizador = SinCanalPropio
