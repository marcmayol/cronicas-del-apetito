package com.marcm.cronicasapetito.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 a 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 16f

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
                    val lines = wrapText(firstLine, bodyPaint, PAGE_WIDTH - MARGIN * 2 - 60f)
                    val needed = LINE_HEIGHT * lines.size + 6f
                    if (y + needed > PAGE_HEIGHT - MARGIN) {
                        doc.finishPage(page)
                        pageNumber++
                        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                        canvas = page.canvas
                        y = MARGIN + 20f
                    }
                    canvas.drawText(hour, MARGIN, y, hourPaint)
                    lines.forEachIndexed { idx, line ->
                        canvas.drawText(line, MARGIN + 60f, y + idx * LINE_HEIGHT, bodyPaint)
                    }
                    y += LINE_HEIGHT * lines.size + 6f
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
