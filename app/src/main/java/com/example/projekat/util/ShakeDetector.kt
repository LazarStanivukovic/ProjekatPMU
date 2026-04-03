package com.example.projekat.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Utility class for detecting device shake gestures using the accelerometer.
 * 
 * Uses a time-windowed approach: detects rapid acceleration changes over
 * a short period rather than just checking absolute values.
 * 
 * Usage:
 * 1. Create instance with context and callback
 * 2. Call start() when screen becomes visible (onResume)
 * 3. Call stop() when screen becomes invisible (onPause)
 * 
 * @param context Android context for accessing SensorManager
 * @param onShake Callback invoked when shake is detected
 * @param shakeThreshold Acceleration change threshold in m/s² (default 3.0)
 * @param cooldownMs Minimum time between shake detections in ms (default 1000)
 */
class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
    private val shakeThreshold: Float = 3.0f,
    private val cooldownMs: Long = 1000L
) : SensorEventListener {

    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = 
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var lastShakeTime: Long = 0
    private var isRunning: Boolean = false
    
    // Previous acceleration values for detecting changes
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var lastUpdateTime: Long = 0
    
    // Shake detection requires multiple rapid movements
    private var shakeCount: Int = 0
    private var firstShakeTime: Long = 0
    
    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_WINDOW_MS = 500L  // Time window to count shakes
        private const val REQUIRED_SHAKES = 2      // Number of shakes needed in window
        private const val MIN_TIME_BETWEEN_SAMPLES_MS = 50L
    }

    /**
     * Start listening for shake events.
     * Call this in onResume or when the composable becomes active.
     */
    fun start() {
        if (isRunning || accelerometer == null) {
            Log.d(TAG, "Cannot start: isRunning=$isRunning, accelerometer=$accelerometer")
            return
        }
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        isRunning = true
        Log.d(TAG, "ShakeDetector started")
    }

    /**
     * Stop listening for shake events.
     * Call this in onPause or when the composable becomes inactive.
     */
    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        Log.d(TAG, "ShakeDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()
        
        // Throttle updates
        if (currentTime - lastUpdateTime < MIN_TIME_BETWEEN_SAMPLES_MS) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Calculate acceleration change (delta)
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ
        
        lastX = x
        lastY = y
        lastZ = z
        
        // Skip first reading (no previous values)
        if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
            return
        }
        
        lastUpdateTime = currentTime
        
        // Calculate magnitude of acceleration change
        val deltaAcceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
        
        // Check if this is a significant movement
        if (deltaAcceleration > shakeThreshold) {
            Log.d(TAG, "Movement detected: delta=$deltaAcceleration")
            
            // Check if we're within the shake window
            if (currentTime - firstShakeTime > SHAKE_WINDOW_MS) {
                // Start new shake window
                shakeCount = 1
                firstShakeTime = currentTime
            } else {
                // Increment shake count within window
                shakeCount++
            }
            
            // Check if we have enough shakes and cooldown passed
            if (shakeCount >= REQUIRED_SHAKES && currentTime - lastShakeTime > cooldownMs) {
                Log.d(TAG, "SHAKE DETECTED! shakeCount=$shakeCount")
                lastShakeTime = currentTime
                shakeCount = 0
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }
}
