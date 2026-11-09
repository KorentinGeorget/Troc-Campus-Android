package com.korentingeorget.troccampus

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import com.korentingeorget.troccampus.data.AnnoncesRepository
import com.korentingeorget.troccampus.data.local.PhotoStore
import com.korentingeorget.troccampus.data.local.SettingsStore
import com.korentingeorget.troccampus.data.local.TrocDatabase
import com.korentingeorget.troccampus.data.location.LocationSource
import com.korentingeorget.troccampus.data.remote.Network
import com.korentingeorget.troccampus.notif.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TrocContainer {
    lateinit var database: TrocDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var photoStore: PhotoStore
        private set
    lateinit var repository: AnnoncesRepository
        private set
    lateinit var locationSource: LocationSource
        private set

    fun init(context: Context) {
        database = TrocDatabase.build(context)
        settings = SettingsStore(context)
        photoStore = PhotoStore(context)
        locationSource = LocationSource(context)

        val okHttpClient = Network.okHttp(context)
        val syncApi = Network.syncApi(okHttpClient)
        val httpbinApi = Network.httpbinApi(okHttpClient)
        val workManager = WorkManager.getInstance(context)

        repository = AnnoncesRepository(
            dao = database.annonceDao(),
            settings = settings,
            photoStore = photoStore,
            syncApi = syncApi,
            httpbinApi = httpbinApi,
            workManager = workManager,
        )
    }
}

class TrocApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TrocContainer.init(this)
        Notifications.createChannels(this)

        CoroutineScope(Dispatchers.IO).launch {
            TrocContainer.repository.ensureSeeded()
        }
    }
}
