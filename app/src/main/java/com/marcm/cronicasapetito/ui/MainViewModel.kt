package com.marcm.cronicasapetito.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.data.Periodos
import com.marcm.cronicasapetito.data.RangoFechas
import com.marcm.cronicasapetito.data.ResumenDia
import com.marcm.cronicasapetito.data.ResumenPeriodo
import com.marcm.cronicasapetito.data.Resumenes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/** Las tres formas de mirar los mismos datos. */
enum class Vista { DIA, SEMANA, MES }

/**
 * Estado de la pantalla principal. Vista, filtro y periodo son tres cosas
 * independientes que se combinan: el filtro se conserva al cambiar de vista y al
 * navegar entre semanas o meses — lo que queda fuera se ve atenuado, no
 * desaparece.
 */
data class EstadoPrincipal(
    val vista: Vista = Vista.DIA,
    val filtro: RangoFechas? = null,
    /** Día ancla: define qué semana y qué mes se están mirando. */
    val ancla: LocalDate = LocalDate.now(),
    val entradas: List<MealEntry> = emptyList(),
    val resumenPorDia: Map<LocalDate, ResumenDia> = emptyMap(),
    val diaSeleccionado: LocalDate? = null,
) {
    val semana: List<LocalDate> get() = Periodos.semanaDe(ancla)
    val mes: YearMonth get() = YearMonth.from(ancla)

    /** Días que entran en el periodo visible, ya recortados por el filtro. */
    val diasDelPeriodo: List<LocalDate>
        get() {
            val todos = when (vista) {
                Vista.SEMANA -> semana
                Vista.MES -> Periodos.casillasDe(mes).filterNotNull()
                Vista.DIA -> entradas.map { Periodos.fechaDe(it.timestampMillis) }.distinct()
            }
            return filtro?.let { r -> todos.filter { it in r } } ?: todos
        }

    val resumenPeriodo: ResumenPeriodo
        get() = Resumenes.delPeriodo(resumenPorDia, diasDelPeriodo)

    /** Registros que de verdad se están mirando (y que se comparten). */
    val entradasVisibles: List<MealEntry>
        get() {
            val enPeriodo = when (vista) {
                Vista.DIA -> entradas
                else -> {
                    val dias = diasDelPeriodo.toSet()
                    entradas.filter { Periodos.fechaDe(it.timestampMillis) in dias }
                }
            }
            return filtro?.let { r ->
                enPeriodo.filter { Periodos.fechaDe(it.timestampMillis) in r }
            } ?: enPeriodo
        }

    val hayFiltro: Boolean get() = filtro != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MealRepository,
    private val estadoGuardado: SavedStateHandle,
) : ViewModel() {

    private val vista = MutableStateFlow(
        estadoGuardado.get<String>(CLAVE_VISTA)?.let { Vista.valueOf(it) } ?: Vista.DIA
    )
    private val filtro = MutableStateFlow(estadoGuardado.leerRango())
    private val ancla = MutableStateFlow(
        estadoGuardado.get<String>(CLAVE_ANCLA)?.let { LocalDate.parse(it) } ?: LocalDate.now()
    )
    private val diaSeleccionado = MutableStateFlow<LocalDate?>(null)

    /**
     * Las entradas se piden ya acotadas al filtro cuando lo hay: con meses de
     * registro no tiene sentido traerse todo el historial para tapar la mitad.
     */
    private val entradas = filtro.flatMapLatest { rango ->
        if (rango == null) repository.observeAll()
        else repository.observeInRange(rango.inicioMillis, rango.finMillis)
    }

    val estado: StateFlow<EstadoPrincipal> =
        combine(vista, filtro, ancla, entradas, diaSeleccionado) { v, f, a, e, sel ->
            EstadoPrincipal(
                vista = v,
                filtro = f,
                ancla = a,
                entradas = e,
                resumenPorDia = Resumenes.porDia(e),
                diaSeleccionado = sel,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoPrincipal())

    /** Nº de registros dentro del filtro, para el contador junto al chip. */
    val registrosFiltrados: StateFlow<Int> =
        estado.map { it.entradasVisibles.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun cambiarVista(nueva: Vista) {
        vista.value = nueva
        estadoGuardado[CLAVE_VISTA] = nueva.name
        // El filtro se conserva a propósito: cambiar de vista no es empezar de cero.
    }

    fun aplicarFiltro(rango: RangoFechas?) {
        filtro.value = rango
        estadoGuardado[CLAVE_DESDE] = rango?.desde?.toString()
        estadoGuardado[CLAVE_HASTA] = rango?.hasta?.toString()
        // Si el rango cae fuera del periodo visible, saltamos a su fecha final:
        // es lo que el usuario acaba de pedir ver.
        if (rango != null && ancla.value !in rango) moverAncla(rango.hasta)
    }

    fun quitarFiltro() = aplicarFiltro(null)

    fun periodoAnterior() = moverAncla(desplazar(-1))

    fun periodoSiguiente() = moverAncla(desplazar(+1))

    fun seleccionarDia(dia: LocalDate?) {
        diaSeleccionado.value = dia
    }

    /** Abrir un día concreto desde Semana o Mes: cambia de vista y de ancla. */
    fun abrirDia(dia: LocalDate) {
        moverAncla(dia)
        diaSeleccionado.value = dia
        cambiarVista(Vista.DIA)
    }

    private fun desplazar(pasos: Long): LocalDate = when (vista.value) {
        Vista.DIA -> ancla.value.plusDays(pasos)
        Vista.SEMANA -> ancla.value.plusWeeks(pasos)
        Vista.MES -> ancla.value.plusMonths(pasos)
    }

    private fun moverAncla(dia: LocalDate) {
        ancla.value = dia
        estadoGuardado[CLAVE_ANCLA] = dia.toString()
        diaSeleccionado.value = null
    }

    private fun SavedStateHandle.leerRango(): RangoFechas? {
        val desde = get<String>(CLAVE_DESDE) ?: return null
        val hasta = get<String>(CLAVE_HASTA) ?: return null
        return RangoFechas(LocalDate.parse(desde), LocalDate.parse(hasta))
    }

    companion object {
        private const val CLAVE_VISTA = "vista"
        private const val CLAVE_DESDE = "filtro_desde"
        private const val CLAVE_HASTA = "filtro_hasta"
        private const val CLAVE_ANCLA = "ancla"

        fun factory(repository: MealRepository) = viewModelFactory {
            initializer { MainViewModel(repository, createSavedStateHandle()) }
        }
    }
}
