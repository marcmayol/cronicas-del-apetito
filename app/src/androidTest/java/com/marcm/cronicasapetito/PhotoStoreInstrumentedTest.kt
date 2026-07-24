package com.marcm.cronicasapetito

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcm.cronicasapetito.data.PhotoStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifica en el dispositivo real el pipeline de guardado de fotos (el bug estaba
 * en decodeSampled: la 1ª pasada con inJustDecodeBounds devuelve null y se tomaba
 * como "sin imagen", así que NADA se guardaba). Simula una foto de cámara y una de
 * galería con imágenes reales generadas al vuelo.
 */
@RunWith(AndroidJUnit4::class)
class PhotoStoreInstrumentedTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun jpegDePrueba(nombre: String, ancho: Int, alto: Int): File {
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.rgb(200, 120, 40))
            drawCircle(ancho / 2f, alto / 2f, minOf(ancho, alto) / 3f, android.graphics.Paint().apply {
                color = Color.rgb(240, 220, 180)
            })
        }
        // Bajo camera/ para que quede cubierto por el FileProvider (file_paths.xml).
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, nombre)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()
        return f
    }

    @Test
    fun importFromFile_guarda_y_se_puede_volver_a_decodificar() = runBlocking<Unit> {
        val origen = jpegDePrueba("cam_test.jpg", 2400, 1800)

        val ruta = PhotoStore.importFromFile(ctx, origen)

        assertNotNull("importFromFile no debe devolver null (regresión del bug decodeSampled)", ruta)
        val guardado = File(ruta!!)
        assertTrue("el JPEG guardado debe existir", guardado.exists())
        assertTrue("el JPEG guardado no puede estar vacío", guardado.length() > 0)

        // Debe poder redecodificarse (miniatura de la lista y PDF).
        val reload = PhotoStore.decodeFile(ruta, 512)
        assertNotNull("decodeFile no debe devolver null tras guardar", reload)
        assertTrue("redimensionado al máximo esperado", reload!!.width <= 1280 && reload.height <= 1280)

        guardado.delete()
    }

    @Test
    fun importFromUri_desde_galeria_guarda() = runBlocking<Unit> {
        val origen = jpegDePrueba("gal_test.jpg", 1600, 1200)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", origen,
        )

        val ruta = PhotoStore.importFromUri(ctx, uri)

        assertNotNull("importFromUri (galería) no debe devolver null", ruta)
        val guardado = File(ruta!!)
        assertTrue(guardado.exists() && guardado.length() > 0)

        guardado.delete()
        origen.delete()
    }
}
