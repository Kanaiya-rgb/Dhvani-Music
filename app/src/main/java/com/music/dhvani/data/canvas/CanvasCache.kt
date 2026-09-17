package com.music.dhvani.data.canvas

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.music.dhvani.data.Http
import java.io.File

/**
 * Media3 LRU disk cache bounded to 150MB so video chunks and HLS segments are
 * cached locally. Looping videos play directly from disk without consuming
 * repeated network bandwidth.
 */
@OptIn(UnstableApi::class)
object CanvasCache {
    private const val MAX_CACHE_BYTES = 150L * 1024 * 1024 // 150 MB

    @Volatile
    private var simpleCache: SimpleCache? = null
    private val lock = Any()

    fun init(context: Context) {
        getCache(context)
    }

    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(lock) {
            simpleCache ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, "canvas")
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = OkHttpDataSource.Factory(Http.client)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
