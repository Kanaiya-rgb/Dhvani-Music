package com.music.flux

import com.music.flux.data.lyrics.LyricsCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsCleanerTest {

    @Test
    fun `cleans artist dash title when uploaded by record label channel`() {
        // When channel is a record label, head is artist, tail is title
        val title1 = LyricsCleaner.cleanTitle("Arijit Singh - O Maahi (Official Audio)", "Zee Music Company")
        assertEquals("O Maahi", title1)

        val title2 = LyricsCleaner.cleanTitle("Diljit Dosanjh - Lover", "Speed Records")
        assertEquals("Lover", title2)

        val title3 = LyricsCleaner.cleanTitle("Alan Walker - Faded", "NCS")
        assertEquals("Faded", title3)
    }

    @Test
    fun `keeps head when tail is movie name or soundtrack`() {
        val title1 = LyricsCleaner.cleanTitle("Kesariya - From Brahmastra", "Pritam")
        assertEquals("Kesariya", title1)

        val title2 = LyricsCleaner.cleanTitle("Tauba Tauba (From \"Bad Newz\")", "Karan Aujla")
        assertEquals("Tauba Tauba", title2)
    }

    @Test
    fun `isTitleMatch handles matching accurately`() {
        assertTrue(LyricsCleaner.isTitleMatch("O Maahi", "O Maahi"))
        assertTrue(LyricsCleaner.isTitleMatch("Kesariya (Audio)", "Kesariya"))
        assertTrue(LyricsCleaner.isTitleMatch("Despacito", "Despacito (Remix)"))
    }
}
