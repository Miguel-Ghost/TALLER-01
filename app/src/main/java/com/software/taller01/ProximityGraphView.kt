package com.software.taller01

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Vista personalizada para mostrar un gráfico en tiempo real de los datos del sensor de proximidad
 */
class ProximityGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var sensorData = mutableListOf<DataLogger.SensorData>()
    private var maxDataPoints = 100
    private var maxRange = 5f


    private val backgroundColor = Color.BLACK
    private val gridColor = Color.GRAY
    private val lineColor = Color.GREEN
    private val nearColor = Color.RED
    private val textColor = Color.WHITE

    init {
        setupPaints()
    }

    private fun setupPaints() {
        try {
            // Pintura de fondo
            backgroundPaint.color = backgroundColor
            backgroundPaint.style = Paint.Style.FILL

            // Pintura de la cuadrícula
            gridPaint.color = gridColor
            gridPaint.strokeWidth = 1f
            gridPaint.style = Paint.Style.STROKE

            // Pintura de la línea principal
            paint.color = lineColor
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND

            // Pintura del texto
            textPaint.color = textColor
            textPaint.textSize = 32f
            textPaint.textAlign = Paint.Align.LEFT
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al configurar pinturas: ${e.message}")
        }
    }

    /**
     * Actualiza los datos del sensor
     */
    fun updateData(newData: List<DataLogger.SensorData>) {
        try {
            sensorData.clear()
            sensorData.addAll(newData.takeLast(maxDataPoints))
            invalidate()
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al actualizar datos: ${e.message}")
        }
    }

    /**
     * Establece el rango máximo del sensor
     */
    fun setMaxRange(range: Float) {
        try {
            maxRange = range
            invalidate()
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al establecer rango máximo: ${e.message}")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        try {
            val width = width.toFloat()
            val height = height.toFloat()

            if (width <= 0 || height <= 0) return

            // Dibujar fondo
            canvas.drawRect(0f, 0f, width, height, backgroundPaint)

            // Dibujar cuadrícula
            drawGrid(canvas, width, height)

            // Dibujar datos del sensor
            if (sensorData.isNotEmpty()) {
                drawSensorData(canvas, width, height)
            }

            // Dibujar información
            drawInfo(canvas, width, height)
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error en onDraw: ${e.message}")
        }
    }

    private fun drawGrid(canvas: Canvas, width: Float, height: Float) {
        try {
            val gridSpacing = 50f

            // Líneas verticales
            var x = 0f
            while (x <= width) {
                canvas.drawLine(x, 0f, x, height, gridPaint)
                x += gridSpacing
            }

            // Líneas horizontales
            var y = 0f
            while (y <= height) {
                canvas.drawLine(0f, y, width, y, gridPaint)
                y += gridSpacing
            }
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al dibujar cuadrícula: ${e.message}")
        }
    }

    private fun drawSensorData(canvas: Canvas, width: Float, height: Float) {
        try {
            if (sensorData.size < 2) return

            val path = Path()
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            pointPaint.style = Paint.Style.FILL

            val dataWidth = width - 100f // Margen para texto
            val dataHeight = height - 100f // Margen para texto
            val stepX = dataWidth / (maxDataPoints - 1)

            var firstPoint = true

            for (i in sensorData.indices) {
                val data = sensorData[i]
                val x = 50f + (i * stepX)
                val normalizedDistance = (data.distance / maxRange).coerceIn(0f, 1f)
                val y = 50f + (normalizedDistance * dataHeight)

                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }

                // Dibujar punto
                pointPaint.color = if (data.isNear) nearColor else lineColor
                canvas.drawCircle(x, y, 4f, pointPaint)
            }

            // Dibujar línea
            canvas.drawPath(path, paint)
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al dibujar datos del sensor: ${e.message}")
        }
    }

    private fun drawInfo(canvas: Canvas, width: Float, height: Float) {
        try {
            val lastData = sensorData.lastOrNull()

            if (lastData != null) {
                val infoText = "Distancia: ${String.format("%.1f", lastData.distance)} cm"
                val statusText = if (lastData.isNear) "CERCA" else "LEJOS"
                val dataCountText = "Datos: ${sensorData.size}"

                canvas.drawText(infoText, 20f, 40f, textPaint)
                canvas.drawText(statusText, 20f, 80f, textPaint)
                canvas.drawText(dataCountText, width - 200f, 40f, textPaint)
            }

            // Título
            textPaint.textSize = 24f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Sensor de Proximidad - Tiempo Real", width / 2f, height - 20f, textPaint)

            // Restaurar alineación
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.textSize = 32f
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error al dibujar información: ${e.message}")
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        try {
            val desiredWidth = 800
            val desiredHeight = 400

            val width = resolveSize(desiredWidth, widthMeasureSpec)
            val height = resolveSize(desiredHeight, heightMeasureSpec)

            setMeasuredDimension(width, height)
        } catch (e: Exception) {
            android.util.Log.e("ProximityGraphView", "Error en onMeasure: ${e.message}")
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
