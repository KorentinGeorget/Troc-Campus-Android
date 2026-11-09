package com.korentingeorget.troccampus.ui.annonces

import android.Manifest
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.korentingeorget.troccampus.data.Annonce
import com.korentingeorget.troccampus.ui.permissions.PermissionState
import com.korentingeorget.troccampus.ui.permissions.rememberPermissionFlow
import com.korentingeorget.troccampus.ui.scanner.CameraMlScannerDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnnoncesListScreen(
    viewModel: TrocViewModel = viewModel(factory = TrocViewModel.Factory),
    onAnnonceClick: (Long) -> Unit,
    onOpenPwa: () -> Unit,
    onShare: (Annonce) -> Unit,
) {
    val context = LocalContext.current
    val annonces by viewModel.annonces.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()

    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var showNotifDialog by rememberSaveable { mutableStateOf(false) }
    var showScannerDialog by rememberSaveable { mutableStateOf(false) }

    // Permissions Caméra et Localisation
    val cameraPermission = rememberPermissionFlow(Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionFlow(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        requireAll = false,
    )
    val notifPermission = rememberPermissionFlow(Manifest.permission.POST_NOTIFICATIONS)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📦 Troc'Campus",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                },
                actions = {
                    // Badge Outbox
                    if (pendingCount > 0) {
                        Surface(
                            onClick = {
                                viewModel.forceSync()
                                Toast.makeText(context, "Synchronisation lancée...", Toast.LENGTH_SHORT).show()
                            },
                            color = Color(0xFFD97706),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    "$pendingCount en attente",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Bouton Alertes / Notifications
                    IconButton(onClick = { showNotifDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alertes Campus", tint = Color.White)
                    }

                    // Bouton Ouvrir PWA dans le navigateur
                    IconButton(onClick = onOpenPwa) {
                        Icon(Icons.Default.Language, contentDescription = "Ouvrir PWA", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF059669),
                    titleContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = Color(0xFF059669),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Déposer une annonce") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Filtres par type d'échange
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "all" to "Toutes",
                    "don" to "🎁 Dons",
                    "vente" to "💶 Ventes",
                    "troc" to "🔄 Trocs",
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filterType == key,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF059669),
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            // Liste des annonces
            if (annonces.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Aucune annonce dans cette catégorie.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp,
                    ),
                ) {
                    items(annonces, key = { it.id }) { item ->
                        AnnonceCardItem(
                            annonce = item,
                            onClick = { onAnnonceClick(item.id) },
                            onShare = { onShare(item) },
                            onTogglePin = { viewModel.togglePinned(item) },
                        )
                    }
                }
            }
        }
    }

    // Modal Déposer une annonce
    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CreateAnnonceSheetContent(
                viewModel = viewModel,
                cameraPermission = cameraPermission,
                locationPermission = locationPermission,
                onOpenScanner = { showScannerDialog = true },
                onSubmitted = {
                    showCreateSheet = false
                    Toast.makeText(context, "🎉 Annonce enregistrée hors ligne & envoyée !", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    // Scanner IA CameraX + ML Kit
    if (showScannerDialog) {
        CameraMlScannerDialog(
            onDismiss = { showScannerDialog = false },
            onCaptureResult = { photoPath, detectedTitle, _ ->
                viewModel.onPhotoSelected(photoPath)
                if (!detectedTitle.isNullOrBlank()) {
                    viewModel.onTitleChange(detectedTitle)
                }
                showScannerDialog = false
                Toast.makeText(context, "📸 Photo et analyse IA enregistrées !", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // Boîte de dialogue Notifications & Push
    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = { Text("🔔 Alertes Campus & Push FCM") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("État de la permission : ${notifPermission.state}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pour vérifier la réception d'un push quand l'application est totalement fermée :")
                    Text("Cliquez ci-dessous, puis fermez l'application par un geste dans les 5 secondes.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notifPermission.state != PermissionState.Granted) {
                            notifPermission.request()
                        }
                        viewModel.scheduleTestPush(5)
                        showNotifDialog = false
                        Toast.makeText(context, "⏳ Push programmé dans 5s ! Fermez l'app maintenant.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                ) {
                    Text("Lancer le push de test (5s)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("Fermer")
                }
            },
        )
    }
}

@Composable
fun AnnonceCardItem(
    annonce: Annonce,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val bitmap = remember(annonce.photoPath) {
        annonce.photoPath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (annonce.pinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Miniature
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (annonce.type) {
                                "don" -> Color(0xFF059669).copy(alpha = 0.15f)
                                "vente" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (annonce.type) {
                            "don" -> "📚"
                            "vente" -> "🧮"
                            else -> "🪑"
                        },
                        fontSize = 28.sp,
                    )
                }
            }

            // Détails
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = when (annonce.type) {
                            "don" -> Color(0xFF059669)
                            "vente" -> Color(0xFF3B82F6)
                            else -> Color(0xFFD97706)
                        },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = annonce.type.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = annonce.formattedPrice,
                        fontWeight = FontWeight.Bold,
                        color = if (annonce.isFree) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Text(
                    text = annonce.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )

                Text(
                    text = "📍 ${annonce.campus}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }

            // Actions rapides : Pin et Share
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (annonce.pinned) Color(0xFF059669) else Color.LightGray,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnonceSheetContent(
    viewModel: TrocViewModel,
    cameraPermission: com.korentingeorget.troccampus.ui.permissions.PermissionFlow,
    locationPermission: com.korentingeorget.troccampus.ui.permissions.PermissionFlow,
    onOpenScanner: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val draftTitle by viewModel.draftTitle.collectAsState()
    val draftType by viewModel.draftType.collectAsState()
    val draftPrice by viewModel.draftPrice.collectAsState()
    val draftCampus by viewModel.draftCampus.collectAsState()
    val draftContact by viewModel.draftContact.collectAsState()
    val draftDesc by viewModel.draftDesc.collectAsState()
    val draftPhotoPath by viewModel.draftPhotoPath.collectAsState()
    val draftLat by viewModel.draftLat.collectAsState()
    val draftAccuracy by viewModel.draftAccuracy.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()

    var campusExpanded by rememberSaveable { mutableStateOf(false) }
    val campusList = listOf(
        "Campus Mariani, Corte",
        "Campus Grimaldi, Corte",
        "IUT de Corse, Corte",
        "Résidence Grossetti, Corte",
        "Campus Bastia",
        "Campus Ajaccio",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Déposer une annonce", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Titre
        OutlinedTextField(
            value = draftTitle,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Titre de l'annonce *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Type d'échange
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("don" to "🎁 Don", "vente" to "💶 Vente", "troc" to "🔄 Troc").forEach { (type, label) ->
                FilterChip(
                    selected = draftType == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Prix (si vente)
        if (draftType == "vente") {
            OutlinedTextField(
                value = draftPrice,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Prix souhaité (€) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // Campus de remise
        ExposedDropdownMenuBox(
            expanded = campusExpanded,
            onExpandedChange = { campusExpanded = !campusExpanded },
        ) {
            OutlinedTextField(
                value = draftCampus,
                onValueChange = {},
                readOnly = true,
                label = { Text("Lieu de remise *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = campusExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = campusExpanded,
                onDismissRequest = { campusExpanded = false },
            ) {
                campusList.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            viewModel.onCampusChange(c)
                            campusExpanded = false
                        },
                    )
                }
            }
        }

        // Contact
        OutlinedTextField(
            value = draftContact,
            onValueChange = viewModel::onContactChange,
            label = { Text("Contact (email ou tél) *") },
            placeholder = { Text("etudiant@univ-corse.fr") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Description
        OutlinedTextField(
            value = draftDesc,
            onValueChange = viewModel::onDescChange,
            label = { Text("Description *") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        // Section Caméra / IA Scanner
        Text("Photo de l'objet (optionnelle)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when (cameraPermission.state) {
                        PermissionState.NotAsked -> cameraPermission.request()
                        PermissionState.DeniedOnce -> cameraPermission.request()
                        PermissionState.DeniedForever -> cameraPermission.openSettings()
                        PermissionState.Granted -> onOpenScanner()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (cameraPermission.state == PermissionState.DeniedForever) {
                        "Autoriser caméra (Réglages)"
                    } else {
                        "Scanner IA (CameraX)"
                    },
                )
            }

            if (draftPhotoPath != null) {
                OutlinedButton(onClick = { viewModel.onPhotoSelected(null) }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer")
                }
            }
        }

        if (draftPhotoPath != null) {
            val file = File(draftPhotoPath!!)
            if (file.exists()) {
                val bmp = remember(draftPhotoPath) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Aperçu photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Section Localisation GPS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = {
                    when (locationPermission.state) {
                        PermissionState.NotAsked -> locationPermission.request()
                        PermissionState.DeniedOnce -> locationPermission.request()
                        PermissionState.DeniedForever -> locationPermission.openSettings()
                        PermissionState.Granted -> viewModel.captureLocation()
                    }
                },
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (draftLat != null) {
                        "Position relevée (±${draftAccuracy?.toInt() ?: 0}m)"
                    } else {
                        "Relever ma position GPS"
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bouton Soumission
        Button(
            onClick = { viewModel.submitAnnonce(onSuccess = onSubmitted) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            enabled = draftTitle.isNotBlank(),
        ) {
            Text("Publier l'annonce", fontWeight = FontWeight.Bold)
        }
    }
}
