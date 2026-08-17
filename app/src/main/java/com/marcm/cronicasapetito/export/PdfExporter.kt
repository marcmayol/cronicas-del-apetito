package com.marcm.cronicasapetito.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * PDF A4 de lo que se está mirando.
 *
 * Requisito duro: esto acaba impreso o fotocopiado en la consulta, así que cada
 * tipo lleva su **glifo** delante de la etiqueta y hay una leyenda al pie de cada
 * página. En blanco y negro la información sigue estando toda.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 a 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44f
    private const val LINE_HEIGHT = 15f
    private const val IMG_SIZE = 84f
    private const val IMG_GAP = 10f

    private const val TINTA = 0xFF2A2018.toInt()
    private const val TINTA_SUAVE = 0xFF5A4A38.toInt()
    private const val TENUE = 0xFFA19281.toInt()
    private const val GRIS_ETIQUETA = 0xFF8A7A66.toInt()
    private const val LINEA = 0xFFE4D6C1.toInt()
    private const val LINEA_SUAVE = 0xFFF1E9DC.toInt()

    private val glifos = mapOf(
        EntryKind.FOOD to "●",
        EntryKind.WALK to "▲",
        EntryKind.MOOD to "◆",
        EntryKind.GYM to "■",
    )
    private val colores = mapOf(
        EntryKind.FOOD to 0xFF9A5B2F.toInt(),
        EntryKind.WALK to 0xFF5C7549.toInt(),
        EntryKind.MOOD to 0xFF7B5C90.toInt(),
        EntryKind.GYM to 0xFF47698C.toInt(),
    )
    private const val LEYENDA = "● comida   ▲ caminata   ◆ ánimo   ■ gimnasio"

    /**
     * @param titulo lo que se está mirando, tal cual se lee en la app
     *   («Semana del 10 al 16 de agosto», «Agosto 2026», «Historial completo»).
     */
    suspend fun export(
        context: Context,
        entries: List<MealEntry>,
        titulo: String,
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val p = Pinceles(context)

        val es = Locale("es")
        val dayFormat = SimpleDateFormat("EEEE d 'de' MMMM yyyy", es)
        val hourFormat = SimpleDateFormat("HH:mm", es)
        val generado = SimpleDateFormat("dd/MM/yyyy", es).format(Date())

        var pagina = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pagina).create())
        var canvas = page.canvas
        var y = cabecera(canvas, p, titulo, pagina)

        fun nuevaPagina() {
            pie(canvas, p, generado)
            doc.finishPage(page)
            pagina++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pagina).create()
            )
            canvas = page.canvas
            y = cabecera(canvas, p, titulo, pagina)
        }

        if (entries.isEmpty()) {
            canvas.drawText("Sin registros en este periodo.", MARGIN, y + 24f, p.cuerpo)
        } else {
            val porDia = entries.groupBy { dayFormat.format(Date(it.timestampMillis)) }
            for ((dia, registros) in porDia) {
                if (y > PAGE_HEIGHT - MARGIN - 80f) nuevaPagina()

                y += 26f
                canvas.drawText(dia.replaceFirstChar { it.uppercase() }, MARGIN, y, p.dia)
                y += 6f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, p.linea)
                y += 6f

                for (entry in registros) {
                    val glifo = glifos[entry.kind] ?: "·"
                    val etiqueta = "$glifo ${etiquetaDe(entry.kind)}"
                    val foto = entry.photoPath?.let { PhotoStore.decodeFile(it, 240) }

                    val textoIzquierda = MARGIN + 132f
                    val textoDerecha =
                        if (foto != null) PAGE_WIDTH - MARGIN - IMG_SIZE - IMG_GAP
                        else PAGE_WIDTH - MARGIN
                    val lineas = partir(contenidoDe(entry), p.cuerpo, textoDerecha - textoIzquierda)
                    val alto = max(LINE_HEIGHT * lineas.size, if (foto != null) IMG_SIZE else 0f) + 10f

                    if (y + alto > PAGE_HEIGHT - MARGIN - 40f) nuevaPagina()

                    y += 14f
                    canvas.drawText(hourFormat.format(Date(entry.timestampMillis)), MARGIN, y, p.hora)
                    p.etiqueta.color = colores[entry.kind] ?: TINTA_SUAVE
                    canvas.drawText(etiqueta, MARGIN + 44f, y, p.etiqueta)
                    lineas.forEachIndexed { i, linea ->
                        canvas.drawText(linea, textoIzquierda, y + i * LINE_HEIGHT, p.cuerpo)
                    }
                    if (foto != null) {
                        val izquierda = PAGE_WIDTH - MARGIN - IMG_SIZE
                        val arriba = y - 10f
                        val destino = RectF(izquierda, arriba, izquierda + IMG_SIZE, arriba + IMG_SIZE)
                        canvas.drawBitmap(foto, recorteCuadrado(foto), destino, p.imagen)
                        canvas.drawRect(destino, p.bordeImagen)
                    }
                    y += alto - 6f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, p.lineaSuave)
                }
            }
        }

        pie(canvas, p, generado)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sello = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "cronicas_apetito_$sello.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        file
    }

    // -----------------------------------------------------------------------

    private fun cabecera(
        canvas: android.graphics.Canvas,
        p: Pinceles,
        titulo: String,
        pagina: Int,
    ): Float {
        var y = MARGIN + 14f
        canvas.drawText("CRÓNICAS DEL APETITO", MARGIN, y, p.sobretitulo)
        y += 22f
        canvas.drawText(titulo, MARGIN, y, p.titulo)
        canvas.drawText("pág. $pagina", PAGE_WIDTH - MARGIN, y, p.paginaNum)
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, p.lineaFuerte)
        return y
    }

    private fun pie(canvas: android.graphics.Canvas, p: Pinceles, generado: String) {
        val y = PAGE_HEIGHT - MARGIN + 6f
        canvas.drawLine(MARGIN, y - 14f, PAGE_WIDTH - MARGIN, y - 14f, p.linea)
        canvas.drawText(LEYENDA, MARGIN, y, p.pie)
        canvas.drawText("generado el $generado", PAGE_WIDTH - MARGIN, y, p.pieDerecha)
    }

    private fun etiquetaDe(kind: String): String = when (kind) {
        EntryKind.FOOD -> "Comida"
        EntryKind.WALK -> "Caminata"
        EntryKind.MOOD -> "Ánimo"
        EntryKind.GYM -> "Gimnasio"
        else -> kind
    }

    private fun contenidoDe(entry: MealEntry): String = when (entry.kind) {
        EntryKind.WALK -> entry.minutes?.let { "$it minutos" } ?: entry.content
        else -> entry.content
    }

    private fun recorteCuadrado(bmp: Bitmap): Rect {
        val lado = min(bmp.width, bmp.height)
        val izquierda = (bmp.width - lado) / 2
        val arriba = (bmp.height - lado) / 2
        return Rect(izquierda, arriba, izquierda + lado, arriba + lado)
    }

    private fun partir(texto: String, paint: Paint, anchoMax: Float): List<String> {
        val palabras = texto.split(" ")
        val lineas = mutableListOf<String>()
        var actual = StringBuilder()
        for (palabra in palabras) {
            val candidata = if (actual.isEmpty()) palabra else "$actual $palabra"
            if (paint.measureText(candidata) <= anchoMax) {
                actual = StringBuilder(candidata)
            } else {
                if (actual.isNotEmpty()) lineas += actual.toString()
                actual = StringBuilder(palabra)
            }
        }
        if (actual.isNotEmpty()) lineas += actual.toString()
        return lineas.ifEmpty { listOf("") }
    }

    private class Pinceles(context: Context) {
        private val lora: Typeface? = ResourcesCompat.getFont(context, R.font.lora)
        private val serif = lora ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)

        val sobretitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRIS_ETIQUETA; textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA; textSize = 17f; typeface = serif
        }
        val paginaNum = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TENUE; textSize = 9f; textAlign = Paint.Align.RIGHT
        }
        val dia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA; textSize = 12f; typeface = serif
        }
        val hora = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA_SUAVE; textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val etiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TINTA; textSize = 10.5f }
        val pie = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TENUE; textSize = 8f }
        val pieDerecha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TENUE; textSize = 8f; textAlign = Paint.Align.RIGHT
        }
        val linea = Paint().apply { color = LINEA; strokeWidth = 0.7f }
        val lineaSuave = Paint().apply { color = LINEA_SUAVE; strokeWidth = 0.6f }
        val lineaFuerte = Paint().apply { color = TINTA; strokeWidth = 1.6f }
        val imagen = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        val bordeImagen = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f
            color = LINEA; isAntiAlias = true
        }
    }
}
