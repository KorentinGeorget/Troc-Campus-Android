package com.korentingeorget.troccampus

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleEventObserver
import com.korentingeorget.troccampus.ui.TrocNavigation
import com.korentingeorget.troccampus.ui.annonces.TrocViewModel
import com.korentingeorget.troccampus.ui.theme.TrocCampusTheme

private const val TAG = "TrocCampus"

class MainActivity : ComponentActivity() {

    private val viewModel: TrocViewModel by viewModels { TrocViewModel.Factory }
    private var openedAdId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        Log.d(
            TAG,
            "onCreate — instance ${System.identityHashCode(this)}, état restauré : ${savedInstanceState != null}",
        )

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                Log.d(TAG, "Activity ${event.name} — instance ${System.identityHashCode(this)}")
            },
        )

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            TrocCampusTheme {
                TrocNavigation(initialAdId = openedAdId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        Log.d(TAG, "handleIntent: action=${intent.action}, data=${intent.data}")

        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    Log.d(TAG, "ACTION_SEND reçu (texte): $text")
                    viewModel.receiveSharedText(text)
                }
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                imageUri?.let { uri ->
                    Log.d(TAG, "ACTION_SEND reçu (image): $uri")
                    viewModel.receiveSharedImage(uri)
                }
            }

            Intent.ACTION_VIEW -> {
                val data = intent.data
                Log.d(TAG, "ACTION_VIEW reçu (App Link) : $data")
                if (data != null) {
                    val adIdParam = data.getQueryParameter("ad")?.toLongOrNull()
                        ?: data.lastPathSegment?.toLongOrNull()
                    if (adIdParam != null) {
                        openedAdId = adIdParam
                        Log.d(TAG, "App Link ciblant l'annonce #$adIdParam")
                    }
                }
            }
        }

        if (intent.hasExtra("adId")) {
            val id = intent.getLongExtra("adId", -1L)
            if (id > 0) {
                openedAdId = id
                Log.d(TAG, "Notification Intent ciblant l'annonce #$id")
            }
        }
    }
}
