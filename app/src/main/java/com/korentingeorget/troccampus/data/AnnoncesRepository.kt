package com.korentingeorget.troccampus.data

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.korentingeorget.troccampus.data.local.AnnonceDao
import com.korentingeorget.troccampus.data.local.AnnonceEntity
import com.korentingeorget.troccampus.data.local.PhotoStore
import com.korentingeorget.troccampus.data.local.SettingsStore
import com.korentingeorget.troccampus.data.local.toEntity
import com.korentingeorget.troccampus.data.remote.AnnonceDto
import com.korentingeorget.troccampus.data.remote.SyncApi
import com.korentingeorget.troccampus.work.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AnnoncesRepository(
    private val dao: AnnonceDao,
    private val settings: SettingsStore,
    private val photoStore: PhotoStore,
    private val syncApi: SyncApi,
    private val httpbinApi: SyncApi,
    private val workManager: WorkManager,
) {
    val annonces: Flow<List<Annonce>> = dao.all().map { list -> list.map { it.toAnnonce() } }
    val pendingCount: Flow<Int> = dao.unsyncedCount()

    suspend fun ensureSeeded() {
        val alreadySeeded = settings.seeded.first()
        if (alreadySeeded) return

        val count = dao.count().first()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val seeds = listOf(
                AnnonceEntity(
                    title = "Livre Réseaux & Télécoms (Tanenbaum 5e éd.)",
                    type = "don",
                    price = 0,
                    campus = "Campus Grimaldi, Corte",
                    contact = "contact-grimaldi@univ-corse.fr",
                    description = "Manuel en très bon état avec schémas, idéal pour étudiant en Licence ou Master Informatique. À récupérer au bâtiment pédagogique.",
                    createdAt = now - 7200000,
                    pinned = true,
                    synced = true,
                    lat = 42.3025,
                    lng = 9.1508,
                    accuracy = 5.0f,
                    isSeed = true,
                ),
                AnnonceEntity(
                    title = "Calculatrice graphique TI-84 Plus CE",
                    type = "vente",
                    price = 35,
                    campus = "Campus Mariani, Corte",
                    contact = "mathieu.mariani@univ-corse.fr",
                    description = "Batterie rechargeable neuve, avec câble USB et coque de protection. Parfait pour les examens et modules de maths/physique.",
                    createdAt = now - 3600000,
                    pinned = false,
                    synced = true,
                    lat = 42.3012,
                    lng = 9.1541,
                    accuracy = 8.0f,
                    isSeed = true,
                ),
                AnnonceEntity(
                    title = "Bureau compact pliable pour chambre CROUS",
                    type = "troc",
                    price = 0,
                    campus = "Résidence Grossetti, Corte",
                    contact = "lea.grossetti@univ-corse.fr",
                    description = "Échange bureau compact en bois clair contre chaise de bureau ergonomique ou petite étagère de rangement.",
                    createdAt = now - 1800000,
                    pinned = false,
                    synced = true,
                    lat = 42.2989,
                    lng = 9.1485,
                    accuracy = 12.0f,
                    isSeed = true,
                ),
            )
            for (seed in seeds) {
                dao.upsert(seed)
            }
        }
        settings.markSeeded()
    }

    suspend fun get(id: Long): Annonce? = dao.get(id)?.toAnnonce()

    suspend fun add(
        title: String,
        type: String,
        price: Int,
        campus: String,
        contact: String,
        description: String,
        lat: Double? = null,
        lng: Double? = null,
        accuracy: Float? = null,
        photoPath: String? = null,
    ): Long {
        val entity = AnnonceEntity(
            title = title,
            type = type,
            price = price,
            campus = campus,
            contact = contact,
            description = description,
            createdAt = System.currentTimeMillis(),
            pinned = false,
            synced = false,
            lat = lat,
            lng = lng,
            accuracy = accuracy,
            photoPath = photoPath,
            isSeed = false,
        )
        val id = dao.upsert(entity)
        enqueueSync()
        return id
    }

    suspend fun togglePinned(annonce: Annonce) {
        dao.upsert(annonce.copy(pinned = !annonce.pinned).toEntity())
    }

    suspend fun delete(annonce: Annonce) {
        if (!annonce.photoPath.isNullOrBlank()) {
            photoStore.delete(annonce.photoPath)
        }
        dao.delete(annonce.toEntity())
    }

    fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("troc_sync", ExistingWorkPolicy.KEEP, request)
    }

    suspend fun syncPending(): Int {
        val unsyncedList = dao.unsynced()
        if (unsyncedList.isEmpty()) return 0

        val syncedIds = mutableListOf<Long>()
        for (item in unsyncedList) {
            val dto = AnnonceDto(
                id = item.id,
                title = item.title,
                type = item.type,
                price = item.price,
                campus = item.campus,
                contact = item.contact,
                description = item.description,
                createdAt = item.createdAt,
                lat = item.lat,
                lng = item.lng,
                hasPhoto = !item.photoPath.isNullOrBlank(),
            )
            try {
                // Essayer d'abord l'endpoint Coolify, sinon repli sur httpbin
                try {
                    syncApi.pushToCoolify(dto)
                } catch (e: Exception) {
                    Log.d("TrocCampus", "Repli HTTP vers httpbin: ${e.message}")
                    httpbinApi.pushToHttpbin(dto)
                }
                syncedIds.add(item.id)
            } catch (e: Exception) {
                Log.w("TrocCampus", "Erreur réseau syncPending sur annonce #${item.id}: ${e.message}")
                break // Réseau instable, on s'arrête là pour que WorkManager réessaie
            }
        }

        if (syncedIds.isNotEmpty()) {
            dao.markSynced(syncedIds)
        }
        return syncedIds.size
    }
}
