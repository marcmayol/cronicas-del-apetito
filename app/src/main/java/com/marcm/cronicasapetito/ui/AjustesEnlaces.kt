package com.marcm.cronicasapetito.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.BuildConfig
import com.marcm.cronicasapetito.R

/** La política de privacidad y las condiciones, publicadas junto a la web. */
private const val URL_PRIVACIDAD = "https://marcmayol.com/cronicas-del-apetito/privacidad.html"
private const val URL_CONDICIONES = "https://marcmayol.com/cronicas-del-apetito/condiciones.html"

/**
 * Enlaces del bloque «Acerca de»: valorar la app y los dos textos legales.
 *
 * Los legales están dentro de la app y no solo en la ficha de la tienda porque
 * quien ya la tiene instalada no vuelve a la ficha a buscarlos.
 *
 * Valorar abre la ficha, no el diálogo de valoración de Google: la In-App
 * Review **no puede colgar de un botón**. Su política dice que el diálogo se
 * pide en un momento natural del uso y sin pedirlo el usuario, y que además
 * Google decide si aparece —a menudo no lo hace—, así que un botón que a veces
 * no hace nada es peor que ninguno.
 */
@Composable
fun FilasEnlaces() {
    val context = LocalContext.current

    if (BuildConfig.URL_TIENDA.isNotEmpty()) {
        Separador()
        FilaEnlace(
            titulo = stringResource(R.string.about_rate),
            detalle = stringResource(R.string.about_rate_desc),
            onClick = { abrirFichaDeTienda(context) },
        )
    }
    Separador()
    FilaEnlace(
        titulo = stringResource(R.string.about_privacy),
        detalle = stringResource(R.string.about_privacy_desc),
        onClick = { abrirEnlace(context, URL_PRIVACIDAD) },
    )
    Separador()
    FilaEnlace(
        titulo = stringResource(R.string.about_terms),
        detalle = stringResource(R.string.about_terms_desc),
        onClick = { abrirEnlace(context, URL_CONDICIONES) },
    )
}

@Composable
private fun FilaEnlace(
    titulo: String,
    detalle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colorsCronicas.tenue,
        )
    }
}

/**
 * Abre la ficha en la app de Play si está, y si no en el navegador. El
 * `market://` importa: sin él, en muchos móviles la reseña se acaba escribiendo
 * en una pestaña del navegador, que es donde nadie la escribe.
 */
private fun abrirFichaDeTienda(context: Context) {
    val id = BuildConfig.URL_TIENDA.substringAfter("id=")
    val enPlay = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
    try {
        context.startActivity(enPlay)
    } catch (e: ActivityNotFoundException) {
        abrirEnlace(context, BuildConfig.URL_TIENDA)
    }
}

private fun abrirEnlace(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // Un móvil sin navegador es raro, pero el aviso es mejor que nada:
        // así al menos queda la dirección a la vista.
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}
