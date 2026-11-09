package com.korentingeorget.troccampus.ui.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.korentingeorget.troccampus.TrocContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private val JUNK_LABELS = setOf(
    "font", "rectangle", "material property", "parallel", "circle",
    "screenshot", "line", "pattern", "symmetry", "square", "slope",
    "electric blue", "office supplies", "publication", "paper product",
    "brand", "room", "wood", "flooring", "floor", "ceiling", "table",
    "furniture", "desk", "indoor", "interior design", "space", "shade",
    "tint", "grey", "black", "white", "flash photography", "handwriting",
    "design", "metal", "plastic", "composite material", "cylinder", "triangle",
    "hardware", "accessory", "electronic device", "technology", "display device"
)

@OptIn(ExperimentalGetImage::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraMlScannerDialog(
    onDismiss: () -> Unit,
    onCaptureResult: (photoPath: String, detectedTitle: String?, detectedLabels: List<String>) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isCapturing by remember { mutableStateOf(false) }
    var detectedTextLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var detectedLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val imageLabeler = remember { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            textRecognizer.close()
            imageLabeler.close()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Viewfinder CameraX
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )

                                    // Analyse OCR : texte lisible (titre de livre, marque, descriptif)
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val lines = visionText.textBlocks
                                                .flatMap { it.lines }
                                                .map { it.text.trim() }
                                                .filter { line ->
                                                    line.length in 4..55 &&
                                                    line.any { it.isLetter() } &&
                                                    !line.matches(Regex("^[0-9\\W_]+$")) &&
                                                    !line.equals("troc'campus", ignoreCase = true)
                                                }
                                                .take(3)
                                            if (lines.isNotEmpty()) {
                                                detectedTextLines = lines
                                            }
                                        }

                                    // Analyse Objets / Étiquettes concrètes (seuil haut 78% + filtre anti-parasites)
                                    imageLabeler.process(inputImage)
                                        .addOnSuccessListener { labels ->
                                            val topLabels = labels
                                                .filter { label ->
                                                    label.confidence >= 0.78f &&
                                                    label.text.lowercase().trim() !in JUNK_LABELS &&
                                                    label.text.length > 2
                                                }
                                                .map { it.text.replaceFirstChar { c -> c.uppercase() } }
                                                .take(3)
                                            if (topLabels.isNotEmpty()) {
                                                detectedLabels = topLabels
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    imageAnalysis,
                                )
                            } catch (exc: Exception) {
                                Log.e("TrocCampus", "Erreur CameraX: ${exc.message}")
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Header avec bouton fermer et badge IA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF059669).copy(alpha = 0.85f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Scanner IA Embarqué (ML Kit)", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Overlay bas : éléments détectés en direct et bouton capture
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (detectedTextLines.isNotEmpty() || detectedLabels.isNotEmpty()) {
                        Text(
                            text = "✨ Suggestions détectées (touchez pour choisir) :",
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            // Textes OCR en priorité
                            detectedTextLines.forEach { textLine ->
                                FilterChip(
                                    selected = selectedLabel == textLine,
                                    onClick = { selectedLabel = if (selectedLabel == textLine) null else textLine },
                                    label = { Text("📄 $textLine") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF059669),
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                            // Objets concrets ML Kit
                            detectedLabels.forEach { label ->
                                FilterChip(
                                    selected = selectedLabel == label,
                                    onClick = { selectedLabel = if (selectedLabel == label) null else label },
                                    label = { Text("🏷️ $label") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF059669),
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text(
                            text = "📸 Cadrez un manuel, un cours ou un matériel...",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Bouton de déclenchement capture photo
                    Button(
                        onClick = {
                            if (isCapturing) return@Button
                            isCapturing = true

                            val photoFile = File(
                                context.filesDir,
                                "photos/ad_${System.currentTimeMillis()}.jpg",
                            ).apply { parentFile?.mkdirs() }

                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        val chosenTitle = selectedLabel
                                            ?: detectedTextLines.firstOrNull()
                                            ?: detectedLabels.firstOrNull()

                                        onCaptureResult(
                                            photoFile.absolutePath,
                                            chosenTitle,
                                            detectedLabels,
                                        )
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        Log.e("TrocCampus", "Erreur capture photo: ${exception.message}")
                                    }
                                },
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        } else {
                            Icon(
                                Icons.Default.Camera,
                                contentDescription = "Prendre la photo et valider le scan",
                                modifier = Modifier.size(36.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Prendre la photo & Appliquer l'IA",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
