package com.music.flux

import com.music.flux.data.lyrics.plainToLyricLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainLyricsTest {

    @Test
    fun `converts plain lyrics to spaced lyric lines and filters footers`() {
        val plain = """
            Hum tere bin ab reh nahi sakte
            Tere bina kya wajood mera
            Tujhse juda gar ho jaayenge
            Toh khud se hi ho jaayenge judaa
            Source: LyricFind
        """.trimIndent()

        val lines = plainToLyricLines(plain, durationMs = 262_000L)
        assertEquals(4, lines.size)
        assertEquals("Hum tere bin ab reh nahi sakte", lines[0].text)
        assertEquals("Toh khud se hi ho jaayenge judaa", lines[3].text)

        // Verifies estimated pacing
        assertTrue(lines[0].timeMs >= 0L)
        assertTrue(lines[1].timeMs > lines[0].timeMs)
        assertTrue(lines[2].timeMs > lines[1].timeMs)
        assertTrue(lines[3].timeMs > lines[2].timeMs)
        assertTrue(lines[3].endMs <= 262_000L)

        // Verifies flag
        assertTrue(lines.all { it.isEstimatedTiming })
    }
}
