package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga el bitmap de una foto local fuera del hilo principal, cacheado por ruta. */
@Composable
fun rememberPhotoBitmap(path: String?, maxPx: Int = 1024): ImageBitmap? =
    produceState<ImageBitmap?>(initialValue = null, path, maxPx) {
        value = if (path.isNullOrBlank()) null
        else withContext(Dispatchers.IO) {
            PhotoStore.decodeFile(path, maxPx)?.asImageBitmap()
        }
    }.value

/**
 * Muestra la foto de una comida. Mientras carga (o si falla) deja un hueco
 * discreto con las esquinas redondeadas.
 */
@Composable
fun PhotoThumb(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    cornerRadiusDp: Int = 12,
    maxPx: Int = 1024
) {
    val bitmap = rememberPhotoBitmap(path, maxPx)
    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadiusDp.dp)),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de la comida",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
