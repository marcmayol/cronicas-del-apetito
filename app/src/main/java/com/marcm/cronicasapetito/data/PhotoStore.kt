package com.marcm.cronicasapetito.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Guarda las fotos de las comidas en el almacenamiento interno de la app
 * (filesDir/photos), redimensionadas y comprimidas a JPEG para no llenar el disco.
 * Sin dependencias externas: todo con las APIs nativas de Android.
 */
object PhotoStore {

    private const val MAX_STORE_PX = 1280   // lado máximo al guardar
    private const val JPEG_QUALITY = 85
    private const val TAG = "PhotoStore"

    private fun photosDir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    /** Archivo temporal en caché donde la app de cámara escribirá la foto. */
    fun newCameraTempFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        return File(dir, "cam_${System.currentTimeMillis()}.jpg")
    }

    /** Uri content:// (vía FileProvider) para pasárselo a la app de cámara. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Importa una imagen desde un [Uri] (galería o cámara): la lee, corrige la
     * orientación EXIF, la redimensiona y la guarda como JPEG permanente.
     * Devuelve la ruta absoluta del archivo guardado, o null si falla.
     */
    suspend fun importFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = decodeSampled({ context.contentResolver.openInputStream(uri) }, MAX_STORE_PX)
                ?: return@runCatching null
            val oriented = applyExif(bmp) { context.contentResolver.openInputStream(uri) }
            writeJpeg(context, oriented)
        }.onFailure { Log.e(TAG, "importFromUri falló", it) }.getOrNull()
    }

    /** Igual que [importFromUri] pero desde un [File] (foto recién hecha con la cámara). */
    suspend fun importFromFile(context: Context, file: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = decodeSampled({ file.inputStream() }, MAX_STORE_PX)
                ?: return@runCatching null
            val oriented = applyExif(bmp) { file.inputStream() }
            writeJpeg(context, oriented).also { file.delete() }
        }.onFailure { Log.e(TAG, "importFromFile falló", it) }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** Decodifica un archivo ya guardado a un tamaño máximo [maxPx] (para mostrar/PDF). */
    fun decodeFile(path: String, maxPx: Int): Bitmap? =
        runCatching {
            val f = File(path)
            if (!f.exists()) return null
            decodeSampled({ f.inputStream() }, maxPx)
        }.getOrNull()

    // --- internos ---

    private fun writeJpeg(context: Context, bmp: Bitmap): String {
        val out = File(photosDir(context), "img_${UUID.randomUUID()}.jpg")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return out.absolutePath
    }

    private fun decodeSampled(streamProvider: () -> InputStream?, maxPx: Int): Bitmap? {
        // 1ª pasada: solo dimensiones. OJO: con inJustDecodeBounds, decodeStream
        // SIEMPRE devuelve null (solo rellena opts); el guard debe ser sobre el
        // stream, no sobre el bitmap.
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (streamProvider() ?: return null).use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

        var sample = 1
        val maxDim = maxOf(opts.outWidth, opts.outHeight)
        while (maxDim / sample > maxPx) sample *= 2

        // 2ª pasada: decodifica de verdad, submuestreado.
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return (streamProvider() ?: return null).use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    }

    private fun applyExif(bmp: Bitmap, streamProvider: () -> InputStream?): Bitmap {
        val orientation = runCatching {
            streamProvider()?.use { ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            ) }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bmp
        }
        return runCatching {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }.getOrDefault(bmp)
    }
}
