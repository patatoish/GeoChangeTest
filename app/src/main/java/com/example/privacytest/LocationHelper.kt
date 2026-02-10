package com.example.privacytest

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _locationState = MutableStateFlow<LocationState>(LocationState())
    val locationState = _locationState.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                updateLocation(location)
            }
        }
    }

    data class LocationState(
        val realLocation: Location? = null,
        val mockLocation: Location? = null,
        val isMockProviderActive: Boolean = false
    )

    private fun updateLocation(location: Location) {
        val current = _locationState.value
        if (location.isFromMockProvider) {
            _locationState.value = current.copy(mockLocation = location, isMockProviderActive = true)
        } else {
            _locationState.value = current.copy(realLocation = location)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1000)
                .build()
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error starting updates", e)
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Mock Provider Logic
    fun enableMockProvider() {
        // Attempts to set this app as mock provider
        // This requires 'ALLOW_MOCK_LOCATION' in Developer Options to be selecting THIS app manually.
        // We can try to setMockMode to true. If it fails, user hasn't selected us.
        try {
            fusedLocationClient.setMockMode(true)
                .addOnSuccessListener { 
                     Log.d("LocationHelper", "Mock mode enabled")
                     // Start pushing mock data
                     pushMockLocation()
                }
                .addOnFailureListener {
                    Log.e("LocationHelper", "Failed to enable mock mode: ${it.message}")
                }
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "SecurityException enabling mock mode - App not selected as provider?", e)
        }
    }
    
    fun disableMockProvider() {
        try {
            fusedLocationClient.setMockMode(false)
        } catch (e: Exception) {
             Log.e("LocationHelper", "Error disabling mock mode", e)
        }
    }

    private fun pushMockLocation() {
        // Push a static mock location (e.g. Null Island or arbitrary)
        // In a real app we'd use a loop. For this util, we push one then user sees it. 
        // Or we should run a coroutine to push periodically.
        // For simplicity:
        val mockLoc = Location("MockProvider")
        mockLoc.latitude = 48.8566 // Paris
        mockLoc.longitude = 2.3522
        mockLoc.altitude = 35.0
        mockLoc.accuracy = 5.0f
        mockLoc.time = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLoc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        
        try {
            fusedLocationClient.setMockLocation(mockLoc)
        } catch (e: Exception) {
            Log.e("LocationHelper", "Failed to push mock location", e)
        }
    }
}
