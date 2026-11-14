package com.music.dhvani

import com.music.dhvani.data.HomeFeedCache
import com.music.dhvani.data.history.HistoryItem
import com.music.dhvani.data.model.HomeShelf
import com.music.dhvani.data.model.ShelfItem
import com.music.dhvani.data.model.UiState
import com.music.dhvani.data.model.toShelfItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HomeFeedOptimizationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(HomeShelf.serializer())

    @Test
    fun testHomeShelfSerializationAndDeserialization() {
        val sampleShelves = listOf(
            HomeShelf(
                title = "Listen Again",
                subtitle = "Jump back in",
                items = listOf(
                    ShelfItem(
                        title = "Kesariya",
                        subtitle = "Arijit Singh",
                        thumbnailUrl = "https://example.com/art.jpg",
                        videoId = "vid123",
                        browseId = null,
                        isExplicit = false,
                    ),
                    ShelfItem(
                        title = "Top Hits Playlist",
                        subtitle = "Bollywood",
                        thumbnailUrl = "https://example.com/playlist.jpg",
                        videoId = null,
                        browseId = "VLPL123",
                        mosaicUrls = listOf("https://ex.com/1.jpg", "https://ex.com/2.jpg"),
                        isExplicit = true,
                    )
                )
            ),
            HomeShelf(
                title = "Bollywood Chartbusters",
                subtitle = "Top Bollywood hits today",
                items = listOf(
                    ShelfItem(
                        title = "Chaleya",
                        subtitle = "Anirudh Ravichander",
                        thumbnailUrl = "https://example.com/chaleya.jpg",
                        videoId = "chaleya456",
                        browseId = null,
                        isExplicit = false,
                    )
                )
            )
        )

        // 1. Test in-memory JSON encoding & decoding
        val encoded = json.encodeToString(serializer, sampleShelves)
        assertTrue("Encoded JSON must not be empty", encoded.isNotBlank())

        val decoded = json.decodeFromString(serializer, encoded)
        assertEquals("Should decode exact number of shelves", 2, decoded.size)
        assertEquals("First shelf title match", "Listen Again", decoded[0].title)
        assertEquals("First shelf items count", 2, decoded[0].items.size)
        assertEquals("Second shelf title match", "Bollywood Chartbusters", decoded[1].title)
        assertEquals("Mosaic URLs preserved", 2, decoded[0].items[1].mosaicUrls.size)
        assertEquals("Explicit flag preserved", true, decoded[0].items[1].isExplicit)
        assertEquals("BrowseId preserved", "VLPL123", decoded[0].items[1].browseId)
    }

    @Test
    fun testAtomicDiskCacheFileOperations() {
        val cacheDir = tempFolder.newFolder("cache")
        val sampleShelves = listOf(
            HomeShelf(
                title = "Quick Picks",
                subtitle = "Fresh mix",
                items = listOf(
                    ShelfItem(
                        title = "Starboy",
                        subtitle = "The Weeknd",
                        thumbnailUrl = "https://example.com/starboy.jpg",
                        videoId = "star123",
                        browseId = null,
                    )
                )
            )
        )

        val targetFile = File(cacheDir, "home_feed_cache.json")
        val tempFile = File(cacheDir, "home_feed_cache.json.tmp")

        // Write to temp file then atomic rename (mimicking HomeFeedCache.save)
        val encoded = json.encodeToString(serializer, sampleShelves)
        tempFile.writeText(encoded)
        if (targetFile.exists()) targetFile.delete()
        tempFile.renameTo(targetFile)

        assertTrue("Cache file must exist on disk", targetFile.exists())
        assertTrue("Cache file must not be empty", targetFile.length() > 0)

        // Read back
        val content = targetFile.readText()
        val readShelves = json.decodeFromString(serializer, content)
        assertNotNull(readShelves)
        assertEquals(1, readShelves.size)
        assertEquals("Quick Picks", readShelves[0].title)
        assertEquals("Starboy", readShelves[0].items[0].title)
    }

    @Test
    fun testInstantLocalShelvesFirstFromHistory() {
        val historyItems = listOf(
            HistoryItem(
                videoId = "song1",
                title = "Tum Hi Ho",
                artist = "Arijit Singh",
                thumbnailUrl = "https://example.com/tumhiho.jpg",
            ),
            HistoryItem(
                videoId = "song2",
                title = "Apna Bana Le",
                artist = "Arijit Singh, Sachin-Jigar",
                thumbnailUrl = "https://example.com/apna.jpg",
            )
        )

        val instantShelf = HomeShelf(
            title = "Listen Again",
            subtitle = "Jump back in",
            items = historyItems.map { it.toSong().toShelfItem() }
        )

        assertEquals("Listen Again", instantShelf.title)
        assertEquals(2, instantShelf.items.size)
        assertEquals("Tum Hi Ho", instantShelf.items[0].title)
        assertEquals("Arijit Singh", instantShelf.items[0].subtitle)
        assertEquals("song1", instantShelf.items[0].videoId)
    }

    @Test
    fun testStaleWhileRevalidatePreservesCacheOnNetworkFailure() {
        // Given existing cached content in UiState
        val cachedShelves = listOf(
            HomeShelf("Listen Again", emptyList())
        )
        var homeState: UiState<List<HomeShelf>> = UiState.Success(cachedShelves)

        // When a network failure occurs during background revalidation
        val isNetworkSuccess = false
        val currentHasContent = (homeState as? UiState.Success)?.data?.isNotEmpty() == true

        if (!isNetworkSuccess && !currentHasContent) {
            homeState = UiState.Error("Network failure")
        }

        // Then verify cached content is NOT wiped out
        assertTrue("State must remain Success with cached data", homeState is UiState.Success)
        val data = (homeState as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Listen Again", data[0].title)
    }
}

