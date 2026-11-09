package com.korentingeorget.troccampus.ui.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Coordonnées par défaut des campus de Corte / Corse
fun getCampusCoordinates(campusName: String): Pair<Double, Double> {
    return when {
        campusName.contains("Mariani", ignoreCase = true) -> Pair(42.3012, 9.1541)
        campusName.contains("Grimaldi", ignoreCase = true) -> Pair(42.3025, 9.1508)
        campusName.contains("Grossetti", ignoreCase = true) -> Pair(42.2989, 9.1485)
        campusName.contains("IUT", ignoreCase = true) -> Pair(42.3005, 9.1530)
        campusName.contains("Bastia", ignoreCase = true) -> Pair(42.6973, 9.4509)
        campusName.contains("Ajaccio", ignoreCase = true) -> Pair(41.9271, 8.7369)
        else -> Pair(42.3012, 9.1541) // Défaut Mariani
    }
}

@Composable
fun CampusCompassCard(
    campusName: String,
    userLat: Double?,
    userLng: Double?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val (targetLat, targetLng) = remember(campusName) { getCampusCoordinates(campusName) }

    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    hasGeomagnetic = true
                }

                if (hasGravity && hasGeomagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        azimuth = (azDeg + 360) % 360
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        if (magnetometer != null) {
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Calcul de la distance et du relèvement vers le campus
    val (distanceMeters, bearingToCampus) = remember(userLat, userLng, targetLat, targetLng) {
        if (userLat != null && userLng != null) {
            val results = FloatArray(2)
            Location.distanceBetween(userLat, userLng, targetLat, targetLng, results)
            Pair(results[0].roundToInt(), results[1])
        } else {
            Pair(null, 0f)
        }
    }

    val targetAngle = ((bearingToCampus - azimuth + 360) % 360)
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 200),
        label = "compassAngle",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                    )
                    Text(
                        "Boussole Campus (Capteur Magnétique)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    "${azimuth.roundToInt()}°",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cadran de la boussole
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
            ) {
                // Flèche de la boussole orientée vers le lieu de remise
                Canvas(
                    modifier = Modifier
                        .size(90.dp)
                        .rotate(animatedAngle),
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h / 2

                    // Aiguille Nord / Cible (Émeraude / Rouge)
                    val needleNorth = Path().apply {
                        moveTo(cx, cy - h * 0.42f)
                        lineTo(cx - 10f, cy)
                        lineTo(cx + 10f, cy)
                        close()
                    }
                    drawPath(needleNorth, Color(0xFF10B981))

                    // Aiguille Sud (Gris)
                    val needleSouth = Path().apply {
                        moveTo(cx, cy + h * 0.42f)
                        lineTo(cx - 10f, cy)
                        lineTo(cx + 10f, cy)
                        close()
                    }
                    drawPath(needleSouth, Color.LightGray.copy(alpha = 0.5f))

                    // Pivot central
                    drawCircle(Color.White, radius = 6f, center = Offset(cx, cy))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Direction : $campusName",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (distanceMeters != null) {
                Text(
                    text = "Distance estimée : $distanceMeters m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Activez la localisation pour calculer la distance exacte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
