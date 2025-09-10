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
 * Detecta 2 pasadas cercanas al sensor dentro de 3 segundos
 * Incluye fallback a sensor de luz para pantalla encendida
 */
class ProximitySensorManager(
    private val context: Context,
    private val dataLogger: DataLogger,
    private val onGestureDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var textToSpeech: TextToSpeech? = null

    // Variables para detección de gestos
    private val gestureEvents = mutableListOf<Long>()
    private val gestureTimeWindow = 3000L // 3 segundos
    private val requiredGestures = 2 // 2 pasadas

    private var isMonitoring = false
    private var lastDistance = 0f
    private var lastIsNear = false
    private var lastLightValue = 0f
    private var useLightSensor = false
    private var useGyroscope = false
    private var sensorType = "PROXIMITY"
    
    // Variables para giroscopio/acelerómetro
    private var lastAccelerationX = 0f
    private var lastAccelerationY = 0f
    private var lastAccelerationZ = 0f
    private var lastGestureTime = 0L
    private val gestureCooldown = 2000L // 2 segundos entre gestos

    companion object {
        private const val TAG = "ProximitySensorManager"
        private const val DEBUG_SENSOR = true // Habilitar logs detallados
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
     * Inicia el monitoreo del sensor con sistema de prioridad
     */
    fun startMonitoring() {
        if (!isMonitoring) {
            // Sistema de prioridad: Proximidad > Luz > Giroscopio
            when {
                proximitySensor != null -> {
                    // Prioridad 1: Sensor de proximidad
                    sensorManager.registerListener(
                        this,
                        proximitySensor,
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                    useLightSensor = false
                    useGyroscope = false
                    sensorType = "PROXIMITY"
                    Log.d(TAG, "Monitoreo iniciado con sensor de PROXIMIDAD (Prioridad 1)")
                }
                lightSensor != null -> {
                    // Prioridad 2: Sensor de luz
                    sensorManager.registerListener(
                        this,
                        lightSensor,
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                    useLightSensor = true
                    useGyroscope = false
                    sensorType = "LIGHT"
                    Log.d(TAG, "Monitoreo iniciado con sensor de LUZ (Prioridad 2)")
                }
                accelerometerSensor != null -> {
                    // Prioridad 3: Acelerómetro para detección de sacudida
                    sensorManager.registerListener(
                        this,
                        accelerometerSensor,
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                    useLightSensor = false
                    useGyroscope = true
                    sensorType = "GYROSCOPE"
                    Log.d(TAG, "Monitoreo iniciado con ACELERÓMETRO (Prioridad 3)")
                }
                else -> {
                    Log.e(TAG, "Ningún sensor disponible (proximidad, luz ni acelerómetro)")
                    return
                }
            }
            
            isMonitoring = true
            lastDistance = -1f
            lastIsNear = false
            lastLightValue = -1f
            lastAccelerationX = 0f
            lastAccelerationY = 0f
            lastAccelerationZ = 0f
            lastGestureTime = 0L
            gestureEvents.clear()
            
            Log.d(TAG, "Monitoreo del sensor iniciado - Tipo: $sensorType")
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
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_PROXIMITY -> {
                    val distance = sensorEvent.values[0]
                    val threshold = 3.0f
                    val isNear = distance < threshold

                    // Registrar datos
                    dataLogger.addData(distance, isNear)

                    // Detectar cambios más sensibles
                    val delta = Math.abs(distance - lastDistance)
                    val hasSignificantChange = if (lastDistance == -1f) {
                        true // Primer dato
                    } else {
                        delta > 0.1f || lastIsNear != isNear // Cambio mínimo de 0.1cm o cambio de estado
                    }

                    if (hasSignificantChange) {
                        if (DEBUG_SENSOR) {
                            Log.d(TAG, "PROXIMIDAD - Cambio detectado: distancia=$distance, delta=$delta, cerca=$isNear")
                        }
                        handleProximityEvent()
                    } else if (DEBUG_SENSOR) {
                        Log.v(TAG, "PROXIMIDAD - Sin cambio significativo: distancia=$distance, delta=$delta")
                    }

                    lastDistance = distance
                    lastIsNear = isNear
                }
                
                Sensor.TYPE_LIGHT -> {
                    val lightValue = sensorEvent.values[0]
                    
                    // Convertir valor de luz a distancia aproximada para el DataLogger
                    val estimatedDistance = if (lightValue < 10f) 1.0f else 5.0f
                    val isNear = lightValue < 10f
                    
                    // Registrar datos usando distancia estimada
                    dataLogger.addData(estimatedDistance, isNear)

                    // Detectar cambios en luz (gestos que bloquean la luz)
                    val delta = Math.abs(lightValue - lastLightValue)
                    val hasSignificantChange = if (lastLightValue == -1f) {
                        true // Primer dato
                    } else {
                        delta > 5f // Cambio significativo en luz
                    }

                    if (hasSignificantChange) {
                        if (DEBUG_SENSOR) {
                            Log.d(TAG, "LUZ - Cambio detectado: luz=$lightValue, delta=$delta, cerca=$isNear")
                        }
                        handleProximityEvent()
                    } else if (DEBUG_SENSOR) {
                        Log.v(TAG, "LUZ - Sin cambio significativo: luz=$lightValue, delta=$delta")
                    }

                    lastLightValue = lightValue
                }
                
                Sensor.TYPE_ACCELEROMETER -> {
                    val accelerationX = sensorEvent.values[0]
                    val accelerationY = sensorEvent.values[1]
                    val accelerationZ = sensorEvent.values[2]
                    
                    // Calcular aceleración total
                    val totalAcceleration = Math.sqrt(
                        (accelerationX * accelerationX + 
                         accelerationY * accelerationY + 
                         accelerationZ * accelerationZ).toDouble()
                    ).toFloat()
                    
                    // Detectar sacudida (aceleración > 10 m/s²)
                    val shakeThreshold = 10.0f
                    val isShake = totalAcceleration > shakeThreshold
                    
                    // Convertir a distancia estimada para el DataLogger
                    val estimatedDistance = if (isShake) 1.0f else 5.0f
                    dataLogger.addData(estimatedDistance, isShake)
                    
                    // Detectar cambios significativos en aceleración
                    val deltaX = Math.abs(accelerationX - lastAccelerationX)
                    val deltaY = Math.abs(accelerationY - lastAccelerationY)
                    val deltaZ = Math.abs(accelerationZ - lastAccelerationZ)
                    val maxDelta = Math.max(deltaX, Math.max(deltaY, deltaZ))
                    
                    val hasSignificantChange = if (lastAccelerationX == 0f && lastAccelerationY == 0f && lastAccelerationZ == 0f) {
                        false // Ignorar primer dato
                    } else {
                        maxDelta > 5.0f // Cambio significativo en aceleración
                    }
                    
                    if (hasSignificantChange && isShake) {
                        if (DEBUG_SENSOR) {
                            Log.d(TAG, "GIROSCOPIO - Sacudida detectada: aceleración=$totalAcceleration, delta=$maxDelta")
                        }
                        handleGyroscopeEvent()
                    } else if (DEBUG_SENSOR) {
                        Log.v(TAG, "GIROSCOPIO - Sin sacudida: aceleración=$totalAcceleration, delta=$maxDelta")
                    }
                    
                    lastAccelerationX = accelerationX
                    lastAccelerationY = accelerationY
                    lastAccelerationZ = accelerationZ
                }
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

        // Verificar cooldown para evitar detecciones múltiples
        if (currentTime - lastGestureTime < gestureCooldown) {
            if (DEBUG_SENSOR) {
                Log.d(TAG, "$sensorType - Evento ignorado por cooldown")
            }
            return
        }

        // Agregar el evento actual
        gestureEvents.add(currentTime)

        // Limpiar eventos antiguos fuera de la ventana de tiempo
        gestureEvents.removeAll { it < currentTime - gestureTimeWindow }

        Log.d(TAG, "$sensorType - Eventos de gesto: ${gestureEvents.size}/${requiredGestures}")

        // Verificar si se detectó el gesto completo
        if (gestureEvents.size >= requiredGestures) {
            Log.d(TAG, "$sensorType - ¡Gesto completo detectado! Ejecutando acciones...")
            detectGesture()
        } else {
            Log.d(TAG, "$sensorType - Evento detectado, faltan ${requiredGestures - gestureEvents.size} eventos")
        }
    }

    /**
     * Maneja un evento del giroscopio (sacudida detectada)
     */
    private fun handleGyroscopeEvent() {
        val currentTime = System.currentTimeMillis()

        // Verificar cooldown para evitar detecciones múltiples
        if (currentTime - lastGestureTime < gestureCooldown) {
            if (DEBUG_SENSOR) {
                Log.d(TAG, "GIROSCOPIO - Sacudida ignorada por cooldown")
            }
            return
        }

        // Agregar el evento actual
        gestureEvents.add(currentTime)

        // Limpiar eventos antiguos fuera de la ventana de tiempo
        gestureEvents.removeAll { it < currentTime - gestureTimeWindow }

        Log.d(TAG, "GIROSCOPIO - Sacudidas detectadas: ${gestureEvents.size}/${requiredGestures}")

        // Verificar si se detectó el gesto completo
        if (gestureEvents.size >= requiredGestures) {
            Log.d(TAG, "GIROSCOPIO - ¡Gesto de sacudida completo detectado! Ejecutando acciones...")
            detectGesture()
        } else {
            Log.d(TAG, "GIROSCOPIO - Sacudida detectada, faltan ${requiredGestures - gestureEvents.size} sacudidas")
        }
    }

    /**
     * Detecta el gesto y ejecuta las acciones correspondientes
     */
    private fun detectGesture() {
        Log.d(TAG, "$sensorType - ¡Gesto detectado! Ejecutando acciones...")

        // Actualizar tiempo del último gesto para cooldown
        lastGestureTime = System.currentTimeMillis()

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
     * Verifica si algún sensor está disponible
     */
    fun isSensorAvailable(): Boolean {
        return proximitySensor != null || lightSensor != null || accelerometerSensor != null
    }

    /**
     * Obtiene el rango máximo del sensor
     */
    fun getMaxRange(): Float {
        return when {
            useGyroscope -> 10.0f // Rango estimado para acelerómetro
            useLightSensor -> 5.0f // Rango estimado para sensor de luz
            else -> proximitySensor?.maximumRange ?: 0f
        }
    }

    /**
     * Obtiene el tipo de sensor actual
     */
    fun getCurrentSensorType(): String {
        return sensorType
    }

    /**
     * Obtiene información detallada del sensor
     */
    fun getSensorInfo(): String {
        return when {
            proximitySensor != null && !useLightSensor && !useGyroscope -> "Proximidad (${String.format("%.1f", proximitySensor.maximumRange)} cm)"
            lightSensor != null && useLightSensor -> "Luz (fallback)"
            accelerometerSensor != null && useGyroscope -> "Giroscopio (sacudida)"
            proximitySensor != null -> "Proximidad disponible"
            lightSensor != null -> "Luz disponible"
            accelerometerSensor != null -> "Giroscopio disponible"
            else -> "Ningún sensor"
        }
    }

    /**
     * Verifica y registra información detallada de sensores disponibles
     */
    fun logSensorAvailability() {
        Log.d(TAG, "=== VERIFICACIÓN DE SENSORES ===")
        Log.d(TAG, "Sensor de proximidad: ${if (proximitySensor != null) "DISPONIBLE" else "NO DISPONIBLE"}")
        if (proximitySensor != null) {
            Log.d(TAG, "  - Rango máximo: ${proximitySensor.maximumRange} cm")
            Log.d(TAG, "  - Resolución: ${proximitySensor.resolution}")
            Log.d(TAG, "  - Potencia: ${proximitySensor.power} mA")
        }
        
        Log.d(TAG, "Sensor de luz: ${if (lightSensor != null) "DISPONIBLE" else "NO DISPONIBLE"}")
        if (lightSensor != null) {
            Log.d(TAG, "  - Rango máximo: ${lightSensor.maximumRange} lux")
            Log.d(TAG, "  - Resolución: ${lightSensor.resolution}")
            Log.d(TAG, "  - Potencia: ${lightSensor.power} mA")
        }
        
        Log.d(TAG, "Acelerómetro: ${if (accelerometerSensor != null) "DISPONIBLE" else "NO DISPONIBLE"}")
        if (accelerometerSensor != null) {
            Log.d(TAG, "  - Rango máximo: ${accelerometerSensor.maximumRange} m/s²")
            Log.d(TAG, "  - Resolución: ${accelerometerSensor.resolution}")
            Log.d(TAG, "  - Potencia: ${accelerometerSensor.power} mA")
        }
        
        Log.d(TAG, "Sensor actual: $sensorType")
        Log.d(TAG, "================================")
    }
}
