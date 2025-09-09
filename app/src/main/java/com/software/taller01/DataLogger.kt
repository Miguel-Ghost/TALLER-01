package com.software.taller01

import kotlin.collections.ArrayList

/**
 * DataLogger para almacenar y gestionar datos del sensor de proximidad
 * Mantiene un buffer circular de los últimos 30 segundos de datos
 */
class DataLogger {
    
    data class SensorData(
        val timestamp: Long,
        val distance: Float,
        val isNear: Boolean
    )
    
    private val dataBuffer = ArrayList<SensorData>()
    private val maxDataPoints = 300
    private val maxTimeWindow = 30000L
    
    /**
     * Agrega un nuevo dato del sensor
     */
    fun addData(distance: Float, isNear: Boolean) {
        val currentTime = System.currentTimeMillis()
        val sensorData = SensorData(currentTime, distance, isNear)
        
        dataBuffer.add(sensorData)

        cleanupOldData(currentTime)

        if (dataBuffer.size > maxDataPoints) {
            dataBuffer.removeAt(0)
        }
    }
    
    /**
     * Limpia datos más antiguos que la ventana de tiempo
     */
    private fun cleanupOldData(currentTime: Long) {
        val cutoffTime = currentTime - maxTimeWindow
        dataBuffer.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Obtiene todos los datos actuales
     */
    fun getAllData(): List<SensorData> {
        return dataBuffer.toList()
    }
    
    /**
     * Obtiene los datos de los últimos N segundos
     */
    fun getRecentData(seconds: Int): List<SensorData> {
        val cutoffTime = System.currentTimeMillis() - (seconds * 1000L)
        return dataBuffer.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * Obtiene el último dato registrado
     */
    fun getLastData(): SensorData? {
        return dataBuffer.lastOrNull()
    }
    
    /**
     * Limpia todos los datos
     */
    fun clear() {
        dataBuffer.clear()
    }
    
    /**
     * Obtiene el número de datos almacenados
     */
    fun getDataCount(): Int {
        return dataBuffer.size
    }
}
