package com.software.taller01

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * Gestor del sensor de proximidad que implementa la lógica de detección de gestos
 * Detecta 3 pasadas cercanas al sensor dentro de 2 segundos
 */
class ProximitySensorManager(
    private val context: Context,
    private val dataLogger: DataLogger,
    private val onGestureDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var textToSpeech: TextToSpeech? = null

    // Variables para detección de gestos
    private val gestureEvents = mutableListOf<Long>()
    private val gestureTimeWindow = 3000L // 3 segundos
    private val requiredGestures = 2 //  2 pasadas


    private var isMonitoring = false
    private var lastDistance = 0f
    private var lastIsNear = false

    companion object {
        private const val TAG = "ProximitySensorManager"
    }

    /**
     * Inicializa el TextToSpeech
     */
    fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                Log.d(TAG, "TTS inicializado correctamente")
            } else {
                Log.e(TAG, "Error al inicializar TTS")
            }
        }
    }

    /**
     * Inicia el monitoreo del sensor
     */
    fun startMonitoring() {
        if (proximitySensor == null) {
            Log.e(TAG, "Sensor de proximidad no disponible")
            return
        }

        if (!isMonitoring) {
            sensorManager.registerListener(
                this,
                proximitySensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            isMonitoring = true
            Log.d(TAG, "Monitoreo del sensor iniciado")

            lastDistance = -1f
            lastIsNear = false
        }
    }

    /**
     * Detiene el monitoreo del sensor
     */
    fun stopMonitoring() {
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
            isMonitoring = false
            Log.d(TAG, "Monitoreo del sensor detenido")
        }
    }

    /**
     * Libera recursos
     */
    fun cleanup() {
        stopMonitoring()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_PROXIMITY) {
                val distance = sensorEvent.values[0]
                val threshold = 3.0f
                val isNear = distance < threshold

                // Registrar datos
                dataLogger.addData(distance, isNear)

                // Detectar cualquier cambio (cerca ↔ lejos)
                val delta = Math.abs(distance - lastDistance)
                if (delta == -1f || delta > 0.2f || lastIsNear != isNear) { // Cambio mínimo de 0.1cm o cambio de estado
                    Log.d(TAG, "Cambio de estado detectado: distancia=$distance, delta=$delta")
                    handleProximityEvent()
                }

                lastDistance = distance
                lastIsNear = isNear
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesario para este sensor
    }

    /**
     * Maneja un evento de proximidad (transición a cerca)
     */
    private fun handleProximityEvent() {
        val currentTime = System.currentTimeMillis()

        // Agregar el evento actual
        gestureEvents.add(currentTime)

        // Limpiar eventos antiguos fuera de la ventana de tiempo
        gestureEvents.removeAll { it < currentTime - gestureTimeWindow }

        Log.d(TAG, "Eventos de gesto: ${gestureEvents.size}")

        // Verificar si se detectó el gesto completo
        if (gestureEvents.size >= requiredGestures) {
            detectGesture()
        }

        Log.d(TAG, "Evento de proximidad detectado! Eventos acumulados: ${gestureEvents.size}")
    }

    /**
     * Detecta el gesto y ejecuta las acciones correspondientes
     */
    private fun detectGesture() {
        Log.d(TAG, "¡Gesto detectado!")

        // Limpiar eventos para evitar detecciones múltiples
        gestureEvents.clear()

        // Vibración de confirmación
        vibrate()

        // Leer notificaciones
        speakNotifications()

        // Notificar a la UI
        onGestureDetected()
    }

    /**
     * Activa la vibración
     */
    private fun vibrate() {
        try {
            vibrator.vibrate(200) // Vibración de 200ms
            Log.d(TAG, "Vibración activada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al activar vibración: ${e.message}")
        }
    }

    /**
     * Lee las notificaciones usando TTS
     */
    private fun speakNotifications() {
        val notificationText = context.getString(R.string.notification_simulated)

        textToSpeech?.speak(
            notificationText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "notification_${System.currentTimeMillis()}"
        )

        Log.d(TAG, "Leyendo notificaciones: $notificationText")
    }

    /**
     * Verifica si el sensor está disponible
     */
    fun isSensorAvailable(): Boolean {
        return proximitySensor != null
    }

    /**
     * Obtiene el rango máximo del sensor
     */
    fun getMaxRange(): Float {
        return proximitySensor?.maximumRange ?: 0f
    }
}
