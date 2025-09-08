# Descripción del Proyecto

ProxiTalk es una aplicación Android que implementa un sistema integrado utilizando el sensor de proximidad del dispositivo. La aplicación detecta gestos específicos (3 pasadas cercanas al sensor en 2 segundos) y responde con vibración y lectura de notificaciones mediante texto a voz.

## Características Principales

### Hardware Integrado
- **Sensor de Proximidad**: Detecta objetos cercanos al dispositivo
- **Vibrador**: Proporciona retroalimentación háptica
- **Motor TTS**: Convierte texto a voz para accesibilidad

### Funcionalidades
- **Detección de Gestos**: Algoritmo que identifica 3 pasadas cercanas en 2 segundos
- **Visualización en Tiempo Real**: Gráfico que muestra los datos del sensor
- **Modo Offline**: Funciona completamente sin conexión a internet
- **Registro de Datos**: Almacena los últimos 30 segundos de datos del sensor

## Arquitectura del Sistema

### Componentes Principales

1. **MainActivity**: Actividad principal que coordina todos los componentes
2. **ProximitySensorManager**: Gestiona el sensor y la lógica de detección de gestos
3. **DataLogger**: Almacena y gestiona los datos del sensor
4. **ProximityGraphView**: Vista personalizada para visualizar datos en tiempo real

### Flujo de Datos

`
Sensor de Proximidad  ProximitySensorManager  DataLogger
                                    
                            Detección de Gestos
                                    
                    Vibración + TTS + Actualización UI
`

## Instalación y Uso

### Requisitos
- Android 6.0 (API 23) o superior
- Dispositivo con sensor de proximidad
- Permisos de vibración

### Instrucciones de Uso

1. **Iniciar la Aplicación**: Abre ProxiTalk desde el launcher
2. **Verificar Sensor**: La aplicación verificará automáticamente la disponibilidad del sensor
3. **Iniciar Monitoreo**: Presiona el botón "Iniciar" para comenzar el monitoreo
4. **Realizar Gesto**: Acerca tu mano al sensor 3 veces en 2 segundos
5. **Respuesta del Sistema**: 
   - Vibración de confirmación
   - Lectura de notificaciones simuladas
   - Actualización del estado en pantalla

### Interpretación del Gráfico

- **Línea Verde**: Distancia medida por el sensor
- **Puntos Rojos**: Momentos cuando el objeto está "cerca"
- **Puntos Verdes**: Momentos cuando el objeto está "lejos"
- **Eje Y**: Distancia (0 = cerca, máximo = lejos)
- **Eje X**: Tiempo (últimos 30 segundos)

## Aspectos Técnicos

### Acoplamiento Hardware-Software

El proyecto demuestra claramente el acoplamiento entre hardware y software:

1. **Entrada Hardware**: Sensor de proximidad detecta cambios físicos
2. **Procesamiento Software**: Algoritmo interpreta los datos y detecta patrones
3. **Salida Hardware**: Vibrador y TTS responden a las decisiones del software

### Algoritmo de Detección de Gestos

`kotlin
// Detectar transiciones de lejos a cerca
if (!lastIsNear && isNear) {
    handleProximityEvent()
}

// Contar eventos en ventana de tiempo
gestureEvents.add(currentTime)
gestureEvents.removeAll { it < currentTime - gestureTimeWindow }

// Verificar gesto completo
if (gestureEvents.size >= requiredGestures) {
    detectGesture()
}
`

### Gestión de Recursos

- **Ciclo de Vida**: Registro/desregistro del sensor en onResume/onPause
- **Memoria**: Buffer circular limitado a 30 segundos de datos
- **Rendimiento**: Actualización de UI cada 100ms

## Permisos Requeridos

`xml
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-feature android:name="android.hardware.sensor.proximity" android:required="true" />
`

## Logs y Debugging

La aplicación incluye logging extensivo para debugging:

- MainActivity: Logs de ciclo de vida y gestión de UI
- ProximitySensorManager: Logs de eventos del sensor y detección de gestos
- DataLogger: Logs de gestión de datos

## Consideraciones de Accesibilidad

- **Retroalimentación Háptica**: Vibración para usuarios con discapacidad visual
- **Audio**: TTS para información audible
- **Interfaz Visual**: Colores de alto contraste y texto legible

## Limitaciones y Mejoras Futuras

### Limitaciones Actuales
- Notificaciones simuladas (no integración real con NotificationListenerService)
- Rango fijo de detección (dependiente del hardware)
- Interfaz básica

### Posibles Mejoras
- Integración real con notificaciones del sistema
- Personalización de gestos y tiempos
- Múltiples idiomas para TTS
- Interfaz más avanzada con configuración

## Conclusión

ProxiTalk es un ejemplo completo de sistema integrado que demuestra:

1. **Integración Hardware-Software**: Uso directo de sensores y actuadores
2. **Procesamiento en Tiempo Real**: Algoritmos que responden inmediatamente
3. **Funcionalidad Aplicada**: Solución práctica para accesibilidad
4. **Arquitectura Limpia**: Código modular y mantenible

El proyecto cumple con todos los requisitos del taller, mostrando un acoplamiento efectivo entre hardware y software en un sistema Android integrado.
