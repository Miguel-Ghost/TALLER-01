package com.software.taller01

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Actividad principal de ProxiTalk
 * Integra el sensor de proximidad, TTS, vibración y visualización en tiempo real
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var sensorInfoText: TextView
    private lateinit var dataCountText: TextView
    private lateinit var proximityGraph: ProximityGraphView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private lateinit var dataLogger: DataLogger
    private lateinit var proximitySensorManager: ProximitySensorManager

    private val updateHandler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    companion object {
        private const val TAG = "MainActivity"
        private const val UPDATE_INTERVAL = 100L // Actualizar cada 100ms
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Iniciando onCreate")
            setContentView(R.layout.activity_main)

            Log.d(TAG, "Inicializando ProxiTalk")

            initializeViews()
            initializeComponents()
            setupEventListeners()
            updateUI()

            Log.d(TAG, "onCreate completado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al inicializar la aplicación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        try {
            statusText = findViewById(R.id.statusText)
            sensorInfoText = findViewById(R.id.sensorInfoText)
            dataCountText = findViewById(R.id.dataCountText)
            proximityGraph = findViewById(R.id.proximityGraph)
            startButton = findViewById(R.id.startButton)
            stopButton = findViewById(R.id.stopButton)

            Log.d(TAG, "Vistas inicializadas correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
            throw e
        }
    }

    private fun initializeComponents() {
        try {
            // Inicializar DataLogger
            dataLogger = DataLogger()

            // Inicializar ProximitySensorManager
            proximitySensorManager = ProximitySensorManager(
                context = this,
                dataLogger = dataLogger,
                onGestureDetected = ::onGestureDetected
            )

            // Inicializar TTS
            proximitySensorManager.initializeTTS()

            // Configurar gráfico
            if (proximitySensorManager.isSensorAvailable()) {
                proximityGraph.setMaxRange(proximitySensorManager.getMaxRange())
            }

            Log.d(TAG, "Componentes inicializados correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar componentes: ${e.message}", e)
            throw e
        }
    }

    private fun setupEventListeners() {
        try {
            startButton.setOnClickListener {
                startMonitoring()
            }

            stopButton.setOnClickListener {
                stopMonitoring()
            }

            Log.d(TAG, "Event listeners configurados")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar event listeners: ${e.message}", e)
            throw e
        }
    }

    private fun startMonitoring() {
        try {
            if (!isMonitoring) {
                proximitySensorManager.startMonitoring()
                isMonitoring = true
                updateStatus("Monitoreando sensor de proximidad...")
                startUIUpdates()
                Log.d(TAG, "Monitoreo iniciado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar monitoreo: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar monitoreo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopMonitoring() {
        try {
            if (isMonitoring) {
                proximitySensorManager.stopMonitoring()
                isMonitoring = false
                updateStatus("Monitoreo detenido")
                stopUIUpdates()
                Log.d(TAG, "Monitoreo detenido")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener monitoreo: ${e.message}", e)
        }
    }

    private fun onGestureDetected() {
        runOnUiThread {
            try {
                updateStatus("¡Gesto detectado! Leyendo notificaciones...")
                Log.d(TAG, "Gesto detectado en MainActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error al manejar gesto detectado: ${e.message}", e)
            }
        }
    }

    private fun updateStatus(message: String) {
        try {
            statusText.text = message
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar estado: ${e.message}", e)
        }
    }

    private fun startUIUpdates() {
        updateHandler.post(updateRunnable)
    }

    private fun stopUIUpdates() {
        updateHandler.removeCallbacks(updateRunnable)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            try {
                updateUI()
                if (isMonitoring) {
                    updateHandler.postDelayed(this, UPDATE_INTERVAL)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en updateRunnable: ${e.message}", e)
            }
        }
    }

    private fun updateUI() {
        try {
            // Actualizar información del sensor
            val sensorAvailable = proximitySensorManager.isSensorAvailable()
            val sensorInfo = if (sensorAvailable) {
                "Sensor: Disponible (${String.format("%.1f", proximitySensorManager.getMaxRange())} cm)"
            } else {
                "Sensor: No disponible"
            }
            sensorInfoText.text = sensorInfo

            // Actualizar contador de datos
            val dataCount = dataLogger.getDataCount()
            dataCountText.text = "Datos: $dataCount"

            // Actualizar gráfico
            val recentData = dataLogger.getRecentData(30) // Últimos 30 segundos
            proximityGraph.updateData(recentData)

            // Actualizar estado de los botones
            startButton.isEnabled = !isMonitoring
            stopButton.isEnabled = isMonitoring
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar UI: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Aplicación en primer plano")
        try {
            if (isMonitoring) {
                startUIUpdates()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onResume: ${e.message}", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - Aplicación en segundo plano")
        try {
            stopUIUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error en onPause: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - Liberando recursos")
        try {
            stopMonitoring()
            proximitySensorManager.cleanup()
            updateHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroy: ${e.message}", e)
        }
    }
}