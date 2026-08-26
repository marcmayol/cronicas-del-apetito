package com.marcm.cronicasapetito.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Al borrar un registro su foto se queda en el disco a propósito: mientras se
 * pueda deshacer, el archivo tiene que existir. Quien las recoge después es la
 * limpieza del siguiente arranque, y equivocarse aquí significa borrar la foto
 * de un registro que sigue vivo.
 */
class FotosHuerfanasTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    /** Un repositorio de mentira: solo hace falta saber qué rutas siguen en uso. */
    private fun repoCon(rutasEnUso: List<String>): MealRepository =
        MealRepository(FalsoDao(rutasEnUso))

    @Test
    fun `borra solo las fotos que ya no apunta ningun registro`() {
        val viva = carpeta.newFile("viva.jpg").apply { writeText("x") }
        val huerfana = carpeta.newFile("huerfana.jpg").apply { writeText("x") }

        val borradas = runBlocking {
            repoCon(listOf(viva.absolutePath)).limpiarFotosHuerfanas(carpeta.root)
        }

        assertEquals(1, borradas)
        assertTrue("la foto en uso no se toca", viva.exists())
        assertFalse("la que ya no usa nadie se va", huerfana.exists())
    }

    @Test
    fun `compara por nombre, no por ruta absoluta`() {
        // Tras reinstalar, la carpeta de la app cambia de sitio y las rutas
        // guardadas apuntan a un lugar que ya no existe. Si se comparase la ruta
        // entera, la limpieza borraría todas las fotos del usuario de una vez.
        val foto = carpeta.newFile("comida.jpg").apply { writeText("x") }
        val rutaAntigua = "/data/user/0/otro.paquete/files/photos/comida.jpg"

        val borradas = runBlocking {
            repoCon(listOf(rutaAntigua)).limpiarFotosHuerfanas(carpeta.root)
        }

        assertEquals(0, borradas)
        assertTrue(foto.exists())
    }

    @Test
    fun `una carpeta que no existe no rompe nada`() {
        val borradas = runBlocking {
            repoCon(emptyList()).limpiarFotosHuerfanas(File(carpeta.root, "no-existe"))
        }
        assertEquals(0, borradas)
    }

    private class FalsoDao(private val rutas: List<String>) : MealEntryDao {
        override suspend fun rutasDeFotoEnUso(): List<String> = rutas

        override suspend fun insert(entry: MealEntry): Long = 0
        override suspend fun update(entry: MealEntry) = Unit
        override suspend fun delete(entry: MealEntry) = Unit
        override suspend fun getById(id: Long): MealEntry? = null
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun getInRange(from: Long, to: Long) = emptyList<MealEntry>()
        override fun observeInRange(from: Long, to: Long) = throw UnsupportedOperationException()
        override suspend fun getAll() = emptyList<MealEntry>()
        override suspend fun countByKindInRange(kind: String, from: Long, to: Long) = 0
        override suspend fun countByKindAndContentInRange(
            kind: String,
            contents: List<String>,
            from: Long,
            to: Long,
        ) = 0
    }
}
