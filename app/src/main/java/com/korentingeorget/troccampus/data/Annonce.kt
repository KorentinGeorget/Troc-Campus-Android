package com.korentingeorget.troccampus.data

data class Annonce(
    val id: Long = 0,
    val title: String,
    val type: String,
    val price: Int = 0,
    val campus: String,
    val contact: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val synced: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Float? = null,
    val photoPath: String? = null,
    val isSeed: Boolean = false,
) {
    val isFree: Boolean get() = type == "don" || price == 0
    val formattedPrice: String
        get() = when (type) {
            "don" -> "Gratuit"
            "troc" -> "Échange"
            else -> "$price €"
        }
}
