package com.music.dhvani

import com.music.dhvani.data.lyrics.BiniLyrics
import com.music.dhvani.data.lyrics.LrcRed
import com.music.dhvani.data.lyrics.LyricsCleaner
import com.music.dhvani.data.lyrics.LyricsRepository
import com.music.dhvani.data.lyrics.LyricsSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GehraHuaVerificationTest {

    @Test
    fun testCleanerStripsDhurandharNoise() {
        val rawTitle = "Gehra Hua (From \"Dhurandhar\")"
        val rawArtist = "Shashwat Sachdev"
        val cleanTitle = LyricsCleaner.cleanTitle(rawTitle, rawArtist)
        val cleanArtist = LyricsCleaner.cleanArtist(rawArtist)

        org.junit.Assert.assertEquals("Gehra Hua", cleanTitle)
        org.junit.Assert.assertEquals("Shashwat Sachdev", cleanArtist)
    }

    @Test
    fun testLrcRedDirectForGehraHua() = runBlocking {
        val rawTitle = "Gehra Hua (From \"Dhurandhar\")"
        val rawArtist = "Shashwat Sachdev"
        val durationMs = 362_000L

        val lines = LrcRed.lyrics(rawTitle, rawArtist, durationMs)
        assertNotNull("LrcRed should return lyrics for Gehra Hua", lines)
        assertTrue("Lyrics should not be empty", lines!!.isNotEmpty())
        assertTrue("Should have word/syllable sync", lines.any { it.isWordSynced })
    }

    @Test
    fun testBiniLyricsForGehraHua() = runBlocking {
        val rawTitle = "Gehra Hua (From \"Dhurandhar\")"
        val rawArtist = "Shashwat Sachdev"
        val durationMs = 362_000L

        val hit = BiniLyrics.identify(rawTitle, rawArtist, durationMs)
        assertNotNull("BiniLyrics should identify Gehra Hua", hit)
        org.junit.Assert.assertEquals("INH102506955", hit!!.isrc)

        val match = BiniLyrics.lyricsFor(hit)
        assertNotNull("BiniLyrics should fetch TTML match", match)
        assertTrue("Match should have lines", match!!.lines.isNotEmpty())
    }

    @Test
    fun testProbeAvailableSourcesForGehraHua() = runBlocking {
        val rawTitle = "Gehra Hua (From \"Dhurandhar\")"
        val rawArtist = "Shashwat Sachdev"
        val durationMs = 362_000L

        val probed = LyricsRepository.probeAvailableSources(
            title = rawTitle,
            artist = rawArtist,
            durationMs = durationMs,
            videoId = "gehra_test_id"
        )

        println("Probed sources result: $probed")
        assertTrue("LrcRed should be available", probed[LyricsSource.LRC_RED] == true)
        assertTrue("BiniLyrics (am-lyrics) should be available", probed[LyricsSource.BINI_LYRICS] == true)
    }
}

