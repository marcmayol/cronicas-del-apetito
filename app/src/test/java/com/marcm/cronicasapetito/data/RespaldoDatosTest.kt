package com.marcm.cronicasapetito.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * El respaldo es lo único de la app cuyo fallo se mide en meses de registros
 * perdidos, y encima solo se nota el día que hace falta. De ahí que se
 * compruebe el viaje de ida y vuelta entero, y no solo que el ZIP se escriba.
 */
class RespaldoDatosTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private fun foto(nombre: String, contenido: String): File =
        carpeta.newFile(nombre).apply { writeText(contenido) }

    private fun exportarYLeer(entradas: List<MealEntry>): RespaldoDatos.Contenido {
        val salida = ByteArrayOutputStream()
        runBlocking { RespaldoDatos.exportar(entradas, salida) }
        val leido = runBlocking { RespaldoDatos.leer(ByteArrayInputStream(salida.toByteArray())) }
        assertNotNull("el respaldo recién escrito debería poder leerse", leido)
        return leido!!
    }

    @Test
    fun `los cuatro tipos de registro vuelven tal cual salieron`() {
        val entradas = listOf(
            MealEntry(timestampMillis = 1_000, content = "Pizza, 3 trozos", kind = EntryKind.FOOD),
            MealEntry(
                timestampMillis = 2_000, content = "45 min",
                kind = EntryKind.WALK, minutes = 45,
            ),
            MealEntry(timestampMillis = 3_000, content = GymAnswer.YES, kind = EntryKind.GYM),
            MealEntry(timestampMillis = 4_000, content = "Tranquilo", kind = EntryKind.MOOD),
        )

        val contenido = exportarYLeer(entradas)

        assertEquals(4, contenido.entradas.size)
        assertEquals(
            entradas.map { it.timestampMillis },
            contenido.entradas.map { it.timestampMillis },
        )
        assertEquals(
            entradas.map { it.content },
            contenido.entradas.map { it.content },
        )
        assertEquals(
            entradas.map { it.kind },
            contenido.entradas.map { it.kind },
        )
    }

    @Test
    fun `los minutos de una caminata sobreviven al viaje`() {
        val contenido = exportarYLeer(
            listOf(
                MealEntry(
                    timestampMillis = 1, content = "45 min",
                    kind = EntryKind.WALK, minutes = 45,
                )
            )
        )
        assertEquals(45, contenido.entradas.single().minutes)
    }

    @Test
    fun `un registro sin minutos no se inventa ninguno`() {
        val contenido = exportarYLeer(
            listOf(MealEntry(timestampMillis = 1, content = "Sopa", kind = EntryKind.FOOD))
        )
        assertNull(contenido.entradas.single().minutes)
    }

    @Test
    fun `las fotos viajan dentro del zip con su contenido intacto`() {
        val archivo = foto("comida_1.jpg", "esto son los bytes de una foto")
        val contenido = exportarYLeer(
            listOf(
                MealEntry(
                    timestampMillis = 1, content = "Lentejas",
                    kind = EntryKind.FOOD, photoPath = archivo.absolutePath,
                )
            )
        )
        assertEquals("comida_1.jpg", contenido.entradas.single().foto)
        assertEquals(
            "esto son los bytes de una foto",
            contenido.fotos.getValue("comida_1.jpg").toString(Charsets.UTF_8),
        )
    }

    /**
     * La ruta absoluta cambia al reinstalar o al cambiar de móvil, así que el
     * respaldo guarda solo el nombre. Si guardara la ruta, restaurar en otro
     * móvil dejaría todas las fotos rotas.
     */
    @Test
    fun `el respaldo no guarda rutas absolutas`() {
        val archivo = foto("comida_2.jpg", "x")
        val salida = ByteArrayOutputStream()
        runBlocking {
            RespaldoDatos.exportar(
                listOf(
                    MealEntry(
                        timestampMillis = 1, content = "Algo",
                        kind = EntryKind.FOOD, photoPath = archivo.absolutePath,
                    )
                ),
                salida,
            )
        }
        val zip = salida.toByteArray().toString(Charsets.ISO_8859_1)
        assertTrue(
            "el nombre de la foto debería estar en el zip",
            zip.contains("comida_2.jpg"),
        )
        assertTrue(
            "la carpeta de origen no debería aparecer por ninguna parte",
            !zip.contains(archivo.parentFile.absolutePath),
        )
    }

    /** Una foto borrada a mano no puede llevarse por delante el resto. */
    @Test
    fun `si falta el archivo de una foto el registro se guarda igual`() {
        val contenido = exportarYLeer(
            listOf(
                MealEntry(
                    timestampMillis = 1, content = "Cena",
                    kind = EntryKind.FOOD,
                    photoPath = File(carpeta.root, "que-no-existe.jpg").absolutePath,
                )
            )
        )
        assertEquals(1, contenido.entradas.size)
        assertEquals("Cena", contenido.entradas.single().content)
        assertTrue(contenido.fotos.isEmpty())
    }

    @Test
    fun `los acentos y las eñes no se estropean`() {
        val contenido = exportarYLeer(
            listOf(
                MealEntry(
                    timestampMillis = 1,
                    content = "Piña, jamón y un té. Ansiedad ↑",
                    kind = EntryKind.MOOD,
                )
            )
        )
        assertEquals("Piña, jamón y un té. Ansiedad ↑", contenido.entradas.single().content)
    }

    @Test
    fun `exportar un historial vacio produce un respaldo legible`() {
        val contenido = exportarYLeer(emptyList())
        assertTrue(contenido.entradas.isEmpty())
    }

    @Test
    fun `un archivo que no es un respaldo se rechaza en vez de romper`() {
        val basura = ByteArrayInputStream("esto no es un zip".toByteArray())
        assertNull(runBlocking { RespaldoDatos.leer(basura) })
    }

    @Test
    fun `el recuento que se le enseña al usuario es el real`() {
        val archivo = foto("una.jpg", "bytes")
        val salida = ByteArrayOutputStream()
        val resultado = runBlocking {
            RespaldoDatos.exportar(
                listOf(
                    MealEntry(
                        timestampMillis = 1, content = "Con foto",
                        kind = EntryKind.FOOD, photoPath = archivo.absolutePath,
                    ),
                    MealEntry(timestampMillis = 2, content = "Sin foto", kind = EntryKind.FOOD),
                ),
                salida,
            )
        }
        assertEquals(2, resultado.registros)
        assertEquals(1, resultado.fotos)
    }
}
