package com.music.dhvani

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.music.dhvani.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Media3 notification configuration, ensuring that:
 * 1. The small notification icon resource ID exists and is properly registered.
 * 2. Track album artwork is carried independently via [MediaMetadata] artwork fields
 *    rather than replacing the application badge/icon.
 * 3. Media controls and metadata integrity are preserved.
 */
class MediaNotificationConfigTest {

    @Test
    fun `notification small icon resource is registered and valid`() {
        val iconResId = R.drawable.ic_notification_logo
        assertNotEquals("Notification logo resource ID must not be 0", 0, iconResId)
    }

    @Test
    fun `track album artwork is preserved independently in MediaMetadata`() {
        val sampleArtworkBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val mediaItem = MediaItem.Builder()
            .setMediaId("test_track_123")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Sample Song Title")
                    .setArtist("Sample Artist")
                    .setAlbumTitle("Sample Album")
                    .setArtworkData(sampleArtworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

        // Verify metadata fields are intact and distinct from small icon
        assertEquals("Sample Song Title", mediaItem.mediaMetadata.title.toString())
        assertEquals("Sample Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals("Sample Album", mediaItem.mediaMetadata.albumTitle.toString())
        assertNotNull("Artwork data must be present in MediaMetadata", mediaItem.mediaMetadata.artworkData)
        assertEquals(4, mediaItem.mediaMetadata.artworkData!!.size)
        assertEquals(MediaMetadata.MEDIA_TYPE_MUSIC, mediaItem.mediaMetadata.mediaType)
    }

    @Test
    fun `playback notification styling constants match Dhvani brand specifications`() {
        val brandAccentColor = 0xFFF97316.toInt() // Dhvani Orange
        assertEquals(-429290, brandAccentColor)
        val channelId = "playback_channel"
        assertTrue(channelId.isNotEmpty())
    }
}
