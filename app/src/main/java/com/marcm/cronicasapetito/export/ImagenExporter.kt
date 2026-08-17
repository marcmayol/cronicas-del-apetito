package com.marcm.cronicasapetito.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.Periodos
import com.marcm.cronicasapetito.data.RangoFechas
import com.marcm.cronicasapetito.data.ResumenDia
import com.marcm.cronicasapetito.data.ResumenPeriodo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dibuja la semana o el mes como imagen para compartir.
 *
 * Se dibuja a mano en un [Canvas] del tamaño final en vez de capturar la
 * pantalla: capturar el móvil daría una imagen con el scroll cortado y el ancho
 * del teléfono. Aquí el resultado es siempre el mismo, independientemente del
 * dispositivo y del zoom de fuente que tenga puesto el usuario.
 *
 * Los cuatro tipos se distinguen por **glifo** además de por color, porque esto
 * acaba impreso o fotocopiado en la consulta.
 */
object ImagenExporter {

    private const val ANCHO = 1080
    /** Cabecera + resumen + pie; las siete filas de día se suman aparte. */
    private const val ALTO_SEMANA_BASE = 330
    /** Cabecera + cabecera de días + resumen + pie; las filas se suman aparte. */
    private const val ALTO_MES_BASE = 470

    private const val FONDO = 0xFFFBF8F2.toInt()
    private const val TINTA = 0xFF2A2018.toInt()
    private const val TINTA_SUAVE = 0xFF5A4A38.toInt()
    private const val TENUE = 0xFFA19281.toInt()
    private const val GRIS_ETIQUETA = 0xFF8A7A66.toInt()
    private const val LINEA_SUAVE = 0xFFF1E9DC.toInt()
    private const val LINEA = 0xFFE4D6C1.toInt()

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

    private val es = Locale("es")
    private val generado = DateTimeFormatter.ofPattern("dd/MM/yyyy", es)

    suspend fun semana(
        context: Context,
        lunes: LocalDate,
        resumenes: Map<LocalDate, ResumenDia>,
        total: ResumenPeriodo,
        filtro: RangoFechas?,
    ): File = withContext(Dispatchers.IO) {
        // Alto ajustado a los siete días: un lienzo fijo dejaba media imagen en
        // blanco, y en un chat eso se lee como un error.
        val alto = ALTO_SEMANA_BASE + 7 * 70
        val bitmap = Bitmap.createBitmap(ANCHO, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(FONDO)
        val p = Pinceles(context)

        var y = cabecera(
            canvas, p,
            titulo = tituloSemana(lunes),
            aclaracion = filtro?.let { "del ${it.desde.dayOfMonth} al ${it.hasta.dayOfMonth}\n(filtro activo)" },
        )

        y = filaCifras(canvas, p, total, y)

        val dias = Periodos.semanaDe(lunes)
        for (dia in dias) {
            val resumen = resumenes[dia] ?: ResumenDia(dia)
            val fuera = filtro?.let { dia !in it } ?: false
            y += 52f
            p.diaEtiqueta.alpha = if (fuera) 100 else 255
            canvas.drawText(
                "${inicial(dia)} ${dia.dayOfMonth}",
                MARGEN, y, p.diaEtiqueta
            )
            if (resumen.vacio) {
                canvas.drawText("Sin anotaciones", MARGEN + 130f, y, p.cuerpoTenue)
            } else {
                dibujarPiezas(canvas, p, resumen, MARGEN + 130f, y, fuera)
            }
            y += 18f
            canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, p.lineaSuave)
        }

        pie(canvas, p, alto - 46f, "generado el ${generado.format(LocalDate.now())}")

        guardar(context, bitmap, "semana")
    }

