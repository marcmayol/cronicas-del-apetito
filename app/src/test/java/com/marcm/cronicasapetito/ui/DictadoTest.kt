package com.marcm.cronicasapetito.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DictadoTest {

    @Test
    fun `en una caja vacia empieza con mayuscula`() {
        assertEquals("Dos huevos fritos", Dictado.unir("", "dos huevos fritos"))
    }

    @Test
    fun `detras de lo escrito va con un espacio y sin tocar mayusculas`() {
        assertEquals("Pasta con atún y una manzana", Dictado.unir("Pasta con atún ", "y una manzana"))
    }

    @Test
    fun `un dictado vacio no cambia nada`() {
        assertEquals("Café", Dictado.unir("Café", "   "))
    }
}
