package com.korentingeorget.troccampus.ui.annonces

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.korentingeorget.troccampus.TrocContainer
import com.korentingeorget.troccampus.data.Annonce
import com.korentingeorget.troccampus.data.AnnoncesRepository
import com.korentingeorget.troccampus.work.SimulatedPushWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrocViewModel(
    private val savedState: SavedStateHandle,
    private val repository: AnnoncesRepository,
) : ViewModel() {

    // Filtres
    val filterType: StateFlow<String> = savedState.getStateFlow("filter_type", "all")

    fun setFilter(type: String) {
        savedState["filter_type"] = type
    }

    // Liste des annonces filtrée
    val annonces: StateFlow<List<Annonce>> = repository.annonces
        .combine(filterType) { list, filter ->
            if (filter == "all") list else list.filter { it.type == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // État du formulaire dans SavedStateHandle (survit à la rotation & am kill)
    val draftTitle = savedState.getStateFlow("draft_title", "")
    val draftType = savedState.getStateFlow("draft_type", "don")
    val draftPrice = savedState.getStateFlow("draft_price", "")
    val draftCampus = savedState.getStateFlow("draft_campus", "Campus Mariani, Corte")
    val draftContact = savedState.getStateFlow("draft_contact", "")
    val draftDesc = savedState.getStateFlow("draft_desc", "")
    val draftPhotoPath = savedState.getStateFlow<String?>("draft_photo", null)
    val draftLat = savedState.getStateFlow<Double?>("draft_lat", null)
    val draftLng = savedState.getStateFlow<Double?>("draft_lng", null)
    val draftAccuracy = savedState.getStateFlow<Float?>("draft_acc", null)

    var isLocating = MutableStateFlow(false)

    fun onTitleChange(v: String) { savedState["draft_title"] = v }
    fun onTypeChange(v: String) { savedState["draft_type"] = v }
    fun onPriceChange(v: String) { savedState["draft_price"] = v }
    fun onCampusChange(v: String) { savedState["draft_campus"] = v }
    fun onContactChange(v: String) { savedState["draft_contact"] = v }
    fun onDescChange(v: String) { savedState["draft_desc"] = v }
    fun onPhotoSelected(path: String?) { savedState["draft_photo"] = path }

    fun captureLocation() {
        viewModelScope.launch {
            isLocating.value = true
            val fix = TrocContainer.locationSource.current()
            if (fix != null) {
                savedState["draft_lat"] = fix.latitude
                savedState["draft_lng"] = fix.longitude
                savedState["draft_acc"] = fix.accuracy
                Log.d("TrocCampus", "Position fixée: ${fix.latitude}, ${fix.longitude} (±${fix.accuracy}m)")
            }
            isLocating.value = false
        }
    }

    fun submitAnnonce(onSuccess: () -> Unit) {
        val title = draftTitle.value.trim()
        if (title.isBlank()) return

        val priceInt = if (draftType.value == "vente") {
            draftPrice.value.toIntOrNull() ?: 0
        } else 0

        viewModelScope.launch {
            repository.add(
                title = title,
                type = draftType.value,
                price = priceInt,
                campus = draftCampus.value,
                contact = draftContact.value.ifBlank { "etudiant@univ-corse.fr" },
                description = draftDesc.value.ifBlank { "Aucune description fournie." },
                lat = draftLat.value,
                lng = draftLng.value,
                accuracy = draftAccuracy.value,
                photoPath = draftPhotoPath.value,
            )
            resetDraft()
            onSuccess()
        }
    }

    fun togglePinned(annonce: Annonce) {
        viewModelScope.launch {
            repository.togglePinned(annonce)
        }
    }

    fun deleteAnnonce(annonce: Annonce) {
        viewModelScope.launch {
            repository.delete(annonce)
        }
    }

    fun forceSync() {
        repository.enqueueSync()
    }

    private fun resetDraft() {
        savedState["draft_title"] = ""
        savedState["draft_type"] = "don"
        savedState["draft_price"] = ""
        savedState["draft_contact"] = ""
        savedState["draft_desc"] = ""
        savedState["draft_photo"] = null
        savedState["draft_lat"] = null
        savedState["draft_lng"] = null
        savedState["draft_acc"] = null
    }

    fun receiveSharedText(text: String) {
        savedState["draft_title"] = text.take(60)
        savedState["draft_desc"] = text
    }

    fun receiveSharedImage(uri: Uri) {
        viewModelScope.launch {
            val copiedPath = TrocContainer.photoStore.copyUri(uri)
            if (copiedPath != null) {
                savedState["draft_photo"] = copiedPath
            }
        }
    }

    fun scheduleTestPush(delaySeconds: Long = 5) {
        val request = OneTimeWorkRequestBuilder<SimulatedPushWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    "title" to "🎁 Nouveau Don sur Campus Mariani !",
                    "body" to "Livre Tanenbaum disponible immédiatement au bâtiment pédagogique.",
                    "adId" to 1L,
                )
            )
            .build()
        WorkManager.getInstance().enqueue(request)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TrocViewModel(
                    savedState = createSavedStateHandle(),
                    repository = TrocContainer.repository,
                )
            }
        }
    }
}
