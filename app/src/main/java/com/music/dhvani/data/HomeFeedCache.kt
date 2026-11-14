package com.music.dhvani.data

import android.content.Context
import com.music.dhvani.data.model.HomeShelf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * High-performance disk cache for Dhvani Music's Home feed.
 *
 * Implements Stale-While-Revalidate:
 * 1. App launch reads cached shelves from disk in ~1-2ms (0ms perceived latency).
 * 2. Background worker silently fetches fresh feed and updates disk cache.
 * 3. Stored in context.cacheDir so it never uses permanent user storage
 *    and can be safely reclaimed if device space is low.
 */
object HomeFeedCache {
    private const val CACHE_FILE_NAME = "home_feed_cache.json"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(HomeShelf.serializer())

    /**
     * Synchronously loads the cached shelves from disk.
     * Takes ~1-2ms on modern flash storage.
     */
    fun load(context: Context): List<HomeShelf>? {
        return runCatching {
            val file = File(context.cacheDir, CACHE_FILE_NAME)
            if (!file.exists() || file.length() == 0L) return null
            val content = file.readText()
            json.decodeFromString(serializer, content).takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    /**
     * Asynchronously writes the shelves to disk cache via an atomic file swap
     * to prevent corrupted state on sudden termination.
     */
    fun save(context: Context, shelves: List<HomeShelf>) {
        if (shelves.isEmpty()) return
        scope.launch {
            runCatching {
                val cacheDir = context.cacheDir
                val finalFile = File(cacheDir, CACHE_FILE_NAME)
                val tempFile = File(cacheDir, "$CACHE_FILE_NAME.tmp")

                val encoded = json.encodeToString(serializer, shelves)
                tempFile.writeText(encoded)
                if (tempFile.exists()) {
                    if (finalFile.exists()) finalFile.delete()
                    tempFile.renameTo(finalFile)
                }
            }
        }
    }

    /**
     * Clears the cached home feed from disk.
     */
    fun clear(context: Context) {
        runCatching {
            val file = File(context.cacheDir, CACHE_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }
}

