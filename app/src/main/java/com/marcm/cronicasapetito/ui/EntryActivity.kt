package com.marcm.cronicasapetito.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.data.PhotoStore
import com.marcm.cronicasapetito.notifications.MealNotifier
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealNotifier.dismiss(this)

        val repo = MealRepository((application as CronicasApp).database.mealDao())

        setContent {
            CronicasTheme {
                EntryScreen(
                    onSave = { foodText, moodText, timestamp, photoPath ->
                        lifecycleScope.launch {
                            repo.addFood(foodText, timestamp, photoPath)
                            if (moodText.isNotBlank()) {
                                repo.addMood(moodText.trim(), timestamp)
                            }
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(
    onSave: (foodText: String, moodText: String, timestamp: Long, photoPath: String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var foodText by rememberSaveable { mutableStateOf("") }
    var moodText by rememberSaveable { mutableStateOf("") }
    var selectedTime by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    // rememberSaveable: sobreviven si Android recrea la Activity al volver de la cámara.
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraTempPath by rememberSaveable { mutableStateOf<String?>(null) }
    var processingPhoto by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val visualComida = visualDe(EntryKind.FOOD)

    // Selector de fotos del sistema (no requiere permisos).
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processingPhoto = true
            scope.launch {
                val previous = photoPath
                val saved = PhotoStore.importFromUri(context, uri)
                processingPhoto = false
                if (saved != null) {
                    photoPath = saved
                    PhotoStore.delete(previous)
                } else {
                    Toast.makeText(context, "No se pudo guardar la foto", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Cámara: escribe en un archivo temporal nuestro y luego lo importamos.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val temp = cameraTempPath?.let { File(it) }
        if (success && temp != null) {
            processingPhoto = true
            scope.launch {
                val previous = photoPath
                val saved = PhotoStore.importFromFile(context, temp)
                processingPhoto = false
                if (saved != null) {
                    photoPath = saved
                    PhotoStore.delete(previous)
                } else {
                    Toast.makeText(context, "No se pudo guardar la foto", Toast.LENGTH_LONG).show()
                }
            }
        } else if (!success) {
            // La cámara se canceló: limpiamos el temporal.
            temp?.delete()
        }
    }

    val cancelAndClean = {
        PhotoStore.delete(photoPath)
        onCancel()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.entry_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = cancelAndClean) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = { SelloTipo(EntryKind.FOOD) },
            )
        },
        bottomBar = {
            BarraGuardar(
                habilitado = foodText.isNotBlank() && !processingPhoto,
                onCancelar = cancelAndClean,
                onGuardar = { onSave(foodText.trim(), moodText, selectedTime, photoPath) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilaFechaHora(
                selectedTime = selectedTime,
                tinte = visualComida.color,
                onPicked = { selectedTime = it },
            )

            Column {
                EtiquetaCampo("¿Qué has comido?")
                OutlinedTextField(
                    value = foodText,
                    onValueChange = { foodText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.input_hint)) },
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = visualComida.color,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                )
            }

            Column {
                EtiquetaCampo("Foto del plato", opcional = true)
                when {
                    processingPhoto -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Guardando foto…", style = MaterialTheme.typography.bodyMedium)
                    }

                    photoPath == null -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BotonFoto(
                            icono = Icons.Filled.PhotoCamera,
                            texto = "Cámara",
                            modifier = Modifier.weight(1f),
                        ) {
                            val file = PhotoStore.newCameraTempFile(context)
                            cameraTempPath = file.absolutePath
                            cameraLauncher.launch(PhotoStore.uriFor(context, file))
                        }
                        BotonFoto(
                            icono = Icons.Filled.Image,
                            texto = "Galería",
                            modifier = Modifier.weight(1f),
                        ) {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }

                    else -> Column {
                        PhotoThumb(
                            path = photoPath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = {
                                PhotoStore.delete(photoPath)
                                photoPath = null
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Text(" Quitar foto")
                            }
                        }
                    }
                }
            }

            Column {
                EtiquetaCampo("¿Cómo te has sentido?", opcional = true)
                OutlinedTextField(
                    value = moodText,
                    onValueChange = { moodText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Puedes dejarlo en blanco si no quieres anotarlo") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = visualDe(EntryKind.MOOD).color,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                )
                // Este comportamiento existía desde siempre pero era invisible.
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = visualDe(EntryKind.MOOD).glifo,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualDe(EntryKind.MOOD).color,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Se guardará como nota de ánimo aparte, con la misma hora",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorsCronicas.tenue,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Sello del tipo en la barra: sitúa en qué pantalla estás. */
@Composable
internal fun SelloTipo(kind: String) {
    val visual = visualDe(kind)
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(26.dp)
            .background(visual.contenedor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = visual.glifo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = visual.color,
        )
    }
}

@Composable
internal fun EtiquetaCampo(texto: String, opcional: Boolean = false) {
    Row(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (opcional) {
            Text(
                text = " (opcional)",
                style = MaterialTheme.typography.bodyMedium,
                color = colorsCronicas.tenue,
            )
        }
    }
}

@Composable
private fun BotonFoto(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

/** Botonera inferior fija: el camino rápido acaba siempre en el mismo sitio. */
@Composable
internal fun BarraGuardar(
    habilitado: Boolean,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit,
    textoGuardar: String = "Guardar",
    textoCancelar: String = "Cancelar",
    alineadoAlInicio: Boolean = false,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = if (alineadoAlInicio) Arrangement.SpaceBetween
                else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancelar) { Text(textoCancelar) }
                if (!alineadoAlInicio) Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onGuardar,
                    enabled = habilitado,
                    shape = RoundedCornerShape(999.dp),
                ) { Text(textoGuardar) }
            }
        }
    }
}

/**
 * Fila «Fecha y hora» con botón para cambiarla. La usan la pantalla de comida y
 * la de caminata, para poder anotar algo que se olvidó en su momento.
 */
@Composable
internal fun FilaFechaHora(
    selectedTime: Long,
    tinte: androidx.compose.ui.graphics.Color,
    onPicked: (Long) -> Unit,
) {
    val context = LocalContext.current
    val formato = remember { SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es")) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = tinte,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FECHA Y HORA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorsCronicas.tenue,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = formato.format(Date(selectedTime)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedButton(
                onClick = { pickDateTime(context, selectedTime, onPicked) },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cambiar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Abre el selector de fecha y, al confirmar, el de hora. Devuelve en [onPicked]
 * el timestamp resultante (con segundos a 0).
 */
private fun pickDateTime(context: Context, initial: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val result = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(result.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // No permitir fechas futuras: una comida olvidada siempre es de hoy o antes.
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}
