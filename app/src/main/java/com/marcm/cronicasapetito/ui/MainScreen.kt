package com.marcm.cronicasapetito.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.actualizador.Actualizador
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.Periodos
import com.marcm.cronicasapetito.export.ImagenExporter
import com.marcm.cronicasapetito.export.PdfExporter
import com.marcm.cronicasapetito.export.shareFile
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.notifications.SleepPrefs
import com.marcm.cronicasapetito.notifications.horaTexto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: MealRepository,
    actualizador: Actualizador,
    onRequestExactAlarmPermission: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(repository))
    val estado by viewModel.estado.collectAsState()
    val registros by viewModel.registrosFiltrados.collectAsState()
    val estadoActualizacion by actualizador.estado.collectAsState()

    var mostrarFiltro by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarAnotar by remember { mutableStateOf(false) }
    var mostrarGimnasio by remember { mutableStateOf(false) }
    var gimnasioAEditar by remember { mutableStateOf<MealEntry?>(null) }
    var registroConAcciones by remember { mutableStateOf<MealEntry?>(null) }
    val avisos = remember { SnackbarHostState() }
    var mostrarDormir by remember { mutableStateOf(false) }

    // Los ajustes se cambian en otra pantalla: al volver hay que releerlos, o el
    // botón de dormir seguiría ahí con los recordatorios ya apagados.
    var relectura by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { relectura++ }
    var durmiendo by remember(relectura) { mutableStateOf(SleepPrefs.isSleeping(context)) }
    val comidaActiva = remember(relectura) { PrefsRecordatorios.comidaActiva(context) }
    val horaDespertar = remember(relectura) {
        horaTexto(PrefsRecordatorios.comidaInicioMin(context))
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) onRequestExactAlarmPermission()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    if (comidaActiva) {
                        IconButton(onClick = { mostrarDormir = true }) {
                            Icon(Icons.Filled.Bedtime, stringResource(R.string.sleep_button))
                        }
                    }
                    IconButton(onClick = { mostrarCompartir = true }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.action_share))
                    }
                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
                        }
                        DropdownMenu(
                            expanded = mostrarMenu,
                            onDismissRequest = { mostrarMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_settings)) },
                                onClick = {
                                    mostrarMenu = false
                                    context.startActivity(
                                        Intent(context, AjustesActivity::class.java)
                                    )
                                },
                            )
                            // Con un filtro puesto, «exportar» tiene que poder
                            // significar «lo que estoy mirando»: si no, el menú
                            // devuelve el historial entero sin avisar.
                            estado.filtro?.let { rango ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_export_filtered)) },
                                    onClick = {
                                        mostrarMenu = false
                                        scope.launch {
                                            val file = PdfExporter.export(
                                                context = context,
                                                // estado.entradas ya viene acotado al
                                                // rango: sale el filtro entero, aunque
                                                // cruce varios meses.
                                                entries = estado.entradas
                                                    .sortedByDescending { it.timestampMillis },
                                                titulo = Fechas.rangoConAnio(rango),
                                            )
                                            shareFile(context, file)
                                        }
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(R.string.menu_export_all))
                                        if (estado.hayFiltro) {
                                            Text(
                                                text = stringResource(R.string.menu_export_all_note),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorsCronicas.tenue,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    mostrarMenu = false
                                    scope.launch {
                                        val todo = repository.getAll()
                                        val file = PdfExporter.export(
                                            context, todo, titulo = context.getString(R.string.history_all)
                                        )
                                        shareFile(context, file)
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarAnotar = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            BannerActualizacion(
                estado = estadoActualizacion,
                onActualizar = { actualizador.actualizarAhora() },
            )

            if (durmiendo) {
                BannerDormir(
                    hora = horaDespertar,
                    onDespertar = {
                        MealAlarmScheduler.wakeUp(context)
                        durmiendo = false
                        Toast.makeText(
                            context, context.getString(R.string.wake_toast), Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }

            // Zona de control: selector de vista + filtro. Siempre visible, para
            // que nunca haya duda de qué se está mirando.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectorVista(
                    vista = estado.vista,
                    onCambiar = viewModel::cambiarVista,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                BotonFiltro(
                    activo = estado.hayFiltro,
                    onClick = { mostrarFiltro = true },
                )
            }

            estado.filtro?.let { rango ->
                ChipFiltro(
                    rango = rango,
                    registros = registros,
                    onQuitar = viewModel::quitarFiltro,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (estado.vista != Vista.DIA) {
                NavegadorPeriodo(
                    titulo = when (estado.vista) {
                        Vista.SEMANA -> Fechas.semana(Periodos.lunesDe(estado.ancla))
                        else -> Fechas.mes(estado.mes)
                    },
                    onAnterior = viewModel::periodoAnterior,
                    onSiguiente = viewModel::periodoSiguiente,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    estado.entradasVisibles.isEmpty() && estado.vista == Vista.DIA ->
                        VacioSegunFiltro(estado, viewModel::quitarFiltro)

                    estado.vista == Vista.DIA -> VistaDia(
                        estado = estado,
                        onAccionesRegistro = { registroConAcciones = it },
                    )

                    estado.vista == Vista.SEMANA -> VistaSemana(
                        estado = estado,
                        onAbrirDia = viewModel::abrirDia,
                    )

                    else -> VistaMes(
                        estado = estado,
                        onSeleccionarDia = viewModel::seleccionarDia,
                        onAbrirDia = viewModel::abrirDia,
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Hojas y diálogos
    // -----------------------------------------------------------------------

    if (mostrarFiltro) {
        FiltroSheet(
            filtroActual = estado.filtro,
            onCerrar = { mostrarFiltro = false },
            onAplicar = {
                mostrarFiltro = false
                viewModel.aplicarFiltro(it)
            },
        )
    }

    if (mostrarCompartir) {
        CompartirSheet(
            contexto = contextoDeCompartir(estado),
            formatoSugerido = if (estado.vista == Vista.DIA) FormatoCompartir.PDF
            else FormatoCompartir.IMAGEN,
            onCerrar = { mostrarCompartir = false },
            onCompartir = { formato ->
                mostrarCompartir = false
                scope.launch {
                    val file = when (formato) {
                        FormatoCompartir.PDF -> PdfExporter.export(
                            context = context,
                            entries = estado.entradasVisibles.sortedByDescending { it.timestampMillis },
                            titulo = contextoDeCompartir(estado),
                        )
                        FormatoCompartir.IMAGEN -> when (estado.vista) {
                            Vista.SEMANA -> ImagenExporter.semana(
                                context,
                                Periodos.lunesDe(estado.ancla),
                                estado.resumenPorDia,
                                estado.resumenPeriodo,
                                estado.filtro,
                            )
                            Vista.MES -> ImagenExporter.mes(
                                context,
                                estado.mes,
                                estado.resumenPorDia,
                                estado.resumenPeriodo,
                                estado.filtro,
                            )
                            // La vista Día es una lista: como imagen se cortaría.
                            Vista.DIA -> PdfExporter.export(
                                context = context,
                                entries = estado.entradasVisibles.sortedByDescending { it.timestampMillis },
                                titulo = contextoDeCompartir(estado),
                            )
                        }
                    }
                    shareFile(context, file)
                }
            },
        )
    }

    if (mostrarAnotar) {
        AddEntryPickerDialog(
            onDismiss = { mostrarAnotar = false },
            onPickFood = {
                mostrarAnotar = false
                context.startActivity(Intent(context, EntryActivity::class.java))
            },
            onPickWalk = {
                mostrarAnotar = false
                context.startActivity(
                    Intent(context, WalkMoodActivity::class.java).apply {
                        putExtra(WalkMoodActivity.EXTRA_START_AT_MINUTES, true)
                    }
                )
            },
            onPickGym = {
                mostrarAnotar = false
                mostrarGimnasio = true
            },
            onPickMood = {
                mostrarAnotar = false
                context.startActivity(
                    Intent(context, WalkMoodActivity::class.java).apply {
                        putExtra(WalkMoodActivity.EXTRA_SOLO_ANIMO, true)
                    }
                )
            },
        )
    }

    registroConAcciones?.let { entrada ->
        val textoBorrado = stringResource(R.string.entry_deleted)
        val textoDeshacer = stringResource(R.string.action_undo)
        AccionesRegistroSheet(
            entry = entrada,
            onCerrar = { registroConAcciones = null },
            onEditar = {
                registroConAcciones = null
                val destino = intentDeEdicion(context, entrada)
                if (destino != null) context.startActivity(destino)
                else gimnasioAEditar = entrada   // el sí/no se cambia aquí mismo
            },
            onBorrar = {
                registroConAcciones = null
                viewModel.borrar(entrada) { borrada ->
                    scope.launch {
                        // Borrar algo que anotaste hace semanas no debería
                        // depender de que aciertes a la primera.
                        val r = avisos.showSnackbar(
                            message = textoBorrado,
                            actionLabel = textoDeshacer,
                            duration = SnackbarDuration.Long,
                        )
                        if (r == SnackbarResult.ActionPerformed) {
                            viewModel.deshacerBorrado(borrada)
                        }
                    }
                }
            },
        )
    }

    if (mostrarGimnasio) {
        GymQuestionDialog(
            onDismiss = { mostrarGimnasio = false },
            onAnswer = { fue ->
                mostrarGimnasio = false
                scope.launch { repository.addGym(fue) }
            },
        )
    }

    gimnasioAEditar?.let { entrada ->
        GymQuestionDialog(
            onDismiss = { gimnasioAEditar = null },
            onAnswer = { fue ->
                gimnasioAEditar = null
                viewModel.corregirGimnasio(entrada, fue)
            },
        )
    }

    if (mostrarDormir) {
        DialogoDormir(
            hora = horaDespertar,
            onCerrar = { mostrarDormir = false },
            onConfirmar = {
                mostrarDormir = false
                MealAlarmScheduler.goToSleep(context)
                durmiendo = true
                Toast.makeText(
                    context,
                    context.getString(R.string.sleep_toast, horaDespertar),
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }
}

/** Lo que dice la cabecera de lo compartido: vista + periodo + filtro. */
private fun contextoDeCompartir(estado: EstadoPrincipal): String = when (estado.vista) {
    Vista.SEMANA -> Fechas.semana(Periodos.lunesDe(estado.ancla)) +
        (estado.filtro?.let { " · filtrado ${Fechas.rango(it).lowercase()}" } ?: "")
    Vista.MES -> Fechas.mes(estado.mes) +
        (estado.filtro?.let { " · filtrado ${Fechas.rango(it).lowercase()}" } ?: "")
    Vista.DIA -> estado.filtro?.let { Fechas.rangoConAnio(it) } ?: "Historial completo"
}

@Composable
private fun BotonFiltro(activo: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 40.dp)
                .background(
                    if (activo) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface,
                    CircleShape
                )
                .border(
                    1.dp,
                    if (activo) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = stringResource(R.string.filter_by_dates),
                modifier = Modifier.size(18.dp),
                tint = if (activo) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (activo) {
            Box(
                Modifier
                    .padding(top = 2.dp, end = 4.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/** Dos vacíos distintos: no es lo mismo no tener nada que estar filtrando. */
@Composable
private fun VacioSegunFiltro(estado: EstadoPrincipal, onQuitarFiltro: () -> Unit) {
    val filtro = estado.filtro
    if (filtro == null) {
        EstadoVacio(
            icono = IconoCuaderno,
            mensaje = stringResource(R.string.empty_history),
        )
    } else {
        EstadoVacio(
            icono = IconoCalendario,
            mensaje = "No hay nada anotado ${Fechas.rango(filtro).lowercase()}.",
            accion = {
                TextButton(onClick = onQuitarFiltro) { Text(stringResource(R.string.filter_clear)) }
            },
        )
    }
}

@Composable
private fun BannerDormir(hora: String, onDespertar: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sleep_banner, hora),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDespertar) {
                Text(stringResource(R.string.wake_button))
            }
        }
    }
}

@Composable
private fun DialogoDormir(hora: String, onCerrar: () -> Unit, onConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = {
            Box(
                Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = { Text(stringResource(R.string.sleep_dialog_title)) },
        text = { Text(stringResource(R.string.sleep_dialog_text, hora)) },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(stringResource(R.string.sleep_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.sleep_cancel)) }
        },
    )
}
