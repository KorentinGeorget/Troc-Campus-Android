package com.korentingeorget.troccampus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.korentingeorget.troccampus.notif.Notifications

class SimulatedPushWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "🎁 Nouveau don sur Campus Mariani !"
        val body = inputData.getString("body") ?: "Livre Réseaux & Télécoms disponible immédiatement au bâtiment pédagogique."
        val adId = inputData.getLong("adId", 1L)

        Notifications.showCampusAlert(
            context = applicationContext,
            title = title,
            body = body,
            adId = adId,
        )
        return Result.success()
    }
}
