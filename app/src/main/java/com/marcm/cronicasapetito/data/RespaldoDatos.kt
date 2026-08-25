package com.marcm.cronicasapetito.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copia de seguridad completa: un ZIP con `datos.json` y la carpeta `fotos/`.
 *
 * Existe porque los datos de esta app no están en ninguna nube ni cuenta —esa
 * es la idea— y sin una salida propia, cambiar de móvil o perderlo significaba
 * perder meses de registros. Es también la única manera de que quien no tenga
 * un cable y el SDK de Android pueda llevarse lo suyo.
 *
 * El formato es JSON legible a propósito: si algún día la app desaparece, el
 * historial se sigue pudiendo leer con cualquier cosa.
 */
object RespaldoDatos {

    private const val TAG = "RespaldoDatos"
    private const val ARCHIVO_DATOS = "datos.json"
    private const val CARPETA_FOTOS = "fotos/"

    /** Versión del formato, por si algún día cambia la forma del JSON. */
    private const val FORMATO = 1

    // -----------------------------------------------------------------------
    // Exportar
    // -----------------------------------------------------------------------

    /**
     * Escribe en [destino] un ZIP con todos los registros y sus fotos.
     * Devuelve cuántos registros y cuántas fotos se guardaron.
     */
    suspend fun exportar(
        entradas: List<MealEntry>,
        destino: OutputStream,
    ): Resultado = withContext(Dispatchers.IO) {
        var fotos = 0
        ZipOutputStream(destino.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ARCHIVO_DATOS))
            zip.write(aJson(entradas).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for (entrada in entradas) {
                val ruta = entrada.photoPath ?: continue
                val archivo = File(ruta)
                if (!archivo.exists()) {
                    // Una foto que ya no está no puede invalidar el respaldo
                    // entero: el registro se guarda igual, sin ella.
                    Log.w(TAG, "falta la foto ${archivo.name}")
                    continue
                }
                zip.putNextEntry(ZipEntry(CARPETA_FOTOS + archivo.name))
                archivo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                fotos++
            }
        }
        Resultado(registros = entradas.size, fotos = fotos)
    }

    /** El historial como JSON. Las fotos van por nombre, no por ruta absoluta. */
    private fun aJson(entradas: List<MealEntry>): String {
        val lista = JSONArray()
        for (e in entradas) {
            lista.put(
                JSONObject().apply {
                    put("timestampMillis", e.timestampMillis)
                    put("kind", e.kind)
                    put("content", e.content)
                    e.minutes?.let { put("minutes", it) }
                    e.photoPath?.let { put("photo", File(it).name) }
                }
            )
        }
        return JSONObject().apply {
            put("formato", FORMATO)
            put("exportado", System.currentTimeMillis())
            put("registros", lista)
        }.toString(2)
    }

    // -----------------------------------------------------------------------
    // Importar
    // -----------------------------------------------------------------------

    /**
     * Lee un ZIP de respaldo y devuelve lo que contiene, sin tocar la base.
     * Se separa de [restaurar] para poder decir cuántos registros hay antes de
     * escribir nada.
     */
    suspend fun leer(origen: InputStream): Contenido? =
        withContext(Dispatchers.IO) {
            runCatching {
                var json: String? = null
                val fotos = mutableMapOf<String, ByteArray>()
                ZipInputStream(origen.buffered()).use { zip ->
                    var entrada = zip.nextEntry
                    while (entrada != null) {
                        when {
                            entrada.name == ARCHIVO_DATOS ->
                                json = zip.readBytes().toString(Charsets.UTF_8)

                            entrada.name.startsWith(CARPETA_FOTOS) && !entrada.isDirectory ->
                                fotos[File(entrada.name).name] = zip.readBytes()
                        }
                        zip.closeEntry()
                        entrada = zip.nextEntry
                    }
                }
                val texto = json ?: return@runCatching null
                Contenido(entradas = desdeJson(texto), fotos = fotos)
            }.onFailure { Log.e(TAG, "no se pudo leer el respaldo", it) }.getOrNull()
        }

    private fun desdeJson(texto: String): List<EntradaRespaldo> {
        val raiz = JSONObject(texto)
        val lista = raiz.optJSONArray("registros") ?: JSONArray()
        return (0 until lista.length()).map { i ->
            val o = lista.getJSONObject(i)
            EntradaRespaldo(
                timestampMillis = o.getLong("timestampMillis"),
                kind = o.optString("kind", EntryKind.FOOD),
                content = o.optString("content", ""),
                minutes = if (o.has("minutes")) o.getInt("minutes") else null,
                foto = if (o.has("photo")) o.getString("photo") else null,
            )
        }
    }

    /**
     * Mete en la base los registros del respaldo que no estén ya, comparando
     * por momento, tipo y contenido. Nunca borra ni sobrescribe: restaurar un
     * respaldo antiguo sobre una app en uso añade lo que falta y deja lo demás
     * como está, que es lo que uno espera al pulsar «importar» por error.
     */
    suspend fun restaurar(
        context: Context,
        contenido: Contenido,
        repositorio: MealRepository,
    ): Resultado = withContext(Dispatchers.IO) {
        val existentes = repositorio.getAll()
            .map { Triple(it.timestampMillis, it.kind, it.content) }
            .toHashSet()

        val dirFotos = File(context.filesDir, "photos").apply { mkdirs() }
        var añadidos = 0
        var fotosEscritas = 0

        for (e in contenido.entradas) {
            if (Triple(e.timestampMillis, e.kind, e.content) in existentes) continue

            val rutaFoto = e.foto?.let { nombre ->
                val bytes = contenido.fotos[nombre] ?: return@let null
                val destino = File(dirFotos, nombre)
                if (!destino.exists()) {
                    destino.writeBytes(bytes)
                    fotosEscritas++
                }
                destino.absolutePath
            }

            repositorio.añadirDeRespaldo(
                MealEntry(
                    timestampMillis = e.timestampMillis,
                    content = e.content,
                    kind = e.kind,
                    minutes = e.minutes,
                    photoPath = rutaFoto,
                )
            )
            añadidos++
        }
        Resultado(registros = añadidos, fotos = fotosEscritas)
    }

    // -----------------------------------------------------------------------

    data class EntradaRespaldo(
        val timestampMillis: Long,
        val kind: String,
        val content: String,
        val minutes: Int?,
        val foto: String?,
    )

    data class Contenido(
        val entradas: List<EntradaRespaldo>,
        val fotos: Map<String, ByteArray>,
    )

    data class Resultado(val registros: Int, val fotos: Int)

    /** Nombre sugerido del archivo: ordena solo al listar una carpeta. */
    fun nombreSugerido(sello: String): String = "cronicas-apetito-$sello.zip"
}

/** Uri de destino elegido por quien exporta, resuelto a un flujo de escritura. */
fun Context.abrirParaEscribir(destino: Uri): OutputStream? =
    contentResolver.openOutputStream(destino)

fun Context.abrirParaLeer(origen: Uri): InputStream? =
    contentResolver.openInputStream(origen)
