package com.music.dhvani

import com.music.dhvani.data.canvas.CanvasArtwork
import com.music.dhvani.data.canvas.CanvasSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtworkTest {

    @Test
    fun `normalizes diacritics and punctuation cleanly`() {
        val raw = "Café del Mar (Official Video) [Remastered]! - 2024"
        val normalized = CanvasArtwork.normalize(raw)
        assertEquals("cafe del mar 2024", normalized)
    }

    @Test
    fun `matches artist and title ignoring accents and case`() {
        val artwork = CanvasArtwork(
            url = "https://example.com/video.mp4",
            title = "Détour",
            artist = "Beyoncé",
            album = "Renaissance",
            source = CanvasSource.APPLE_MUSIC,
        )

        assertTrue(artwork.matches("Detour", "Beyonce"))
        assertTrue(artwork.matches("détour", "beyoncé"))
        assertTrue(artwork.matches("Detour (Visualizer)", "Beyoncé feat. Jay-Z"))
    }

    @Test
    fun `rejects mismatched titles or artists`() {
        val artwork = CanvasArtwork(
            url = "https://example.com/video.mp4",
            title = "Flowers",
            artist = "Miley Cyrus",
            source = CanvasSource.COMMUNITY,
        )

        assertFalse(artwork.matches("Dracula", "Miley Cyrus"))
        assertFalse(artwork.matches("Flowers", "Taylor Swift"))
        assertFalse(artwork.matches("", ""))
    }

    @Test
    fun `tidal uuid path resolves properly`() {
        val uuid = "12345678-1234-1234-1234-123456789abc"
        val path = uuid.replace("-", "/")
        val expected = "12345678/1234/1234/1234/123456789abc"
        assertEquals(expected, path)
        val url = "https://resources.tidal.com/videos/$path/1280x1280.mp4"
        assertEquals("https://resources.tidal.com/videos/12345678/1234/1234/1234/123456789abc/1280x1280.mp4", url)
    }
}
