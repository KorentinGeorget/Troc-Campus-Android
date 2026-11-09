package com.korentingeorget.troccampus.ui.annonces

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korentingeorget.troccampus.TrocContainer
import com.korentingeorget.troccampus.data.Annonce
import com.korentingeorget.troccampus.ui.compass.CampusCompassCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnonceDetailScreen(
    adId: Long,
    onBack: () -> Unit,
    onShare: (Annonce) -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var annonce by remember { mutableStateOf<Annonce?>(null) }

    LaunchedEffect(adId) {
        annonce = TrocContainer.repository.get(adId)
    }

    val item = annonce
    if (item == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chargement...") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Annonce introuvable ou en cours de chargement...")
            }
        }
        return
    }

    val bitmap = remember(item.photoPath) {
        item.photoPath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val updated = item.copy(pinned = !item.pinned)
                            annonce = updated
                            coroutineScope.launch {
                                TrocContainer.repository.togglePinned(item)
                            }
                        },
                    ) {
                        Icon(
                            if (item.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (item.pinned) "Désépingler" else "Épingler",
                            tint = if (item.pinned) Color(0xFF10B981) else Color.White,
                        )
                    }

                    IconButton(onClick = { onShare(item) }) {
                        Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White)
                    }

                    if (!item.isSeed) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    TrocContainer.repository.delete(item)
                                }
                                Toast.makeText(context, "Annonce supprimée", Toast.LENGTH_SHORT).show()
                                onDeleted()
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF059669),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Photo principale ou Bannière d'illustration
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (item.type) {
                                "don" -> Color(0xFF059669).copy(alpha = 0.2f)
                                "vente" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (item.type) {
                            "don" -> "🎁"
                            "vente" -> "💶"
                            else -> "🔄"
                        },
                        fontSize = 56.sp,
                    )
                }
            }

            // Titre, Type et Prix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = when (item.type) {
                        "don" -> Color(0xFF059669)
                        "vente" -> Color(0xFF3B82F6)
                        else -> Color(0xFFD97706)
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = item.type.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                Text(
                    text = item.formattedPrice,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isFree) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            // Lieu du campus
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.campus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (item.accuracy != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(GPS ±${item.accuracy.toInt()}m)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                }
            }

            // Description
            Text(
                text = "Description de l'annonce",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Boussole Campus Temps Réel
            CampusCompassCard(
                campusName = item.campus,
                userLat = item.lat,
                userLng = item.lng,
            )

            // Coordonnées et Contact
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Contact de l'étudiant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.contact,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = { context.contactStudent(item.contact, item.title) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Contacter")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(item.contact))
                                Toast.makeText(context, "Coordonnées copiées !", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
