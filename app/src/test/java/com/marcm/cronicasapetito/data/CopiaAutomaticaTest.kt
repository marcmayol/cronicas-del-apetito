package com.marcm.cronicasapetito.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CopiaAutomaticaTest {

    private fun auto(sello: String) = "${CopiaAutomatica.PREFIJO}$sello.zip"

    @Test
    fun `se quedan las cuatro mas nuevas y sobran las demas`() {
        val nombres = listOf(
            auto("20261001-0300"),
            auto("20261029-0300"),
            auto("20261008-0300"),
            auto("20261022-0300"),
            auto("20261015-0300"),
            auto("20260924-0300"),
        )
        assertEquals(
            listOf(auto("20261001-0300"), auto("20260924-0300")),
            CopiaAutomatica.sobrantes(nombres),
        )
    }

    @Test
    fun `las copias hechas a mano y otros archivos no se tocan nunca`() {
        val nombres = listOf(
            "cronicas-apetito-20250101.zip", // a mano, la más vieja de todas
            "notas.txt",
            "${CopiaAutomatica.PREFIJO}20250101-0300.txt", // no es un zip
            auto("20261001-0300"),
            auto("20261008-0300"),
            auto("20261015-0300"),
            auto("20261022-0300"),
            auto("20261029-0300"),
        )
        assertEquals(listOf(auto("20261001-0300")), CopiaAutomatica.sobrantes(nombres))
    }

    @Test
    fun `con pocas copias no sobra ninguna`() {
        assertEquals(
            emptyList<String>(),
            CopiaAutomatica.sobrantes(listOf(auto("20261029-0300"), auto("20261022-0300"))),
        )
    }

    @Test
    fun `el mismo dia a horas distintas ordena por la hora`() {
        val nombres = listOf(auto("20261029-0900"), auto("20261029-2300"), auto("20261029-1500"))
        assertEquals(
            listOf(auto("20261029-0900")),
            CopiaAutomatica.sobrantes(nombres, conservar = 2),
        )
    }
}