    suspend fun mes(
        context: Context,
        mes: YearMonth,
        resumenes: Map<LocalDate, ResumenDia>,
        total: ResumenPeriodo,
        filtro: RangoFechas?,
    ): File = withContext(Dispatchers.IO) {
        // El alto se ajusta a las filas que tenga el mes (4, 5 o 6): un alto fijo
        // deja un hueco muerto bajo el resumen en los meses cortos.
        val filasDelMes = (Periodos.casillasDe(mes).size + 6) / 7
        val alto = ALTO_MES_BASE + filasDelMes * 94
        val bitmap = Bitmap.createBitmap(ANCHO, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(FONDO)
        val p = Pinceles(context)

        var y = cabecera(
            canvas, p,
            titulo = mesEnPalabras(mes),
            aclaracion = filtro?.let {
                "del ${it.desde.dayOfMonth} al ${it.hasta.dayOfMonth}\n(filtro activo)"
            },
        )

        // Cabecera de días de la semana
        val ancho = (ANCHO - MARGEN * 2) / 7f
        y += 34f
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, inicial ->
            canvas.drawText(inicial, MARGEN + ancho * i + ancho / 2, y, p.inicialDia)
        }

        val casillas = Periodos.casillasDe(mes)
        val altoCasilla = 86f
        var fila = 0
        y += 16f
        casillas.chunked(7).forEach { semana ->
            semana.forEachIndexed { columna, dia ->
                if (dia == null) return@forEachIndexed
                val izquierda = MARGEN + ancho * columna
                val arriba = y + fila * (altoCasilla + 8f)
                val fuera = filtro?.let { dia !in it } ?: false
                dibujarCasilla(canvas, p, dia, resumenes[dia], izquierda, arriba, ancho - 8f, altoCasilla, fuera)
            }
            fila++
        }

        val finRejilla = y + fila * (altoCasilla + 8f) + 10f
        canvas.drawLine(MARGEN, finRejilla, ANCHO - MARGEN, finRejilla, p.linea)
        filaCifras(canvas, p, total, finRejilla + 4f)

        pie(
            canvas, p, alto - 46f,
            filtro?.let { "del ${it.desde.dayOfMonth} al ${Fecha.corta(it.hasta)}" }
                ?: mesEnPalabras(mes).lowercase(es)
        )

        guardar(context, bitmap, "mes")
    }

    // -----------------------------------------------------------------------

    private const val MARGEN = 64f

