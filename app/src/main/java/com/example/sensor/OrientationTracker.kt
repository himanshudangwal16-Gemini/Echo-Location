package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class OrientationTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var orientationSensor: Sensor? = null // fallback

    // Outbound state: azimuth angle in degrees (0 to 360, where 0 is north, 90 east, etc.)
    var currentAzimuth: Float = 0f
        private set

    // Optional delta callback to trigger sounds when sweeping across obstacle thresholds
    var onHeadingChanged: ((Float) -> Unit)? = null

    init {
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVectorSensor == null) {
            // Deprecated but widely available fallback
            orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
            Log.w("OrientationTracker", "Rotation vector sensor not found, using orientation.")
        }
    }

    fun startListening() {
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var heading = 0f
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            
            // Azimuth/yaw is at index 0. Convert from radians to absolute degrees
            heading = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            // Map from -180..180 to 0..360
            if (heading < 0) {
                heading += 360f
            }
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            // Yaw value is directly at index 0 in degrees
            heading = event.values[0]
        }

        // Notify if changed
        if (Math.abs(currentAzimuth - heading) > 0.8f) {
            currentAzimuth = heading
            onHeadingChanged?.invoke(heading)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not critical for coarse mapping
    }
}
