package com.marcm.cronicasapetito.ui

import androidx.annotation.StringRes
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
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

// --- De noche -----------------------------------------------------------------
// No es la paleta clara invertida sin más: se conserva el marrón cálido para que
// siga siendo la misma app, pero el papel pasa a ser tinta. El último aviso es a
// medianoche y hay un botón de irse a dormir, así que esta es la pantalla que más
// veces se mira a oscuras.
private val MarronClaro = Color(0xFFD9A574)     // primary de noche
private val TostadoOscuro = Color(0xFF4A3A2C)   // contenedores tonales
private val NocheFondo = Color(0xFF1A1512)      // background
private val NocheSuperficie = Color(0xFF241E1A) // surface: por encima del fondo
private val NocheVariante = Color(0xFF2E2620)   // surfaceVariant
private val TintaClara = Color(0xFFF0E7DC)      // onSurface
private val TintaClaraSuave = Color(0xFFB9AA99) // onSurfaceVariant
private val BordeNoche = Color(0xFF6B5A47)      // outline
private val BordeTarjetaNoche = Color(0xFF3A302A)
private val RojoTierraClaro = Color(0xFFE08268) // error de noche

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

private val DarkColors = darkColorScheme(
    primary = MarronClaro,
    onPrimary = Color(0xFF3A2415),
    primaryContainer = TostadoOscuro,
    onPrimaryContainer = Color(0xFFF1E3D1),
    secondary = Color(0xFFD79A6A),
    onSecondary = Color(0xFF3A2415),
    secondaryContainer = TostadoOscuro,
    onSecondaryContainer = Color(0xFFF1E3D1),
    background = NocheFondo,
    onBackground = TintaClara,
    surface = NocheSuperficie,
    onSurface = TintaClara,
    surfaceVariant = NocheVariante,
    onSurfaceVariant = TintaClaraSuave,
    outline = BordeNoche,
    outlineVariant = BordeTarjetaNoche,
    error = RojoTierraClaro,
    onError = Color(0xFF3A1610),
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

private val ColoresNoche = ColoresCronicas(
    tenue = Color(0xFF9A8B7A),
    fondo = NocheFondo,
)

private val LocalColoresCronicas = staticCompositionLocalOf { ColoresCronicas() }

/**
 * Si la app se está pintando de noche. No es lo mismo que [isSystemInDarkTheme]:
 * desde Ajustes se puede forzar claro u oscuro contra lo que diga el móvil, y
 * todo lo que dependa del tema tiene que mirar aquí y no al sistema.
 */
val LocalEsNoche = staticCompositionLocalOf { false }

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
    /** Nombre del tipo, como recurso: la app se lee en más de un idioma. */
    @StringRes val etiqueta: Int,
    val color: Color,
    val contenedor: Color,
    /** Glifo con forma propia: se distingue sin color. */
    val glifo: String,
)

private val VisualComida =
    VisualTipo(R.string.kind_food, Color(0xFF9A5B2F), Color(0xFFF5E7D8), "●")
private val VisualCaminata =
    VisualTipo(R.string.kind_walk, Color(0xFF5C7549), Color(0xFFE8EFDF), "▲")
private val VisualAnimo =
    VisualTipo(R.string.kind_mood, Color(0xFF7B5C90), Color(0xFFEFE7F4), "◆")
private val VisualGimnasio =
    VisualTipo(R.string.kind_gym, Color(0xFF47698C), Color(0xFFE3EBF2), "■")

// De noche los cuatro tonos se aclaran: los de día están calculados para
// contrastar sobre blanco y sobre tinta desaparecerían. Los contenedores, al
// revés, se oscurecen hasta ser solo un tinte.
private val VisualComidaNoche =
    VisualTipo(R.string.kind_food, Color(0xFFD9995F), Color(0xFF3A2A1C), "●")
private val VisualCaminataNoche =
    VisualTipo(R.string.kind_walk, Color(0xFF9DBE85), Color(0xFF25301E), "▲")
private val VisualAnimoNoche =
    VisualTipo(R.string.kind_mood, Color(0xFFC3A3D6), Color(0xFF2E2436), "◆")
private val VisualGimnasioNoche =
    VisualTipo(R.string.kind_gym, Color(0xFF8FB4D9), Color(0xFF1F2B36), "■")

/**
 * El visual de un tipo, según sea de día o de noche. El glifo no cambia nunca:
 * es lo que hace legible el registro sin color, y de eso depende que se entienda
 * fotocopiado en la consulta.
 */
@Composable
fun visualDe(kind: String): VisualTipo = if (LocalEsNoche.current) {
    when (kind) {
        EntryKind.WALK -> VisualCaminataNoche
        EntryKind.MOOD -> VisualAnimoNoche
        EntryKind.GYM -> VisualGimnasioNoche
        else -> VisualComidaNoche
    }
} else {
    when (kind) {
        EntryKind.WALK -> VisualCaminata
        EntryKind.MOOD -> VisualAnimo
        EntryKind.GYM -> VisualGimnasio
        else -> VisualComida
    }
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
    val noche = when (TemaApp.modo) {
        TemaApp.Modo.CLARO -> false
        TemaApp.Modo.OSCURO -> true
        TemaApp.Modo.SISTEMA -> isSystemInDarkTheme()
    }

    // La barra de estado se pinta desde aquí y no desde themes.xml: el XML solo
    // sabe lo que dice el móvil, y con el tema forzado a mano quedaría una
    // franja clara sobre una app oscura, o al revés.
    val vista = LocalView.current
    if (!vista.isInEditMode) {
        val fondoBarra = (if (noche) NocheFondo else Color(0xFFFFF8EE)).toArgb()
        SideEffect {
            val ventana = (vista.context as Activity).window
            // De Android 15 en adelante el sistema ignora este color: la barra es
            // transparente y detrás se ve el fondo de la propia app, que ya es el
            // que toca. Ponerlo allí no hace nada; aquí abajo, todavía es lo único
            // que evita la franja del tema equivocado.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                ventana.statusBarColor = fondoBarra
            }
            WindowCompat.getInsetsController(ventana, vista)
                .isAppearanceLightStatusBars = !noche
        }
    }

    CompositionLocalProvider(
        LocalColoresCronicas provides if (noche) ColoresNoche else ColoresCronicas(),
        LocalEsNoche provides noche,
    ) {
        MaterialTheme(
            colorScheme = if (noche) DarkColors else LightColors,
            typography = CronicasTypography,
            content = content,
        )
    }
}