    private fun cabecera(canvas: Canvas, p: Pinceles, titulo: String, aclaracion: String?): Float {
        var y = 96f
        canvas.drawText("CRÓNICAS DEL APETITO", MARGEN, y, p.sobretitulo)
        y += 44f
        canvas.drawText(titulo, MARGEN, y, p.titulo)
        if (aclaracion != null) {
            var ay = 96f
            aclaracion.split("\n").forEach { linea ->
                canvas.drawText(linea, ANCHO - MARGEN, ay, p.aclaracion)
                ay += 26f
            }
        }
        y += 22f
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, p.lineaFuerte)
        return y
    }

    private fun filaCifras(canvas: Canvas, p: Pinceles, total: ResumenPeriodo, desdeY: Float): Float {
        val y = desdeY + 54f
        val cifras = listOf(
            Triple(EntryKind.FOOD, total.comidas.toString(), "comidas"),
            Triple(EntryKind.WALK, total.minutosCaminados.toString(), "min"),
            Triple(EntryKind.MOOD, total.notasAnimo.toString(), "notas"),
            Triple(EntryKind.GYM, total.diasGimnasio.toString(), "gym"),
        )
        var x = MARGEN
        cifras.forEach { (kind, valor, etiqueta) ->
            p.cifra.color = colores[kind] ?: TINTA
            canvas.drawText(valor, x, y, p.cifra)
            val ancho = p.cifra.measureText(valor)
            canvas.drawText(" $etiqueta", x + ancho, y, p.cuerpo)
            x += ancho + p.cuerpo.measureText(" $etiqueta") + 46f
        }
        val linea = y + 26f
        canvas.drawLine(MARGEN, linea, ANCHO - MARGEN, linea, p.linea)
        return linea
    }

    private fun dibujarPiezas(
        canvas: Canvas,
        p: Pinceles,
        resumen: ResumenDia,
        desdeX: Float,
        y: Float,
        atenuado: Boolean,
    ) {
        var x = desdeX
        val piezas = buildList {
            if (resumen.comidas > 0) add(
                EntryKind.FOOD to "${resumen.comidas} ${if (resumen.comidas == 1) "comida" else "comidas"}"
            )
            if (resumen.minutosCaminados > 0) add(EntryKind.WALK to "${resumen.minutosCaminados} min")
            if (resumen.notasAnimo > 0) add(
                EntryKind.MOOD to "${resumen.notasAnimo} ${if (resumen.notasAnimo == 1) "nota" else "notas"}"
            )
            if (resumen.gimnasio == true) add(EntryKind.GYM to "gimnasio")
        }
        piezas.forEach { (kind, texto) ->
            val glifo = glifos[kind] ?: "·"
            p.glifo.color = colores[kind] ?: TINTA
            p.glifo.alpha = if (atenuado) 100 else 255
            canvas.drawText(glifo, x, y, p.glifo)
            x += p.glifo.measureText(glifo) + 10f
            p.cuerpo.alpha = if (atenuado) 100 else 255
            canvas.drawText(texto, x, y, p.cuerpo)
            x += p.cuerpo.measureText(texto) + 34f
            p.cuerpo.alpha = 255
        }
    }

    private fun dibujarCasilla(
        canvas: Canvas,
        p: Pinceles,
        dia: LocalDate,
        resumen: ResumenDia?,
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float,
        fuera: Boolean,
    ) {
        p.borde.alpha = if (fuera) 90 else 255
        canvas.drawRoundRect(x, y, x + ancho, y + alto, 14f, 14f, p.borde)
        p.numeroDia.alpha = if (fuera) 100 else 255
        canvas.drawText(dia.dayOfMonth.toString(), x + 12f, y + 30f, p.numeroDia)

        val tipos = resumen?.tiposPresentes.orEmpty()
        var gx = x + 12f
        tipos.forEach { kind ->
            val glifo = glifos[kind] ?: "·"
            p.glifoCasilla.color = colores[kind] ?: TINTA
            p.glifoCasilla.alpha = if (fuera) 100 else 255
            canvas.drawText(glifo, gx, y + 64f, p.glifoCasilla)
            gx += p.glifoCasilla.measureText(glifo) + 6f
        }
    }

    private fun pie(canvas: Canvas, p: Pinceles, y: Float, derecha: String) {
        canvas.drawLine(MARGEN, y - 28f, ANCHO - MARGEN, y - 28f, p.linea)
        canvas.drawText("● comida   ▲ caminata   ◆ ánimo   ■ gimnasio", MARGEN, y, p.pie)
        canvas.drawText(derecha, ANCHO - MARGEN, y, p.pieDerecha)
    }

    private fun guardar(context: Context, bitmap: Bitmap, prefijo: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sello = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())
        val file = File(dir, "cronicas_${prefijo}_$sello.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun inicial(d: LocalDate) = when (d.dayOfWeek.value) {
        1 -> "L"; 2 -> "M"; 3 -> "X"; 4 -> "J"; 5 -> "V"; 6 -> "S"; else -> "D"
    }

    private fun tituloSemana(lunes: LocalDate): String {
        val domingo = lunes.plusDays(6)
        val mesFin = DateTimeFormatter.ofPattern("MMMM", es).format(domingo)
        return if (lunes.month == domingo.month) {
            "Semana del ${lunes.dayOfMonth} al ${domingo.dayOfMonth} de $mesFin de ${domingo.year}"
        } else {
            val mesInicio = DateTimeFormatter.ofPattern("MMMM", es).format(lunes)
            "Semana del ${lunes.dayOfMonth} de $mesInicio al ${domingo.dayOfMonth} de $mesFin de ${domingo.year}"
        }
    }

    private fun mesEnPalabras(mes: YearMonth): String =
        DateTimeFormatter.ofPattern("MMMM yyyy", es).format(mes.atDay(1))
            .replaceFirstChar { it.uppercase() }

    private object Fecha {
        private val corto = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))
        fun corta(d: LocalDate): String = corto.format(d)
    }

    /** Todos los pinceles del documento, creados una vez por exportación. */
    private class Pinceles(context: Context) {
        private val lora: Typeface? = ResourcesCompat.getFont(context, R.font.lora)
        private val serif = lora ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)

        val sobretitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRIS_ETIQUETA; textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA; textSize = 52f; typeface = serif
        }
        val aclaracion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRIS_ETIQUETA; textSize = 24f; textAlign = Paint.Align.RIGHT
        }
        val cifra = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 42f; typeface = serif }
        val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TINTA_SUAVE; textSize = 28f }
        val cuerpoTenue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TENUE; textSize = 28f }
        val diaEtiqueta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TINTA; textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val glifo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 26f }
        val glifoCasilla = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f }
        val inicialDia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TENUE; textSize = 24f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val numeroDia = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TINTA_SUAVE; textSize = 26f }
        val pie = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TENUE; textSize = 22f }
        val pieDerecha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TENUE; textSize = 22f; textAlign = Paint.Align.RIGHT
        }
        val linea = Paint().apply { color = LINEA; strokeWidth = 1.5f }
        val lineaSuave = Paint().apply { color = LINEA_SUAVE; strokeWidth = 1.5f }
        val lineaFuerte = Paint().apply { color = TINTA; strokeWidth = 4f }
        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LINEA; style = Paint.Style.STROKE; strokeWidth = 2f
        }
    }
}
