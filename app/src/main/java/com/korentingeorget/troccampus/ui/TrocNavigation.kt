package com.korentingeorget.troccampus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.korentingeorget.troccampus.ui.annonces.AnnonceDetailScreen
import com.korentingeorget.troccampus.ui.annonces.AnnoncesListScreen
import com.korentingeorget.troccampus.ui.annonces.openPwa
import com.korentingeorget.troccampus.ui.annonces.shareAnnonce
import kotlinx.serialization.Serializable

@Serializable
data object AnnoncesList : NavKey

@Serializable
data class AnnonceDetailKey(val id: Long) : NavKey

@Composable
fun TrocNavigation(
    initialAdId: Long? = null,
) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(AnnoncesList)

    LaunchedEffect(initialAdId) {
        if (initialAdId != null && backStack.none { it is AnnonceDetailKey && it.id == initialAdId }) {
            backStack.add(AnnonceDetailKey(initialAdId))
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AnnoncesList> {
                AnnoncesListScreen(
                    onAnnonceClick = { adId ->
                        backStack.add(AnnonceDetailKey(adId))
                    },
                    onOpenPwa = { context.openPwa() },
                    onShare = { annonce -> context.shareAnnonce(annonce) },
                )
            }
            entry<AnnonceDetailKey> { key ->
                AnnonceDetailScreen(
                    adId = key.id,
                    onBack = { backStack.removeLastOrNull() },
                    onShare = { annonce -> context.shareAnnonce(annonce) },
                    onDeleted = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
