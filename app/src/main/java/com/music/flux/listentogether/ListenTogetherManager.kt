package com.music.flux.listentogether

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.music.flux.data.model.Song
import com.music.flux.data.model.durationMillis
import com.music.flux.data.settings.AppSettings
import com.music.flux.playback.playSongs
import com.music.flux.playback.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Manager that bridges the Listen Together WebSocket client with the media player.
 * Synchronizes playback actions (play, pause, seek, track changes, queue) between connected users.
 */
class ListenTogetherManager private constructor(
    private val context: Context,
    val client: ListenTogetherClient,
) {
    companion object {
        private const val TAG = "ListenTogetherManager"
        private const val POSITION_TOLERANCE_MS = 2000L
        private const val PLAYBACK_POSITION_TOLERANCE_MS = 3000L

        @Volatile
        private var instance: ListenTogetherManager? = null

        fun getInstance(): ListenTogetherManager {
            return instance ?: throw IllegalStateException("ListenTogetherManager not initialized")
        }

        fun init(context: Context): ListenTogetherManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val client = ListenTogetherClient.init(context)
                    ListenTogetherManager(context.applicationContext, client).also { instance = it }
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controller: MediaController? = null
    private var eventCollectorJob: Job? = null

    // Synchronization guard to prevent feedback loops
    @Volatile
    private var isSyncing = false

    private var lastSyncedTrackId: String? = null
    private var lastSyncedIsPlaying: Boolean? = null

    // Client state delegations for UI
    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val roomState: StateFlow<RoomState?> = client.roomState
    val role: StateFlow<RoomRole> = client.role
    val userId: StateFlow<String?> = client.userId
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = client.pendingJoinRequests
    val bufferingUsers: StateFlow<List<String>> = client.bufferingUsers
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = client.pendingSuggestions
    val logs: StateFlow<List<LogEntry>> = client.logs

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost

    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            val ctrl = controller ?: return

            val currentTrackId = ctrl.currentMediaItem?.mediaId
            val position = ctrl.currentPosition

            if (playWhenReady) {
                Log.d(TAG, "Host sending PLAY at $position (track: $currentTrackId)")
                client.sendPlaybackAction(PlaybackActions.PLAY, trackId = currentTrackId, position = position)
                lastSyncedIsPlaying = true
            } else {
                Log.d(TAG, "Host sending PAUSE at $position (track: $currentTrackId)")
                client.sendPlaybackAction(PlaybackActions.PAUSE, trackId = currentTrackId, position = position)
                lastSyncedIsPlaying = false
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            if (mediaItem == null) return
            val ctrl = controller ?: return

            val trackId = mediaItem.mediaId
            if (trackId == lastSyncedTrackId) return

            lastSyncedTrackId = trackId
            lastSyncedIsPlaying = false

            val trackInfo = mediaItemToTrackInfo(mediaItem)
            Log.d(TAG, "Host sending CHANGE_TRACK: ${trackInfo.title} ($trackId)")
            client.sendPlaybackAction(
                action = PlaybackActions.CHANGE_TRACK,
                trackId = trackId,
                trackInfo = trackInfo,
                position = 0L
            )

            // If player was playing, send PLAY after short delay to let guests receive track change
            if (ctrl.playWhenReady) {
                scope.launch {
                    delay(200)
                    if (isHost && isInRoom) {
                        client.sendPlaybackAction(PlaybackActions.PLAY, trackId = trackId, position = ctrl.currentPosition)
                        lastSyncedIsPlaying = true
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isSyncing || !isHost || !isInRoom) return
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                val trackId = controller?.currentMediaItem?.mediaId
                Log.d(TAG, "Host sending SEEK to ${newPosition.positionMs} (track: $trackId)")
                client.sendPlaybackAction(
                    action = PlaybackActions.SEEK,
                    trackId = trackId,
                    position = newPosition.positionMs
                )
            }
        }
    }

    init {
        observeEvents()
    }

    private fun observeEvents() {
        eventCollectorJob?.cancel()
        eventCollectorJob = scope.launch {
            client.events.collectLatest { event ->
                handleClientEvent(event)
            }
        }
    }

    private fun handleClientEvent(event: ListenTogetherEvent) {
        when (event) {
            is ListenTogetherEvent.PlaybackSync -> {
                if (!isHost) {
                    applyGuestPlaybackAction(event.action)
                }
            }
            is ListenTogetherEvent.SyncStateReceived -> {
                if (!isHost) {
                    applyGuestSyncState(event.state)
                }
            }
            is ListenTogetherEvent.JoinApproved -> {
                if (!isHost) {
                    applyGuestRoomState(event.state)
                }
            }
            is ListenTogetherEvent.Reconnected -> {
                if (!event.isHost) {
                    applyGuestRoomState(event.state)
                }
            }
            is ListenTogetherEvent.UserLeft -> {
                Log.i(TAG, "User left: ${event.username}")
            }
            else -> Unit
        }
    }

    /**
     * Attaches the active MediaController from MainActivity.
     */
    fun setMediaController(mediaController: MediaController?) {
        if (controller == mediaController) return

        controller?.removeListener(playerListener)
        controller = mediaController

        if (mediaController != null && isInRoom && isHost) {
            mediaController.addListener(playerListener)
        }
    }

    fun onRoomJoined() {
        val ctrl = controller ?: return
        if (isHost) {
            ctrl.removeListener(playerListener)
            ctrl.addListener(playerListener)
            // Sync current host state
            ctrl.currentMediaItem?.let { item ->
                val trackInfo = mediaItemToTrackInfo(item)
                lastSyncedTrackId = item.mediaId
                lastSyncedIsPlaying = ctrl.playWhenReady
                client.sendPlaybackAction(
                    action = PlaybackActions.CHANGE_TRACK,
                    trackId = item.mediaId,
                    trackInfo = trackInfo,
                    position = ctrl.currentPosition
                )
            }
        } else {
            ctrl.removeListener(playerListener)
            client.requestSync()
        }
    }

    private fun applyGuestPlaybackAction(action: PlaybackActionPayload) {
        val ctrl = controller ?: return

        when (action.action) {
            PlaybackActions.CHANGE_TRACK -> {
                val trackInfo = action.trackInfo ?: return
                loadGuestTrack(trackInfo, playImmediately = false, startPosition = action.position ?: 0L)
            }

            PlaybackActions.PLAY -> {
                isSyncing = true
                try {
                    val targetPos = action.position ?: ctrl.currentPosition
                    val drift = abs(ctrl.currentPosition - targetPos)
                    if (drift > PLAYBACK_POSITION_TOLERANCE_MS) {
                        ctrl.seekTo(targetPos)
                    }
                    ctrl.play()
                } finally {
                    isSyncing = false
                }
            }

            PlaybackActions.PAUSE -> {
                isSyncing = true
                try {
                    ctrl.pause()
                    val targetPos = action.position ?: ctrl.currentPosition
                    val drift = abs(ctrl.currentPosition - targetPos)
                    if (drift > POSITION_TOLERANCE_MS) {
                        ctrl.seekTo(targetPos)
                    }
                } finally {
                    isSyncing = false
                }
            }

            PlaybackActions.SEEK -> {
                val targetPos = action.position ?: return
                isSyncing = true
                try {
                    ctrl.seekTo(targetPos)
                } finally {
                    isSyncing = false
                }
            }

            PlaybackActions.SKIP_NEXT -> {
                if (ctrl.hasNextMediaItem()) {
                    isSyncing = true
                    try {
                        ctrl.seekToNextMediaItem()
                    } finally {
                        isSyncing = false
                    }
                }
            }

            PlaybackActions.SKIP_PREV -> {
                if (ctrl.hasPreviousMediaItem()) {
                    isSyncing = true
                    try {
                        ctrl.seekToPreviousMediaItem()
                    } finally {
                        isSyncing = false
                    }
                }
            }
        }
    }

    private fun applyGuestSyncState(state: SyncStatePayload) {
        val ctrl = controller ?: return
        val currentTrack = state.currentTrack

        if (currentTrack != null) {
            val currentMediaId = ctrl.currentMediaItem?.mediaId
            if (currentMediaId != currentTrack.id) {
                loadGuestTrack(currentTrack, playImmediately = state.isPlaying, startPosition = state.position)
                return
            }
        }

        isSyncing = true
        try {
            val drift = abs(ctrl.currentPosition - state.position)
            if (drift > POSITION_TOLERANCE_MS) {
                ctrl.seekTo(state.position)
            }
            if (state.isPlaying) {
                ctrl.play()
            } else {
                ctrl.pause()
            }
        } finally {
            isSyncing = false
        }
    }

    private fun applyGuestRoomState(state: RoomState) {
        val ctrl = controller ?: return
        val currentTrack = state.currentTrack

        if (currentTrack != null) {
            val currentMediaId = ctrl.currentMediaItem?.mediaId
            if (currentMediaId != currentTrack.id) {
                loadGuestTrack(currentTrack, playImmediately = state.isPlaying, startPosition = state.position)
                return
            }
        }

        isSyncing = true
        try {
            val drift = abs(ctrl.currentPosition - state.position)
            if (drift > POSITION_TOLERANCE_MS) {
                ctrl.seekTo(state.position)
            }
            if (state.isPlaying) {
                ctrl.play()
            } else {
                ctrl.pause()
            }
        } finally {
            isSyncing = false
        }
    }

    private fun loadGuestTrack(trackInfo: TrackInfo, playImmediately: Boolean, startPosition: Long) {
        val ctrl = controller ?: return
        val song = trackInfo.toSong()

        isSyncing = true
        try {
            ctrl.playSongs(listOf(song), 0)
            if (startPosition > 0) {
                ctrl.seekTo(startPosition)
            }
            if (!playImmediately) {
                ctrl.pause()
            }
            client.sendBufferReady(trackInfo.id)
        } finally {
            isSyncing = false
        }
    }

    // Helper conversion
    private fun mediaItemToTrackInfo(item: MediaItem): TrackInfo {
        val metadata = item.mediaMetadata
        return TrackInfo(
            id = item.mediaId,
            title = metadata.title?.toString().orEmpty().ifBlank { "Unknown Title" },
            artist = metadata.artist?.toString().orEmpty().ifBlank { "Unknown Artist" },
            album = metadata.albumTitle?.toString(),
            duration = controller?.duration?.coerceAtLeast(0L) ?: 0L,
            thumbnail = metadata.artworkUri?.toString()
        )
    }

    fun TrackInfo.toSong(): Song {
        val durationFormatted = if (duration > 0) {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "$minutes:${seconds.toString().padStart(2, '0')}"
        } else null

        return Song(
            videoId = id,
            title = title,
            artist = artist,
            albumName = album,
            durationText = durationFormatted,
            thumbnailUrl = thumbnail
        )
    }

    // UI actions
    fun createRoom(username: String) {
        client.createRoom(username)
    }

    fun joinRoom(roomCode: String, username: String) {
        client.joinRoom(roomCode, username)
    }

    fun leaveRoom() {
        controller?.removeListener(playerListener)
        client.leaveRoom()
    }

    fun approveJoin(userId: String) {
        client.approveJoin(userId)
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        client.rejectJoin(userId, reason)
    }

    fun suggestTrack(song: Song) {
        val durationMs = song.durationMillis()
        val trackInfo = TrackInfo(
            id = song.videoId,
            title = song.title,
            artist = song.artist,
            album = song.albumName,
            duration = durationMs,
            thumbnail = song.thumbnailUrl,
            suggestedBy = AppSettings.listenTogetherUsername.value
        )
        client.suggestTrack(trackInfo)
    }

    fun approveSuggestion(suggestion: SuggestionReceivedPayload) {
        client.approveSuggestion(suggestion.suggestionId)
        // If host, add song to queue
        val song = suggestion.trackInfo.toSong()
        controller?.addMediaItem(song.toMediaItem())
    }

    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        client.rejectSuggestion(suggestionId, reason)
    }

    fun kickUser(userId: String, reason: String? = null) {
        client.kickUser(userId, reason)
    }

    fun transferHost(newHostId: String) {
        client.transferHost(newHostId)
    }
}
