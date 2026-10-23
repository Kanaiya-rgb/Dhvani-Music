package com.music.dhvani

import com.music.dhvani.data.artist.ArtistSignatureRepository
import com.music.dhvani.data.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ArtistSignatureTest {

    @Before
    fun setUp() {
        ArtistSignatureRepository.clearCache()
        AppSettings.artistSignaturesEnabled.value = true
        AppSettings.customSignatureApiUrl.value = ""
    }

    @Test
    fun curatedArtistReturnsValidSvg() = runBlocking {
        val sig = ArtistSignatureRepository.getSignature("Taylor Swift")
        assertNotNull(sig)
        assert(sig!!.endsWith(".svg"))
    }

    @Test
    fun arRahmanReturnsVerifiedUrl() = runBlocking {
        val sig = ArtistSignatureRepository.getSignature("A. R. Rahman")
        assertNotNull(sig)
        assertEquals("https://upload.wikimedia.org/wikipedia/en/1/1b/A_R_Rahman_signature.svg", sig)
    }

    @Test
    fun blankOrUnknownArtistDoesNotThrowNpe() = runBlocking {
        val blank = ArtistSignatureRepository.getSignature("   ")
        assertNull(blank)

        // Random non-existent artist should not throw ConcurrentHashMap NPE
        val unknown = ArtistSignatureRepository.getSignature("XYZ_NON_EXISTENT_ARTIST_99999")
        assertNull(unknown)

        // Query again to check cached sentinel branch
        val cachedUnknown = ArtistSignatureRepository.getSignature("XYZ_NON_EXISTENT_ARTIST_99999")
        assertNull(cachedUnknown)
    }

    @Test
    fun disabledSettingReturnsNull() = runBlocking {
        AppSettings.artistSignaturesEnabled.value = false
        val sig = ArtistSignatureRepository.getSignature("Taylor Swift")
        assertNull(sig)
    }
}
