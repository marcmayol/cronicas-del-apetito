package com.marcm.cronicasapetito.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.marcm.cronicasapetito.R

/**
 * Dictar en vez de escribir, con el reconocimiento de voz que va dentro del
 * propio móvil. El audio no sale del teléfono: es la misma promesa que el
 * resto de la app, y lo que se dicta aquí (qué has comido, cómo estás) es
 * justo lo que menos debería viajar.
 *
 * Solo existe en Android 12 o más y si el móvil trae reconocimiento sin
 * conexión. Donde no lo hay, el botón no aparece y queda el micro del teclado,
 * que ya es cosa de quien lo use.
 */
object Dictado {
    fun disponible(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    /**
     * Añade lo dictado detrás de lo que ya hubiera escrito, con un espacio, y
     * con mayúscula si empieza el texto. El reconocedor no pone puntos, así que
     * tampoco se inventan aquí.
     */
    fun unir(actual: String, dictado: String): String {
        val nuevo = dictado.trim()
        if (nuevo.isEmpty()) return actual
        val base = actual.trimEnd()
        return if (base.isEmpty()) nuevo.replaceFirstChar { it.uppercase() }
        else "$base $nuevo"
    }
}

/**
 * El micrófono que va dentro de una caja de texto (como `trailingIcon`).
 * [onDictado] recibe la frase reconocida; quien lo usa decide cómo juntarla.
 */
@Composable
fun BotonDictar(tinte: Color, onDictado: (String) -> Unit) {
    val context = LocalContext.current
    if (!remember { Dictado.disponible(context) }) return

    var escuchando by remember { mutableStateOf(false) }
    val reconocedor = remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun parar() {
        reconocedor.value?.destroy()
        reconocedor.value = null
        escuchando = false
    }

    fun empezar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        parar()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // El idioma en el que se ve la app, que es en el que se va a hablar.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, context.resources.configuration.locales[0].toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        val sr = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let(onDictado)
                parar()
            }

            override fun onError(error: Int) {
                when (error) {
                    // Sin el paquete de idioma sin conexión: se pide la descarga
                    // (Android 13+) y se explica qué pasa.
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            runCatching { sr.triggerModelDownload(intent) }
                        }
                        Toast.makeText(context, R.string.dictation_no_language, Toast.LENGTH_LONG).show()
                    }
                    // Callarse o pulsar antes de hablar no es un fallo que contar.
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_CLIENT -> Unit
                    else -> Toast.makeText(context, R.string.dictation_failed, Toast.LENGTH_SHORT).show()
                }
                parar()
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        reconocedor.value = sr
        escuchando = true
        sr.startListening(intent)
    }

    val pedirMicro = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) empezar()
        else Toast.makeText(context, R.string.dictation_no_permission, Toast.LENGTH_LONG).show()
    }

    DisposableEffect(Unit) { onDispose { parar() } }

    IconButton(onClick = {
        when {
            escuchando -> reconocedor.value?.stopListening()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> empezar()
            else -> pedirMicro.launch(Manifest.permission.RECORD_AUDIO)
        }
    }) {
        Icon(
            imageVector = if (escuchando) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = stringResource(
                if (escuchando) R.string.dictation_stop else R.string.dictation_start
            ),
            tint = if (escuchando) tinte else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
