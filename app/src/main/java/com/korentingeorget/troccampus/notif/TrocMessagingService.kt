package com.korentingeorget.troccampus.notif

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.korentingeorget.troccampus.TrocContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrocMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TrocCampus", "Nouveau jeton FCM : $token")
        scope.launch {
            TrocContainer.settings.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("TrocCampus", "Message FCM reçu. Data: ${message.data}")

        val title = message.data["title"]
            ?: message.notification?.title
            ?: "Troc'Campus"
        val body = message.data["body"]
            ?: message.notification?.body
            ?: "Une nouvelle annonce est disponible sur votre campus !"
        val adId = message.data["adId"]?.toLongOrNull()

        Notifications.showCampusAlert(this, title, body, adId)
    }
}
