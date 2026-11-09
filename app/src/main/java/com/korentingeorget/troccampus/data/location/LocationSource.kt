package com.korentingeorget.troccampus.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Fix(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val precise: Boolean,
)

class LocationSource(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private fun has(perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(): Fix? {
        val fine = has(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = has(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse) return null

        val priority = if (fine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val cancelTokenSource = CancellationTokenSource()
        val loc: Location? = suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(priority, cancelTokenSource.token)
                .addOnSuccessListener { location ->
                    cont.resume(location)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }

            cont.invokeOnCancellation {
                cancelTokenSource.cancel()
            }
        }

        return loc?.let {
            Fix(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                precise = fine,
            )
        }
    }
}
