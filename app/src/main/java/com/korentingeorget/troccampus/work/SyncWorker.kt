package com.korentingeorget.troccampus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.korentingeorget.troccampus.TrocContainer
import com.korentingeorget.troccampus.notif.Notifications
import retrofit2.HttpException
import java.io.IOException

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val count = TrocContainer.repository.syncPending()
            if (count > 0) {
                Notifications.showSyncDone(applicationContext, count)
            }
            Result.success(workDataOf("synced_count" to count))
        } catch (e: IOException) {
            Result.retry()
        } catch (e: HttpException) {
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
