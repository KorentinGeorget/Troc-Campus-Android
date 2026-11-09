package com.korentingeorget.troccampus.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.korentingeorget.troccampus.data.Annonce

@Entity(tableName = "annonces")
data class AnnonceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val price: Int,
    val campus: String,
    val contact: String,
    val description: String,
    val createdAt: Long,
    val isSeed: Boolean = false,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Float? = null,
    val photoPath: String? = null,
) {
    fun toAnnonce(): Annonce = Annonce(
        id = id,
        title = title,
        type = type,
        price = price,
        campus = campus,
        contact = contact,
        description = description,
        createdAt = createdAt,
        pinned = pinned,
        synced = synced,
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        photoPath = photoPath,
        isSeed = isSeed,
    )
}

fun Annonce.toEntity(): AnnonceEntity = AnnonceEntity(
    id = id,
    title = title,
    type = type,
    price = price,
    campus = campus,
    contact = contact,
    description = description,
    createdAt = createdAt,
    pinned = pinned,
    synced = synced,
    lat = lat,
    lng = lng,
    accuracy = accuracy,
    photoPath = photoPath,
    isSeed = isSeed,
)
