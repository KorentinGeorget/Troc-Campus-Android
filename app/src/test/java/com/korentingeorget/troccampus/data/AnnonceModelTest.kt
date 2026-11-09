package com.korentingeorget.troccampus.data

import com.korentingeorget.troccampus.data.local.AnnonceEntity
import com.korentingeorget.troccampus.data.local.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnonceModelTest {

    @Test
    fun formattedPrice_displaysCorrectlyForEveryType() {
        val don = Annonce(title = "Livre", type = "don", campus = "Mariani", contact = "test@univ.fr", description = "Desc")
        assertEquals("Gratuit", don.formattedPrice)
        assertTrue(don.isFree)

        val troc = Annonce(title = "Chaise", type = "troc", campus = "Grimaldi", contact = "test@univ.fr", description = "Desc")
        assertEquals("Échange", troc.formattedPrice)
        assertTrue(troc.isFree)

        val vente = Annonce(title = "Calculatrice", type = "vente", price = 35, campus = "Mariani", contact = "test@univ.fr", description = "Desc")
        assertEquals("35 €", vente.formattedPrice)
        assertFalse(vente.isFree)
    }

    @Test
    fun entityMapping_preservesAllFields() {
        val annonce = Annonce(
            id = 42,
            title = "Bureau CROUS",
            type = "troc",
            price = 0,
            campus = "Résidence Grossetti, Corte",
            contact = "lea.grossetti@univ-corse.fr",
            description = "Échange bureau",
            createdAt = 1700000000L,
            pinned = true,
            synced = true,
            lat = 42.302,
            lng = 9.153,
            accuracy = 8.5f,
            photoPath = "/data/data/photos/ad_1.jpg",
            isSeed = true,
        )

        val entity = annonce.toEntity()
        assertEquals(42L, entity.id)
        assertTrue(entity.pinned)
        assertTrue(entity.synced)
        assertEquals(42.302, entity.lat ?: 0.0, 0.0001)
        assertEquals(9.153, entity.lng ?: 0.0, 0.0001)
        assertEquals(8.5f, entity.accuracy ?: 0.0f, 0.001f)
        assertEquals("/data/data/photos/ad_1.jpg", entity.photoPath)
        assertTrue(entity.isSeed)

        val back = entity.toAnnonce()
        assertEquals(annonce, back)
    }
}
