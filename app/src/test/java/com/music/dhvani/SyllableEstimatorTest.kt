package com.music.dhvani

import com.music.dhvani.data.lyrics.SyllableEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllableEstimatorTest {

    @Test
    fun `estimateSyllables handles simple English words`() {
        assertEquals(1, SyllableEstimator.estimateSyllables("the"))
        assertEquals(1, SyllableEstimator.estimateSyllables("love"))
        assertEquals(1, SyllableEstimator.estimateSyllables("game"))
        assertEquals(2, SyllableEstimator.estimateSyllables("music"))
        assertEquals(3, SyllableEstimator.estimateSyllables("harmony"))
    }

    @Test
    fun `estimateSyllables handles Hindi and romanized words`() {
        assertEquals(1, SyllableEstimator.estimateSyllables("dil"))
        assertEquals(2, SyllableEstimator.estimateSyllables("tere"))
        assertEquals(3, SyllableEstimator.estimateSyllables("zindagi"))
        val actual = SyllableEstimator.estimateSyllables("\u0917\u093E\u0928\u093E")
        assertTrue(actual >= 2)
    }

    @Test
    fun `estimateWords distributes duration properly`() {
        val line = "Never gonna give you up"
        val startMs = 1_000L
        val endMs = 4_000L
        val words = SyllableEstimator.estimateWords(line, startMs, endMs)

        assertEquals(5, words.size)
        assertEquals("Never", words[0].text)
        assertEquals("up", words[4].text)

        // Monotonically increasing timestamps
        for (i in 0 until words.size - 1) {
            assertTrue(words[i].startMs < words[i].endMs)
            assertTrue(words[i].endMs <= words[i + 1].startMs)
        }

        assertTrue(words.first().startMs >= startMs)
        assertTrue(words.last().endMs <= endMs + 500L)
    }

    @Test
    fun `estimateWords handles empty line gracefully`() {
        val words = SyllableEstimator.estimateWords("", 1_000L, 2_000L)
        assertTrue(words.isEmpty())
    }
}
