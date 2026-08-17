package com.marcm.cronicasapetito.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind

// ---------------------------------------------------------------------------
// Paleta — sistema del rediseño. Marrón/crema afinado: fondo crema, tarjetas
// blancas, y cuatro tonos tierra para los tipos de registro que comparten
// saturación y luminosidad (contraste >= 4.5:1 sobre blanco).
// ---------------------------------------------------------------------------

private val Marron = Color(0xFF6B4326)          // primary
private val MarronOscuro = Color(0xFF4A3423)    // texto sobre contenedores tonales
private val Tostado = Color(0xFFF1E3D1)         // secondaryContainer, selección
private val Crema = Color(0xFFFAF6EF)           // background
private val Tinta = Color(0xFF2A2018)           // onSurface
private val TintaSuave = Color(0xFF6F6052)      // onSurfaceVariant
private val Pergamino = Color(0xFFF1E9DC)       // surfaceVariant
private val Borde = Color(0xFFC9B394)           // outline, bordes interactivos
private val BordeTarjeta = Color(0xFFEDE2D2)    // outlineVariant
private val RojoTierra = Color(0xFFA4442E)      // error, nunca alarma

private val LightColors = lightColorScheme(
    primary = Marron,
    onPrimary = Color.White,
    primaryContainer = Tostado,
    onPrimaryContainer = MarronOscuro,
    secondary = Color(0xFF9A5B2F),
    onSecondary = Color.White,
    secondaryContainer = Tostado,
    onSecondaryContainer = MarronOscuro,
    background = Crema,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Pergamino,
    onSurfaceVariant = TintaSuave,
    outline = Borde,
    outlineVariant = BordeTarjeta,
    error = RojoTierra,
    onError = Color.White,
)

/**
 * Colores del sistema que no tienen un rol Material 3 propio. Se leen con
 * [colorsCronicas] desde cualquier composable dentro de [CronicasTheme].
 */
data class ColoresCronicas(
    /** Horas, placeholders y metadatos: presente pero sin peso. */
    val tenue: Color = Color(0xFFA19281),
    /** Fondo de la pantalla principal, más cálido que las tarjetas. */
    val fondo: Color = Crema,
)

private val LocalColoresCronicas = staticCompositionLocalOf { ColoresCronicas() }

val colorsCronicas: ColoresCronicas
    @Composable get() = LocalColoresCronicas.current

// ---------------------------------------------------------------------------
// Los cuatro tipos de registro
//
// Regla del sistema: el color de un tipo NUNCA aparece sin su glifo o su icono.
// Así el registro sigue siendo legible en una fotocopia en blanco y negro y para
// quien no distingue bien los colores — que es el requisito duro del brief,
// porque esto acaba impreso en la consulta.
// ---------------------------------------------------------------------------

data class VisualTipo(
    val etiqueta: String,
    val color: Color,
    val contenedor: Color,
    /** Glifo con forma propia: se distingue sin color. */
    val glifo: String,
)

private val VisualComida = VisualTipo("Comida", Color(0xFF9A5B2F), Color(0xFFF5E7D8), "●")
private val VisualCaminata = VisualTipo("Caminata", Color(0xFF5C7549), Color(0xFFE8EFDF), "▲")
private val VisualAnimo = VisualTipo("Estado de ánimo", Color(0xFF7B5C90), Color(0xFFEFE7F4), "◆")
private val VisualGimnasio = VisualTipo("Gimnasio", Color(0xFF47698C), Color(0xFFE3EBF2), "■")

fun visualDe(kind: String): VisualTipo = when (kind) {
    EntryKind.FOOD -> VisualComida
    EntryKind.WALK -> VisualCaminata
    EntryKind.MOOD -> VisualAnimo
    EntryKind.GYM -> VisualGimnasio
    else -> VisualTipo(kind, TintaSuave, Pergamino, "·")
}

// ---------------------------------------------------------------------------
// Tipografía — Lora (SIL OFL) solo en títulos, cabeceras de día y cifras;
// Roboto en todo el cuerpo, que es lo que crece con el zoom del sistema.
// El TTF es una fuente variable: los pesos salen del eje wght.
// ---------------------------------------------------------------------------

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun loraPeso(peso: FontWeight) = Font(
    R.font.lora,
    weight = peso,
    variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight)),
)

private val Lora = FontFamily(
    loraPeso(FontWeight.Medium),
    loraPeso(FontWeight.SemiBold),
)

private val CronicasTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 19.sp
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = Lora, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
        ),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 15.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.08.em
        ),
    )
}

/** Cifras de resumen: Lora, para que los números tengan el aire del cuaderno. */
val estiloCifra: TextStyle
    @Composable get() = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp)

@Composable
fun CronicasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CronicasTypography,
        content = content,
    )
}
