package com.marcm.cronicasapetito.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.MealNotifier
import kotlinx.coroutines.launch

private enum class Paso { PREGUNTA, MINUTOS, ANIMO }

class WalkMoodActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_AT_MINUTES = "start_at_minutes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealNotifier.dismiss(this)

        val repo = MealRepository((application as CronicasApp).database.mealDao())
        val empezarEnMinutos = intent.getBooleanExtra(EXTRA_START_AT_MINUTES, false)

        setContent {
            CronicasTheme {
                FlujoCaminata(
                    pasoInicial = if (empezarEnMinutos) Paso.MINUTOS else Paso.PREGUNTA,
                    onSave = { minutos, animo, timestamp ->
                        lifecycleScope.launch {
                            if (minutos != null && minutos > 0) repo.addWalk(minutos, timestamp)
                            if (animo.isNotBlank()) repo.addMood(animo.trim(), timestamp)
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FlujoCaminata(
    pasoInicial: Paso = Paso.PREGUNTA,
    onSave: (minutos: Int?, animo: String, timestamp: Long) -> Unit,
    onCancel: () -> Unit
) {
    var paso by remember { mutableStateOf(pasoInicial) }
    var minutos by remember { mutableStateOf(0) }
    var animo by remember { mutableStateOf("") }
    var momento by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val visual = visualDe(EntryKind.WALK)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Caminata") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = { SelloTipo(EntryKind.WALK) },
            )
        },
        bottomBar = {
            when (paso) {
                Paso.PREGUNTA -> Unit
                Paso.MINUTOS -> BarraGuardar(
                    habilitado = minutos > 0,
                    onCancelar = {
                        if (pasoInicial == Paso.PREGUNTA) paso = Paso.PREGUNTA else onCancel()
                    },
                    onGuardar = { paso = Paso.ANIMO },
                    textoGuardar = "Continuar",
                    textoCancelar = if (pasoInicial == Paso.PREGUNTA) "Atrás" else "Cancelar",
                    alineadoAlInicio = true,
                )
                Paso.ANIMO -> BarraGuardar(
                    habilitado = true,
                    onCancelar = onCancel,
                    onGuardar = { onSave(minutos, animo, momento) },
                    textoGuardar = if (animo.isBlank()) "Omitir" else "Guardar",
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Los tres tramos: el paso en el que estás, que antes no se veía.
            IndicadorPasos(
                total = 3,
                completados = when (paso) {
                    Paso.PREGUNTA -> 1
                    Paso.MINUTOS -> 2
                    Paso.ANIMO -> 3
                },
                color = visual.color,
            )

            when (paso) {
                Paso.PREGUNTA -> {
                    Text("¿Has ido a caminar?", style = MaterialTheme.typography.headlineSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { paso = Paso.MINUTOS },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Sí", fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            onClick = { onSave(null, "", momento) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                        ) { Text("No", fontWeight = FontWeight.SemiBold) }
                    }
                }

                Paso.MINUTOS -> {
                    Text(
                        "¿Cuánto tiempo has caminado?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FilaFechaHora(
                        selectedTime = momento,
                        tinte = visual.color,
                        onPicked = { momento = it },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.4.dp, visual.color),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    text = minutos.toString(),
                                    style = estiloCifra,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "minutos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        BotonPaso("−5") { minutos = (minutos - 5).coerceAtLeast(0) }
                        Spacer(Modifier.width(8.dp))
                        BotonPaso("+5") { minutos = (minutos + 5).coerceAtMost(600) }
                    }
                    // Teclear números de pie es lo más lento que hay.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60, 90).forEach { atajo ->
                            ChipMinutos(
                                minutos = atajo,
                                activo = minutos == atajo,
                                color = visual.color,
                                contenedor = visual.contenedor,
                                onClick = { minutos = atajo },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = if (minutos == 0) "" else minutos.toString(),
                        onValueChange = { texto ->
                            minutos = texto.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Otra cantidad") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = visual.color,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }

                Paso.ANIMO -> {
                    Text("¿Cómo te has sentido?", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Si quieres, anota cómo te has sentido. Es opcional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = animo,
                        onValueChange = { animo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Describe cómo te sientes ahora mismo") },
                        minLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = visualDe(EntryKind.MOOD).color,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicadorPasos(total: Int, completados: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { indice ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (indice < completados) color else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun BotonPaso(texto: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ChipMinutos(
    minutos: Int,
    activo: Boolean,
    color: androidx.compose.ui.graphics.Color,
    contenedor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (activo) contenedor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (activo) color else MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Text(
            text = minutos.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
            color = if (activo) color else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
