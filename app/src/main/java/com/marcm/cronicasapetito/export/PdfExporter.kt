package com.marcm.cronicasapetito.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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

object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 a 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 16f
    private const val IMG_SIZE = 96f     // lado de la miniatura en el PDF (pt)
    private const val IMG_GAP = 10f      // separación entre texto y foto

    suspend fun export(
        context: Context,
        entries: List<MealEntry>,
        from: Long?,
        to: Long?
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            color = 0xFF2A1B0E.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = 0xFF555555.toInt()
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val dayPaint = Paint().apply {
            color = 0xFF7A4E2D.toInt()
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hourPaint = Paint().apply {
            color = 0xFF2A1B0E.toInt()
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = 0xFF2A1B0E.toInt()
            textSize = 12f
            typeface = Typeface.DEFAULT
        }
        val imgPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        val imgBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = 0xFFBBA78F.toInt()
            isAntiAlias = true
        }

        val dayFormat = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es"))
        val hourFormat = SimpleDateFormat("HH:mm", Locale("es"))
        val rangeFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 20f

        // Cabecera
        canvas.drawText("Crónicas del Apetito", MARGIN, y, titlePaint); y += 22f
        val subtitle = if (from != null && to != null) {
            "Rango: ${rangeFormat.format(Date(from))} – ${rangeFormat.format(Date(to))}"
        } else {
            "Historial completo"
        }
        canvas.drawText(subtitle, MARGIN, y, subtitlePaint); y += 24f

        if (entries.isEmpty()) {
            canvas.drawText("Sin registros en este rango.", MARGIN, y, bodyPaint)
        } else {
            val grouped = entries.groupBy { dayFormat.format(Date(it.timestampMillis)) }
            for ((day, dayEntries) in grouped) {
                if (y > PAGE_HEIGHT - MARGIN - 60f) {
                    doc.finishPage(page)
                    pageNumber++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN + 20f
                }
                canvas.drawText(day.replaceFirstChar { it.uppercase() }, MARGIN, y, dayPaint)
                y += LINE_HEIGHT + 4f

                for (entry in dayEntries) {
                    val hour = hourFormat.format(Date(entry.timestampMillis))
                    val label = kindLabel(entry.kind)
                    val firstLine = "[$label] ${entry.content}"

                    // Foto a la derecha (si la hay): reservamos una columna cuadrada.
                    val photo = entry.photoPath?.let { PhotoStore.decodeFile(it, 260) }
                    val textLeft = MARGIN + 60f
                    val textRight = if (photo != null) PAGE_WIDTH - MARGIN - IMG_SIZE - IMG_GAP
                                    else PAGE_WIDTH - MARGIN

                    val lines = wrapText(firstLine, bodyPaint, textRight - textLeft)
                    val textHeight = LINE_HEIGHT * lines.size
                    val rowHeight = max(textHeight, if (photo != null) IMG_SIZE else 0f) + 8f

                    if (y + rowHeight > PAGE_HEIGHT - MARGIN) {
                        doc.finishPage(page)
                        pageNumber++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                        canvas = page.canvas
                        y = MARGIN + 20f
                    }
                    canvas.drawText(hour, MARGIN, y, hourPaint)
                    lines.forEachIndexed { idx, line ->
                        canvas.drawText(line, textLeft, y + idx * LINE_HEIGHT, bodyPaint)
                    }
                    if (photo != null) {
                        val left = PAGE_WIDTH - MARGIN - IMG_SIZE
                        val top = y - 11f   // el borde superior queda a la altura de la 1ª línea
                        val dst = RectF(left, top, left + IMG_SIZE, top + IMG_SIZE)
                        canvas.drawBitmap(photo, centerCropSquare(photo), dst, imgPaint)
                        canvas.drawRect(dst, imgBorderPaint)
                    }
                    y += rowHeight
                }
                y += 8f
            }
        }

        doc.finishPage(page)

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "cronicas_apetito_$stamp.pdf")
        outFile.outputStream().use { doc.writeTo(it) }
        doc.close()
        outFile
    }

    private fun kindLabel(kind: String): String = when (kind) {
        EntryKind.FOOD -> "Comida"
        EntryKind.WALK -> "Caminata"
        EntryKind.MOOD -> "Estado"
        EntryKind.GYM -> "Gimnasio"
        else -> kind
    }

    /** Rectángulo cuadrado centrado sobre el bitmap (recorte tipo "center-crop"). */
    private fun centerCropSquare(bmp: Bitmap): Rect {
        val side = min(bmp.width, bmp.height)
        val left = (bmp.width - side) / 2
        val top = (bmp.height - side) / 2
        return Rect(left, top, left + side, top + side)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines.ifEmpty { listOf("") }
    }
}
