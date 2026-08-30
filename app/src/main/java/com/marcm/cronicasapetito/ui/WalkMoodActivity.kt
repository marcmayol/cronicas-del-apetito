package com.marcm.cronicasapetito.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.MealRepository
import androidx.compose.ui.platform.LocalContext
import com.marcm.cronicasapetito.notifications.MealNotifier
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import kotlinx.coroutines.launch

private enum class Paso { PREGUNTA, MINUTOS, ANIMO }

class WalkMoodActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_AT_MINUTES = "start_at_minutes"
        /** Anotar solo cómo te sientes, sin pasar por la caminata. */
        const val EXTRA_SOLO_ANIMO = "solo_animo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 en adelante dibuja bajo las barras del sistema quiera o no.
        // Declararlo aquí hace que se comporte igual en todas las versiones, y
        // que los insets que aplica la UI sean los mismos en el móvil de 2019
        // que en el de este año.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = MealRepository((application as CronicasApp).database.mealDao())
        val empezarEnMinutos = intent.getBooleanExtra(EXTRA_START_AT_MINUTES, false)
        val soloAnimo = intent.getBooleanExtra(EXTRA_SOLO_ANIMO, false)
        val idAEditar = intent.getLongExtra(EXTRA_EDITAR_ID, 0L)

        // El aviso de comida se descarta porque esta pantalla es su respuesta.
        // Anotando solo el ánimo no lo es: la pregunta de la comida sigue viva.
        if (!soloAnimo) MealNotifier.dismiss(this)

        setContent {
            CronicasTheme {
                // Corrigiendo se espera al registro: pintar la pantalla vacía y
                // rellenarla después deja un hueco en el que se puede guardar.
                var original by remember { mutableStateOf<MealEntry?>(null) }
                var cargando by remember { mutableStateOf(idAEditar != 0L) }

                LaunchedEffect(idAEditar) {
                    if (idAEditar != 0L) {
                        original = repo.obtener(idAEditar)
                        cargando = false
                    }
                }

                if (!cargando) {
                    FlujoCaminata(
                        pasoInicial = when {
                            soloAnimo -> Paso.ANIMO
                            empezarEnMinutos -> Paso.MINUTOS
                            else -> Paso.PREGUNTA
                        },
                        soloAnimo = soloAnimo,
                        original = original,
                        onSave = { minutos, animo, timestamp ->
                            lifecycleScope.launch {
                                val previo = original
                                when {
                                    // Corrigiendo se cambia el registro, no se
                                    // añade otro encima.
                                    previo != null && previo.kind == EntryKind.MOOD ->
                                        repo.actualizar(
                                            previo.copy(
                                                content = animo.trim(),
                                                timestampMillis = timestamp,
                                            )
                                        )

                                    previo != null -> repo.actualizar(
                                        previo.copy(
                                            content = "$minutos min",
                                            minutes = minutos,
                                            timestampMillis = timestamp,
                                        )
                                    )

                                    else -> {
                                        if (minutos != null && minutos > 0) {
                                            repo.addWalk(minutos, timestamp)
                                        }
                                        if (animo.isNotBlank()) {
                                            repo.addMood(animo.trim(), timestamp)
                                        }
                                    }
                                }
                                finish()
                            }
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FlujoCaminata(
    pasoInicial: Paso = Paso.PREGUNTA,
    soloAnimo: Boolean = false,
    original: MealEntry? = null,
    onSave: (minutos: Int?, animo: String, timestamp: Long) -> Unit,
    onCancel: () -> Unit
) {
    var paso by remember { mutableStateOf(pasoInicial) }
    var minutos by remember { mutableStateOf(original?.minutes ?: 0) }
    var animo by remember {
        mutableStateOf(if (original?.kind == EntryKind.MOOD) original.content else "")
    }
    var momento by remember {
        mutableLongStateOf(original?.timestampMillis ?: System.currentTimeMillis())
    }
    val conAnimo = PrefsRecordatorios.animoActivo(LocalContext.current)
    val tipo = if (soloAnimo) EntryKind.MOOD else EntryKind.WALK
    val visual = visualDe(tipo)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (soloAnimo) R.string.kind_mood else R.string.walk_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = { SelloTipo(tipo) },
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
                    onGuardar = {
                        // Sin el carril de ánimo —o corrigiendo una caminata ya
                        // anotada— esto acaba aquí: encadenar el ánimo al corregir
                        // crearía una nota nueva que nadie ha pedido.
                        if (conAnimo && original == null) paso = Paso.ANIMO
                        else onSave(minutos, "", momento)
                    },
                    textoGuardar = stringResource(
                        if (conAnimo && original == null) R.string.action_continue
                        else R.string.action_save
                    ),
                    textoCancelar = stringResource(
                        if (pasoInicial == Paso.PREGUNTA) R.string.action_previous
                        else R.string.action_cancel
                    ),
                    alineadoAlInicio = true,
                )
                Paso.ANIMO -> BarraGuardar(
                    // Anotado suelto, un ánimo en blanco no guardaría nada;
                    // como cola de la caminata sí puede omitirse.
                    habilitado = !soloAnimo || animo.isNotBlank(),
                    onCancelar = onCancel,
                    onGuardar = { onSave(if (soloAnimo) null else minutos, animo, momento) },
                    textoGuardar = stringResource(
                        if (!soloAnimo && animo.isBlank()) R.string.action_skip
                        else R.string.action_save
                    ),
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
            // Anotando solo el ánimo no hay recorrido que enseñar.
            if (!soloAnimo) {
                IndicadorPasos(
                    total = 3,
                    completados = when (paso) {
                        Paso.PREGUNTA -> 1
                        Paso.MINUTOS -> 2
                        Paso.ANIMO -> 3
                    },
                    color = visual.color,
                )
            }

            when (paso) {
                Paso.PREGUNTA -> {
                    Text(stringResource(R.string.walk_did_you_walk), style = MaterialTheme.typography.headlineSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { paso = Paso.MINUTOS },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(stringResource(R.string.action_yes), fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            onClick = { onSave(null, "", momento) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                        ) { Text(stringResource(R.string.action_no), fontWeight = FontWeight.SemiBold) }
                    }
                }

                Paso.MINUTOS -> {
                    Text(
                        stringResource(R.string.walk_how_long),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FilaFechaHora(
                        selectedTime = momento,
                        tinte = visual.color,
                        onPicked = { momento = it },
                    )
                    // Una sola caja con el número, y editable. Antes había dos que
                    // enseñaban lo mismo: la de arriba con borde de color parecía un
                    // campo pero no dejaba escribir, y debajo había otra que sí.
                    OutlinedTextField(
                        value = if (minutos == 0) "" else minutos.toString(),
                        onValueChange = { texto ->
                            minutos = texto.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = estiloCifra,
                        placeholder = {
                            Text("0", style = estiloCifra, color = colorsCronicas.tenue)
                        },
                        suffix = {
                            Text(
                                text = stringResource(R.string.walk_minutes_unit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = visual.color,
                            unfocusedBorderColor = visual.color,
                            cursorColor = visual.color,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    )
                    // Los atajos se quedan: teclear números de pie es lo más lento que
                    // hay. Los ±5 no aportaban nada que no hicieran mejor el teclado o
                    // estos chips, y sumaban una fila de ruido.
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
                }

                Paso.ANIMO -> {
                    Text(
                        text = stringResource(
                            if (soloAnimo) R.string.mood_how_do_you_feel
                            else R.string.entry_how_did_you_feel
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(
                            if (soloAnimo) R.string.mood_standalone_hint
                            else R.string.mood_optional_hint
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (soloAnimo) {
                        FilaFechaHora(
                            selectedTime = momento,
                            tinte = visual.color,
                            onPicked = { momento = it },
                        )
                    }
                    OutlinedTextField(
                        value = animo,
                        onValueChange = { animo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.mood_placeholder)) },
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
