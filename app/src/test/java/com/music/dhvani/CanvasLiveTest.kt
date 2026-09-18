package com.music.dhvani

import com.music.dhvani.data.canvas.AppleMusicCanvas
import com.music.dhvani.data.canvas.CommunityCanvas
import com.music.dhvani.data.canvas.TidalCanvas
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class CanvasLiveTest {

    @Test
    fun testRealSongLookups() = runBlocking {
        println("=== Testing Flowers ===")
        val f1 = AppleMusicCanvas.fetch("Flowers", "Miley Cyrus", "Endless Summer Vacation")
        println("Flowers Apple: $f1")
        val f2 = TidalCanvas.fetch("Flowers", "Miley Cyrus", "Endless Summer Vacation")
        println("Flowers Tidal: $f2")
        val f3 = CommunityCanvas.fetch("Flowers", "Miley Cyrus", "Endless Summer Vacation")
        println("Flowers Community: $f3")

        println("=== Testing Espresso ===")
        val e1 = AppleMusicCanvas.fetch("Espresso", "Sabrina Carpenter", "Short n' Sweet")
        println("Espresso Apple: $e1")
        val e2 = TidalCanvas.fetch("Espresso", "Sabrina Carpenter", "Short n' Sweet")
        println("Espresso Tidal: $e2")

        println("=== Testing Cruel Summer ===")
        val c1 = AppleMusicCanvas.fetch("Cruel Summer", "Taylor Swift", "Lover")
        println("Cruel Summer Apple: $c1")
        val c2 = TidalCanvas.fetch("Cruel Summer", "Taylor Swift", "Lover")
        println("Cruel Summer Tidal: $c2")

        println("=== Testing Musixmatch ===")
        val tokenUrl = "https://apic.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0"
        println("Testing token.get...")
        val mxm = com.music.dhvani.data.lyrics.Musixmatch.lyrics("Flowers", "Miley Cyrus", 200000L)
        println("Musixmatch Flowers: ${mxm?.size} lines")

        println("=== Testing Spotify Canvas ===")
        val spResult = com.music.dhvani.data.canvas.SpotifyCanvas.fetch("Espresso", "Sabrina Carpenter")
        println("Spotify Espresso Canvas: $spResult")
    }
}
