package com.korentingeorget.troccampus.ui.annonces

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.korentingeorget.troccampus.BuildConfig
import com.korentingeorget.troccampus.TrocContainer
import com.korentingeorget.troccampus.data.Annonce

fun Context.shareAnnonce(annonce: Annonce) {
    val text = buildString {
        append("[${annonce.type.uppercase()}] ${annonce.title} (${annonce.campus})\n")
        append(annonce.description)
        append("\n\nLien : https://${BuildConfig.PWA_HOST}/?ad=${annonce.id}")
    }

    val intent = if (!annonce.photoPath.isNullOrBlank()) {
        val photoUri = TrocContainer.photoStore.uriFor(annonce.photoPath)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    startActivity(Intent.createChooser(intent, "Partager cette annonce via..."))
}

fun Context.openPwa() {
    val url = Uri.parse("https://${BuildConfig.PWA_HOST}/")
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER).apply {
        data = url
    }
    startActivity(intent)
}

fun Context.contactStudent(contact: String, title: String) {
    if (contact.contains("@")) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$contact?subject=${Uri.encode("Troc'Campus : $title")}")
        }
        startActivity(Intent.createChooser(emailIntent, "Contacter par email"))
    } else {
        val telIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$contact")
        }
        startActivity(telIntent)
    }
}
