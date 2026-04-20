package com.example.matchmyskills.util

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Utility class for GPS-based location fetching and geocoding.
 * Handles fetching user's current location and converting coordinates to city/state names.
 */
object LocationHelper {

    private const val TAG = "LocationHelper"

    /**
     * Callback interface for location updates
     */
    interface LocationCallback {
        fun onLocationFetched(city: String, state: String)
        fun onLocationError(message: String)
    }

    /**
     * Check if fine location permission is granted
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Fetch user's last known location and convert to city/state
     * @param context Context for accessing location services
     * @param callback Callback to receive location or error
     */
    fun fetchLocation(context: Context, callback: LocationCallback) {
        // Check permission first
        if (!isLocationPermissionGranted(context)) {
            Log.w(TAG, "Location permission not granted")
            callback.onLocationError("Location permission denied")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Location found: ${location.latitude}, ${location.longitude}")
                        val lat = location.latitude
                        val lng = location.longitude
                        convertLocationToAddress(context, lat, lng, callback)
                    } else {
                        Log.w(TAG, "Last known location is null, trying getLastLocation")
                        // Fallback to getLastLocation
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { fallbackLocation ->
                                if (fallbackLocation != null) {
                                    convertLocationToAddress(
                                        context,
                                        fallbackLocation.latitude,
                                        fallbackLocation.longitude,
                                        callback
                                    )
                                } else {
                                    callback.onLocationError("Location unavailable")
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Failed to get fallback location", exception)
                                callback.onLocationError("Failed to get location")
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get current location", exception)
                    callback.onLocationError("Failed to get location: ${exception.message}")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception in fetchLocation", e)
            callback.onLocationError("Location permission denied")
        }
    }

    /**
     * Convert latitude/longitude to city and state names using Geocoder
     * @param context Context for Geocoder
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @param callback Callback to receive city/state or error
     */
    private fun convertLocationToAddress(
        context: Context,
        latitude: Double,
        longitude: Double,
        callback: LocationCallback
    ) {
        val geocoder = Geocoder(context, Locale.getDefault())

        try {
            // Use coroutines-friendly getFromLocation (API 33+) if available, otherwise use deprecated version
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    handleAddresses(addresses, callback)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                handleAddresses(addresses, callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoder error", e)
            callback.onLocationError("Unable to determine location name")
        }
    }

    /**
     * Helper function to handle addresses returned by Geocoder
     */
    private fun handleAddresses(addresses: List<Address>?, callback: LocationCallback) {
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.locality ?: "Unknown City"
            val state = address.adminArea ?: "Unknown State"
            Log.d(TAG, "Address found: $city, $state")
            callback.onLocationFetched(city, state)
        } else {
            Log.w(TAG, "No addresses found for coordinates")
            callback.onLocationError("Location not found")
        }
    }

    /**
     * Get location permissions required for this feature
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
