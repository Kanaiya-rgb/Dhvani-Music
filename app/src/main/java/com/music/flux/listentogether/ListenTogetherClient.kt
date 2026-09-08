package com.music.flux.listentogether

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.music.flux.data.Http
import com.music.flux.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/**
 * Connection state for the Listen Together feature
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
}

/**
 * Room role for the current user
 */
enum class RoomRole {
    HOST,
    GUEST,
    NONE,
}

/**
 * Log entry for debugging
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String,
    val details: String? = null,
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG,
}

/**
 * Pending action to execute when connected
 */
sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}

/**
 * Event types for the Listen Together client
 */
sealed class ListenTogetherEvent {
    data class Connected(val userId: String) : ListenTogetherEvent()
    data object Disconnected : ListenTogetherEvent()
    data class ConnectionError(val error: String) : ListenTogetherEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ListenTogetherEvent()

    // Room events
    data class RoomCreated(val roomCode: String, val userId: String) : ListenTogetherEvent()
    data class JoinRequestReceived(val userId: String, val username: String) : ListenTogetherEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: RoomState) : ListenTogetherEvent()
    data class JoinRejected(val reason: String) : ListenTogetherEvent()
    data class UserJoined(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserLeft(val userId: String, val username: String) : ListenTogetherEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : ListenTogetherEvent()
    data class Kicked(val reason: String) : ListenTogetherEvent()
    data class Reconnected(val roomCode: String, val userId: String, val state: RoomState, val isHost: Boolean) : ListenTogetherEvent()
    data class UserReconnected(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserDisconnected(val userId: String, val username: String) : ListenTogetherEvent()

    // Playback events
    data class PlaybackSync(val action: PlaybackActionPayload) : ListenTogetherEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : ListenTogetherEvent()
    data class BufferComplete(val trackId: String) : ListenTogetherEvent()
    data class SyncStateReceived(val state: SyncStatePayload) : ListenTogetherEvent()

    // Suggestions
    data class SuggestionReceived(val suggestion: SuggestionReceivedPayload) : ListenTogetherEvent()
    data class SuggestionApproved(val suggestion: SuggestionApprovedPayload) : ListenTogetherEvent()
    data class SuggestionRejected(val suggestion: SuggestionRejectedPayload) : ListenTogetherEvent()

    // Error events
    data class ServerError(val code: String, val message: String) : ListenTogetherEvent()
}

/**
 * WebSocket client for Listen Together feature
 */
class ListenTogetherClient(
    private val context: Context,
) {
    companion object {
        private const val TAG = "ListenTogether"
        val DEFAULT_SERVER_URL: String get() = ListenTogetherServers.defaultServerUrl
        private const val MAX_RECONNECT_ATTEMPTS = 15
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 120000L
        private const val PING_INTERVAL_MS = 25000L
        private const val MAX_LOG_ENTRIES = 200
        private const val SESSION_GRACE_PERIOD_MS = 10 * 60 * 1000L // 10 minutes

        @Volatile
        private var instance: ListenTogetherClient? = null

        fun getInstance(): ListenTogetherClient? = instance

        fun init(context: Context): ListenTogetherClient {
            return instance ?: synchronized(this) {
                instance ?: ListenTogetherClient(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.NONE)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<JoinRequestPayload>>(emptyList())
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = _pendingJoinRequests.asStateFlow()

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    private val _pendingSuggestions = MutableStateFlow<List<SuggestionReceivedPayload>>(emptyList())
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = _pendingSuggestions.asStateFlow()

    private val _blockedUsernames = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsernames: StateFlow<Set<String>> = _blockedUsernames.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _events = MutableSharedFlow<ListenTogetherEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    // Internal state
    private var webSocket: WebSocket? = null
    private val messageCodec = MessageCodec(compressionEnabled = true)
    private var currentRoomCode: String? = null
    private var sessionToken: String? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var pendingAction: PendingAction? = null
    private var pingSequence = AtomicLong(0)
    private var isIntentionalDisconnect = false
    private var isNetworkAvailable = true

    val isInRoom: Boolean get() = _roomState.value != null
    val isHost: Boolean get() = _role.value == RoomRole.HOST
    val hasPersistedSession: Boolean get() = sessionToken != null && currentRoomCode != null

    init {
        restoreSession()
        observeNetworkChanges()
    }

    private fun log(level: LogLevel, message: String, details: String? = null) {
        val entry = LogEntry(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
            level = level,
            message = message,
            details = details,
        )
        when (level) {
            LogLevel.INFO -> Log.i(TAG, "$message: $details")
            LogLevel.WARNING -> Log.w(TAG, "$message: $details")
            LogLevel.ERROR -> Log.e(TAG, "$message: $details")
            LogLevel.DEBUG -> Log.d(TAG, "$message: $details")
        }
        val current = _logs.value.toMutableList()
        if (current.size >= MAX_LOG_ENTRIES) {
            current.removeAt(0)
        }
        current.add(entry)
        _logs.value = current
    }

    private fun restoreSession() {
        val token = AppSettings.listenTogetherSessionToken.value
        val roomCode = AppSettings.listenTogetherRoomCode.value
        val savedUserId = AppSettings.listenTogetherUserId.value
        val isHost = AppSettings.listenTogetherIsHost.value
        val timestamp = AppSettings.listenTogetherSessionTimestamp.value

        if (token.isNotEmpty() && roomCode.isNotEmpty()) {
            val elapsed = System.currentTimeMillis() - timestamp
            if (elapsed < SESSION_GRACE_PERIOD_MS) {
                sessionToken = token
                currentRoomCode = roomCode
                _userId.value = savedUserId
                _role.value = if (isHost) RoomRole.HOST else RoomRole.GUEST
                log(LogLevel.INFO, "Restored session for room $roomCode", "Token: $token")
            } else {
                clearSession()
            }
        }
    }

    private fun persistSession() {
        AppSettings.setListenTogetherSession(
            token = sessionToken ?: "",
            roomCode = currentRoomCode ?: "",
            userId = _userId.value ?: "",
            isHost = isHost,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun clearSession() {
        sessionToken = null
        currentRoomCode = null
        AppSettings.clearListenTogetherSession()
    }

    private fun observeNetworkChanges() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isNetworkAvailable = true
                    log(LogLevel.INFO, "Network became available")
                    if (isInRoom && _connectionState.value == ConnectionState.DISCONNECTED && !isIntentionalDisconnect) {
                        reconnect()
                    }
                }

                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                    log(LogLevel.WARNING, "Network lost")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    /**
     * Connects to WebSocket server.
     */
    fun connect(serverUrl: String? = null) {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            return
        }

        val url = (serverUrl ?: AppSettings.listenTogetherServerUrl.value).ifBlank { DEFAULT_SERVER_URL }
        log(LogLevel.INFO, "Connecting to server", url)
        _connectionState.value = ConnectionState.CONNECTING
        isIntentionalDisconnect = false

        try {
            val request = Request.Builder().url(url).build()
            webSocket = Http.client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    log(LogLevel.INFO, "WebSocket connected successfully")
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0
                    startPingJob()

                    if (sessionToken != null && currentRoomCode != null) {
                        log(LogLevel.INFO, "Attempting to reconnect session", "Room: $currentRoomCode")
                        sendMessage(MessageTypes.RECONNECT, ReconnectPayload(sessionToken!!))
                    } else {
                        executePendingAction()
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    handleBinaryMessage(bytes.toByteArray())
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    log(LogLevel.INFO, "Server closing connection", "Code: $code, Reason: $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    log(LogLevel.INFO, "Connection closed", "Code: $code, Reason: $reason")
                    handleDisconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    log(LogLevel.ERROR, "Connection failure", t.message)
                    handleConnectionFailure(t)
                }
            })
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to create WebSocket", e.message)
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnect()
        }
    }

    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        when (action) {
            is PendingAction.CreateRoom -> {
                sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.username))
            }
            is PendingAction.JoinRoom -> {
                sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.roomCode.uppercase(), action.username))
            }
        }
    }

    private fun handleBinaryMessage(data: ByteArray) {
        try {
            val (msgType, payloadBytes) = messageCodec.decode(data)
            val payload = messageCodec.decodePayload(msgType, payloadBytes)

            when (msgType) {
                MessageTypes.ROOM_CREATED -> {
                    val p = payload as? RoomCreatedPayload ?: return
                    currentRoomCode = p.roomCode
                    sessionToken = p.sessionToken
                    _userId.value = p.userId
                    _role.value = RoomRole.HOST
                    _roomState.value = RoomState(
                        roomCode = p.roomCode,
                        hostId = p.userId,
                        users = listOf(UserInfo(p.userId, AppSettings.listenTogetherUsername.value, isHost = true)),
                        isPlaying = false,
                        position = 0,
                        lastUpdate = System.currentTimeMillis()
                    )
                    persistSession()
                    log(LogLevel.INFO, "Room created", "Code: ${p.roomCode}")
                    _events.tryEmit(ListenTogetherEvent.RoomCreated(p.roomCode, p.userId))
                }

                MessageTypes.JOIN_REQUEST -> {
                    val p = payload as? JoinRequestPayload ?: return
                    log(LogLevel.INFO, "Join request received from ${p.username} (${p.userId})")
                    if (AppSettings.listenTogetherAutoApproval.value) {
                        approveJoin(p.userId)
                    } else {
                        val current = _pendingJoinRequests.value.toMutableList()
                        if (current.none { it.userId == p.userId }) {
                            current.add(p)
                            _pendingJoinRequests.value = current
                        }
                        _events.tryEmit(ListenTogetherEvent.JoinRequestReceived(p.userId, p.username))
                    }
                }

                MessageTypes.JOIN_APPROVED -> {
                    val p = payload as? JoinApprovedPayload ?: return
                    currentRoomCode = p.roomCode
                    sessionToken = p.sessionToken
                    _userId.value = p.userId
                    _role.value = RoomRole.GUEST
                    _roomState.value = p.state
                    persistSession()
                    log(LogLevel.INFO, "Join approved", "Room: ${p.roomCode}")
                    _events.tryEmit(ListenTogetherEvent.JoinApproved(p.roomCode, p.userId, p.state))
                }

                MessageTypes.JOIN_REJECTED -> {
                    val p = payload as? JoinRejectedPayload ?: return
                    log(LogLevel.WARNING, "Join rejected", p.reason)
                    _events.tryEmit(ListenTogetherEvent.JoinRejected(p.reason))
                }

                MessageTypes.USER_JOINED -> {
                    val p = payload as? UserJoinedPayload ?: return
                    log(LogLevel.INFO, "User joined: ${p.username}")
                    val current = _roomState.value
                    if (current != null) {
                        val updatedUsers = current.users.filter { it.userId != p.userId } +
                            UserInfo(p.userId, p.username, isHost = false, isConnected = true)
                        _roomState.value = current.copy(users = updatedUsers)
                    }
                    _events.tryEmit(ListenTogetherEvent.UserJoined(p.userId, p.username))
                }

                MessageTypes.USER_LEFT -> {
                    val p = payload as? UserLeftPayload ?: return
                    log(LogLevel.INFO, "User left: ${p.username}")
                    val current = _roomState.value
                    if (current != null) {
                        _roomState.value = current.copy(users = current.users.filter { it.userId != p.userId })
                    }
                    _events.tryEmit(ListenTogetherEvent.UserLeft(p.userId, p.username))
                }

                MessageTypes.SYNC_PLAYBACK -> {
                    val p = payload as? PlaybackActionPayload ?: return
                    val current = _roomState.value
                    if (current != null) {
                        var updated = current
                        when (p.action) {
                            PlaybackActions.PLAY -> updated = updated.copy(isPlaying = true, position = p.position ?: updated.position)
                            PlaybackActions.PAUSE -> updated = updated.copy(isPlaying = false, position = p.position ?: updated.position)
                            PlaybackActions.SEEK -> updated = updated.copy(position = p.position ?: updated.position)
                            PlaybackActions.CHANGE_TRACK -> {
                                if (p.trackInfo != null) updated = updated.copy(currentTrack = p.trackInfo, position = 0, isPlaying = false)
                            }
                            PlaybackActions.SYNC_QUEUE -> {
                                if (p.queue != null) updated = updated.copy(queue = p.queue)
                            }
                        }
                        _roomState.value = updated
                    }
                    _events.tryEmit(ListenTogetherEvent.PlaybackSync(p))
                }

                MessageTypes.BUFFER_WAIT -> {
                    val p = payload as? BufferWaitPayload ?: return
                    _bufferingUsers.value = p.waitingFor
                    _events.tryEmit(ListenTogetherEvent.BufferWait(p.trackId, p.waitingFor))
                }

                MessageTypes.BUFFER_COMPLETE -> {
                    val p = payload as? BufferCompletePayload ?: return
                    _bufferingUsers.value = emptyList()
                    _events.tryEmit(ListenTogetherEvent.BufferComplete(p.trackId))
                }

                MessageTypes.SYNC_STATE -> {
                    val p = payload as? SyncStatePayload ?: return
                    val current = _roomState.value
                    if (current != null) {
                        _roomState.value = current.copy(
                            currentTrack = p.currentTrack ?: current.currentTrack,
                            isPlaying = p.isPlaying,
                            position = p.position,
                            queue = p.queue ?: current.queue,
                            volume = p.volume ?: current.volume
                        )
                    }
                    _events.tryEmit(ListenTogetherEvent.SyncStateReceived(p))
                }

                MessageTypes.RECONNECTED -> {
                    val p = payload as? ReconnectedPayload ?: return
                    currentRoomCode = p.roomCode
                    _userId.value = p.userId
                    _role.value = if (p.isHost) RoomRole.HOST else RoomRole.GUEST
                    _roomState.value = p.state
                    log(LogLevel.INFO, "Reconnected to room ${p.roomCode} as ${if (p.isHost) "Host" else "Guest"}")
                    _events.tryEmit(ListenTogetherEvent.Reconnected(p.roomCode, p.userId, p.state, p.isHost))
                }

                MessageTypes.HOST_CHANGED -> {
                    val p = payload as? HostChangedPayload ?: return
                    val current = _roomState.value
                    if (current != null) {
                        _roomState.value = current.copy(
                            hostId = p.newHostId,
                            users = current.users.map { it.copy(isHost = it.userId == p.newHostId) }
                        )
                    }
                    if (p.newHostId == _userId.value) {
                        _role.value = RoomRole.HOST
                    } else if (_role.value == RoomRole.HOST) {
                        _role.value = RoomRole.GUEST
                    }
                    persistSession()
                    _events.tryEmit(ListenTogetherEvent.HostChanged(p.newHostId, p.newHostName))
                }

                MessageTypes.KICKED -> {
                    val p = payload as? KickedPayload ?: return
                    log(LogLevel.WARNING, "Kicked from room", p.reason)
                    leaveRoom()
                    _events.tryEmit(ListenTogetherEvent.Kicked(p.reason))
                }

                MessageTypes.SUGGESTION_RECEIVED -> {
                    val p = payload as? SuggestionReceivedPayload ?: return
                    if (isHost && AppSettings.listenTogetherAutoApproveSuggestions.value) {
                        approveSuggestion(p.suggestionId)
                    } else {
                        val current = _pendingSuggestions.value.toMutableList()
                        current.add(p)
                        _pendingSuggestions.value = current
                        _events.tryEmit(ListenTogetherEvent.SuggestionReceived(p))
                    }
                }

                MessageTypes.SUGGESTION_APPROVED -> {
                    val p = payload as? SuggestionApprovedPayload ?: return
                    _events.tryEmit(ListenTogetherEvent.SuggestionApproved(p))
                }

                MessageTypes.SUGGESTION_REJECTED -> {
                    val p = payload as? SuggestionRejectedPayload ?: return
                    _events.tryEmit(ListenTogetherEvent.SuggestionRejected(p))
                }

                MessageTypes.ERROR -> {
                    val p = payload as? ErrorPayload ?: return
                    log(LogLevel.ERROR, "Server error [${p.code}]", p.message)
                    _events.tryEmit(ListenTogetherEvent.ServerError(p.code, p.message))
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to handle binary message", e.message)
        }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED) {
                delay(PING_INTERVAL_MS)
                try {
                    val ping = com.music.flux.listentogether.proto.Listentogether.PingPayload.newBuilder()
                        .setClientTime(System.currentTimeMillis())
                        .setSequence(pingSequence.incrementAndGet())
                        .build()
                    val envelope = com.music.flux.listentogether.proto.Listentogether.Envelope.newBuilder()
                        .setType(MessageTypes.PING)
                        .setPayload(ping.toByteString())
                        .build()
                    webSocket?.send(ByteString.of(*envelope.toByteArray()))
                } catch (e: Exception) {
                    log(LogLevel.WARNING, "Failed to send ping", e.message)
                }
            }
        }
    }

    fun sendMessage(type: String, payload: Any? = null) {
        val ws = webSocket
        if (ws == null || _connectionState.value != ConnectionState.CONNECTED) {
            log(LogLevel.WARNING, "Cannot send $type: not connected")
            return
        }

        try {
            val encoded = messageCodec.encode(type, payload)
            ws.send(ByteString.of(*encoded))
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error sending message $type", e.message)
        }
    }

    fun createRoom(username: String) {
        val trimmed = username.trim().ifBlank { "Listener" }
        AppSettings.setListenTogetherUsername(trimmed)
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(trimmed))
        } else {
            pendingAction = PendingAction.CreateRoom(trimmed)
            connect()
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        val cleanCode = roomCode.trim().uppercase()
        val cleanName = username.trim().ifBlank { "Listener" }
        AppSettings.setListenTogetherUsername(cleanName)
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(cleanCode, cleanName))
        } else {
            pendingAction = PendingAction.JoinRoom(cleanCode, cleanName)
            connect()
        }
    }

    fun leaveRoom() {
        isIntentionalDisconnect = true
        if (isInRoom) {
            sendMessage(MessageTypes.LEAVE_ROOM)
        }
        clearSession()
        _roomState.value = null
        _role.value = RoomRole.NONE
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        _pendingSuggestions.value = emptyList()
        disconnect()
    }

    fun approveJoin(targetUserId: String) {
        sendMessage(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(targetUserId))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != targetUserId }
    }

    fun rejectJoin(targetUserId: String, reason: String? = null) {
        sendMessage(MessageTypes.REJECT_JOIN, RejectJoinPayload(targetUserId, reason))
        _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != targetUserId }
    }

    fun sendPlaybackAction(
        action: String,
        trackId: String? = null,
        position: Long? = null,
        trackInfo: TrackInfo? = null,
        insertNext: Boolean? = null,
        queue: List<TrackInfo>? = null,
        queueTitle: String? = null,
        volume: Float? = null,
    ) {
        sendMessage(
            MessageTypes.PLAYBACK_ACTION,
            PlaybackActionPayload(
                action = action,
                trackId = trackId,
                position = position,
                trackInfo = trackInfo,
                insertNext = insertNext,
                queue = queue,
                queueTitle = queueTitle,
                volume = volume,
                serverTime = System.currentTimeMillis()
            )
        )
    }

    fun sendBufferReady(trackId: String) {
        sendMessage(MessageTypes.BUFFER_READY, BufferReadyPayload(trackId))
    }

    fun suggestTrack(trackInfo: TrackInfo) {
        sendMessage(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(trackInfo))
    }

    fun approveSuggestion(suggestionId: String) {
        sendMessage(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(suggestionId))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }

    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        sendMessage(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(suggestionId, reason))
        _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
    }

    fun kickUser(targetUserId: String, reason: String? = null) {
        sendMessage(MessageTypes.KICK_USER, KickUserPayload(targetUserId, reason))
    }

    fun transferHost(newHostId: String) {
        sendMessage(MessageTypes.TRANSFER_HOST, TransferHostPayload(newHostId))
    }

    fun requestSync() {
        sendMessage(MessageTypes.REQUEST_SYNC)
    }

    private fun handleDisconnect() {
        pingJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
        _events.tryEmit(ListenTogetherEvent.Disconnected)

        if (!isIntentionalDisconnect && isInRoom) {
            scheduleReconnect()
        }
    }

    private fun handleConnectionFailure(t: Throwable) {
        pingJob?.cancel()
        _connectionState.value = ConnectionState.ERROR
        _events.tryEmit(ListenTogetherEvent.ConnectionError(t.message ?: "Unknown connection error"))

        if (!isIntentionalDisconnect && isInRoom) {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!isNetworkAvailable) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log(LogLevel.WARNING, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayMs = (INITIAL_RECONNECT_DELAY_MS * (1 shl (reconnectAttempts - 1).coerceAtMost(6)))
                .coerceAtMost(MAX_RECONNECT_DELAY_MS)
            log(LogLevel.INFO, "Reconnecting attempt $reconnectAttempts in ${delayMs}ms")
            _connectionState.value = ConnectionState.RECONNECTING
            _events.tryEmit(ListenTogetherEvent.Reconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
            delay(delayMs)
            connect()
        }
    }

    fun reconnect() {
        reconnectAttempts = 0
        connect()
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        pingJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
