package com.example.projekat.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages geofences for task location-based notifications.
 * Handles adding, updating, and removing geofences via GeofencingClient.
 * Also includes active location monitoring for better emulator support.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    // Track active geofences for manual checking
    private val activeGeofences = mutableMapOf<String, GeofenceData>()
    private var locationCallback: LocationCallback? = null
    private var isMonitoring = false
    
    data class GeofenceData(
        val taskId: String,
        val lat: Double,
        val lng: Double,
        val radiusMeters: Int,
        var hasTriggered: Boolean = false
    )
    
    companion object {
        private const val TAG = "GeofenceManager"
        const val GEOFENCE_REQUEST_CODE = 1001
        const val ACTION_GEOFENCE_EVENT = "com.example.projekat.ACTION_GEOFENCE_EVENT"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Checks if all required location permissions are granted.
     */
    fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation && coarseLocation
    }

    /**
     * Checks if background location permission is granted (Android 10+).
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed on older versions
        }
    }

    /**
     * Adds a geofence for a task location.
     * @param taskId Unique ID for the task (used as geofence request ID)
     * @param taskTitle Title of the task for notification
     * @param lat Latitude of the geofence center
     * @param lng Longitude of the geofence center
     * @param radiusMeters Radius of the geofence in meters
     */
    fun addGeofenceForTask(
        taskId: String,
        taskTitle: String,
        lat: Double,
        lng: Double,
        radiusMeters: Int = 100
    ) {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted, cannot add geofence")
            return
        }

        if (!hasBackgroundLocationPermission()) {
            Log.w(TAG, "Background location permission not granted, geofence may not work in background")
        }

        // Store for manual checking (helps with emulator)
        activeGeofences[taskId] = GeofenceData(taskId, lat, lng, radiusMeters)
        
        // Use a larger minimum radius for better detection
        val effectiveRadius = maxOf(radiusMeters, 100).toFloat()

        val geofence = Geofence.Builder()
            .setRequestId(taskId)
            .setCircularRegion(lat, lng, effectiveRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(5000) // 5 seconds dwell time (reduced from 30s)
            .setNotificationResponsiveness(5000) // Respond within 5 seconds
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added for task: $taskId at ($lat, $lng) with radius ${effectiveRadius}m")
                    // Start active monitoring if not already running
                    startActiveMonitoring()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add geofence for task: $taskId - ${e.message}", e)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when adding geofence", e)
        }
    }

    /**
     * Removes a geofence for a task.
     * @param taskId The task ID (geofence request ID) to remove
     */
    fun removeGeofenceForTask(taskId: String) {
        activeGeofences.remove(taskId)
        
        geofencingClient.removeGeofences(listOf(taskId))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed for task: $taskId")
                // Stop monitoring if no more geofences
                if (activeGeofences.isEmpty()) {
                    stopActiveMonitoring()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove geofence for task: $taskId", e)
            }
    }

    /**
     * Updates the geofence for a task. Removes old geofence and adds new one.
     */
    fun updateGeofenceForTask(
        taskId: String,
        taskTitle: String,
        lat: Double,
        lng: Double,
        radiusMeters: Int = 100
    ) {
        // Reset triggered state for updated geofence
        activeGeofences[taskId]?.hasTriggered = false
        
        // Remove old geofence first, then add new one
        geofencingClient.removeGeofences(listOf(taskId))
            .addOnCompleteListener {
                addGeofenceForTask(taskId, taskTitle, lat, lng, radiusMeters)
            }
    }

    /**
     * Removes all geofences created by this app.
     */
    fun removeAllGeofences() {
        activeGeofences.clear()
        stopActiveMonitoring()
        
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove all geofences", e)
            }
    }
    
    /**
     * Start active location monitoring for manual geofence checking.
     * This helps on emulators where standard geofencing may not work well.
     */
    private fun startActiveMonitoring() {
        if (isMonitoring || !hasLocationPermissions()) return
        
        Log.d(TAG, "Starting active location monitoring for ${activeGeofences.size} geofences")
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                checkGeofencesManually(location)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isMonitoring = true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when starting location updates", e)
        }
    }
    
    /**
     * Stop active location monitoring.
     */
    private fun stopActiveMonitoring() {
        if (!isMonitoring) return
        
        Log.d(TAG, "Stopping active location monitoring")
        
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isMonitoring = false
    }
    
    /**
     * Manually check if current location is within any geofence.
     * Sends broadcast to trigger notification if inside.
     */
    private fun checkGeofencesManually(currentLocation: Location) {
        for ((taskId, geofenceData) in activeGeofences) {
            if (geofenceData.hasTriggered) continue
            
            val geofenceLocation = Location("geofence").apply {
                latitude = geofenceData.lat
                longitude = geofenceData.lng
            }
            
            val distance = currentLocation.distanceTo(geofenceLocation)
            
            Log.d(TAG, "Distance to geofence $taskId: ${distance}m (radius: ${geofenceData.radiusMeters}m)")
            
            if (distance <= geofenceData.radiusMeters) {
                Log.d(TAG, "Manual geofence trigger for task: $taskId")
                geofenceData.hasTriggered = true
                
                // Send broadcast to trigger notification
                val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
                    action = ACTION_GEOFENCE_EVENT
                    putExtra("manual_trigger", true)
                    putExtra("task_id", taskId)
                }
                context.sendBroadcast(intent)
            }
        }
    }
    
    /**
     * Reset triggered state for a geofence (e.g., when user leaves and re-enters).
     */
    fun resetGeofenceTrigger(taskId: String) {
        activeGeofences[taskId]?.hasTriggered = false
    }
}
