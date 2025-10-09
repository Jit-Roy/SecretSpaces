package com.secretspaces32.android.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        if (!isLocationEnabled()) {
            return null
        }

        return try {
            // Add timeout of 10 seconds to prevent hanging
            withTimeout(10000L) {
                try {
                    // First try to get fresh location
                    val cancellationToken = CancellationTokenSource()
                    var location = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationToken.token
                    ).await()

                    // If getCurrentLocation returns null, try lastLocation immediately
                    if (location == null) {
                        location = fusedLocationClient.lastLocation.await()
                    }

                    // Return location even if it's not perfectly accurate
                    // Filter out only obviously wrong locations
                    if (location != null && isReasonableLocation(location)) {
                        location
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Try last known location as fallback
                    try {
                        fusedLocationClient.lastLocation.await()
                    } catch (e2: Exception) {
                        null
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Timeout - try to get last location
            try {
                fusedLocationClient.lastLocation.await()
            } catch (e2: Exception) {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isLocationTooOld(location: Location): Boolean {
        val maxAge = 5 * 60 * 1000 // 5 minutes
        return System.currentTimeMillis() - location.time > maxAge
    }

    private fun isReasonableLocation(location: Location): Boolean {
        // Basic sanity checks for location
        return location.latitude >= -90 && location.latitude <= 90 &&
                location.longitude >= -180 && location.longitude <= 180 &&
                location.accuracy <= 5000f // Allow up to 5km accuracy
    }

    private fun isValidLocation(location: Location): Boolean {
        // Check if location is not the default Google HQ coordinates
        val googleHQLat = 37.422
        val googleHQLng = -122.084

        val distanceFromGoogleHQ = calculateDistance(
            location.latitude, location.longitude,
            googleHQLat, googleHQLng
        )

        // If within 1km of Google HQ and accuracy is poor, it's likely a default location
        if (distanceFromGoogleHQ < 1000 && location.accuracy > 100) {
            return false
        }

        // Check for reasonable accuracy (less than 500 meters)
        return location.accuracy <= 500f
    }

    companion object {
        fun calculateDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0].toDouble()
        }

        fun formatDistance(distanceInMeters: Double): String {
            return when {
                distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
                else -> "${"%.1f".format(distanceInMeters / 1000)}km"
            }
        }

        fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> "${days / 7}w ago"
            }
        }
    }
}
