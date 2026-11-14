package com.music.dhvani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.AudioQuality
import com.music.dhvani.data.sources.SourceConfig
import com.music.dhvani.data.sources.SourceHealth
import com.music.dhvani.data.sources.SourceKind
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.sources.RemoteSourcesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where the app is allowed to get audio from.
 *
 * Dhvani prioritizes sources from top to bottom: high-resolution modular addons
 * and bit-exact studio sources are queried first, followed by high-quality AAC,
 * and finally Standard Opus as the universal fallback.
 */
@Composable
fun SourcesScreen(
    contentPadding: PaddingValues,
    onEditCustomModule: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val configs by SourceRegistry.configs.collectAsStateWithLifecycle()
    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.meteredConnection.collectAsStateWithLifecycle()

    /** Last known reachability per source, filled in as the probes come back. */
    val health = remember { mutableStateMapOf<String, SourceHealth>() }
    var editing by remember { mutableStateOf<SourceConfig?>(null) }
    var showTurnstileDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showTurnstileDialog) {
        com.music.dhvani.ui.components.TurnstileVerificationDialog(
            onDismiss = { showTurnstileDialog = false },
            onVerified = {
                showTurnstileDialog = false
                scope.launch(Dispatchers.IO) {
                    configs.filter { it.kind == SourceKind.TIDAL }.forEach { cfg ->
                        val source = SourceRegistry.instance(cfg.id) ?: return@forEach
                        health[cfg.id] = runCatching { source.health() }.getOrElse { SourceHealth.Unreachable(it.message ?: "Failed") }
                    }
                }
            },
        )
    }

    // Every source that has a server to reach is probed, not just the built-in
    // module, so a custom index gets the same reachability line.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            com.music.dhvani.data.sources.RemoteSourcesManager.syncRemoteSources(force = true)
        }
    }
    val probeKey = configs.filter { it.kind.needsServer || it.kind == SourceKind.MONOCHROME }.joinToString { "${it.id}@${it.baseUrl}" }
    LaunchedEffect(probeKey) {
        configs.filter { (it.kind.needsServer || it.kind == SourceKind.MONOCHROME) && it.isComplete }.forEach { config ->
            val source = SourceRegistry.instance(config.id) ?: return@forEach
            health[config.id] = withContext(Dispatchers.IO) {
                runCatching { source.health() }
                    .getOrElse { SourceHealth.Unreachable(it.message ?: "Failed") }
            }
        }
    }

    /** Whether the ceiling in force right now would cap a lossless stream anyway. */
    val currentCeiling = if (metered == true) cellularQuality else wifiQuality
    val cappedByQuality = currentCeiling == AudioQuality.LOW || currentCeiling == AudioQuality.MEDIUM
    val anyLosslessSource = configs.any { it.enabled && it.isComplete && it.kind.canServeLossless }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        // Screen Header & Priority Explanation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GROUP_INSET + 2.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Sources & Modules",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Dhvani queries enabled streaming sources in priority order from top to bottom. Drag handles (≡) to adjust playback priority. If a source lacks a song or is unreachable, playback seamlessly steps down to the next source.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(14.dp))

            // Quick status pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Active count pill
                val enabledCount = configs.count { it.enabled }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = "$enabledCount of ${configs.size} Sources Active",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }

                // Gist Sync pill
                val isGistSyncing by com.music.dhvani.data.sources.RemoteSourcesManager.isSyncing.collectAsState()
                Surface(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            com.music.dhvani.data.sources.RemoteSourcesManager.syncRemoteSources(force = true)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = "Sync Gist",
                            tint = if (isGistSyncing) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isGistSyncing) "Syncing..." else "Gist Sync",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }

                // Lossless capability pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (anyLosslessSource && !cappedByQuality) Color(0xFF10B981).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(0.6.dp, if (anyLosslessSource && !cappedByQuality) Color(0xFF10B981).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (anyLosslessSource && !cappedByQuality) Color(0xFF34D399) else Color(0xFFFBBF24)),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = when {
                                cappedByQuality -> "Quality Capped"
                                anyLosslessSource -> "Hi-Res Available"
                                else -> "Standard Catalog"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = if (anyLosslessSource && !cappedByQuality) Color(0xFF34D399) else Color(0xFFFBBF24),
                        )
                    }
                }
            }
        }

        // Section header
        Text(
            text = "SOURCES — TRIED IN THIS ORDER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
            modifier = Modifier.padding(
                start = GROUP_INSET + 4.dp,
                end = GROUP_INSET,
                top = 18.dp,
                bottom = 8.dp,
            ),
        )

        // Sources List Card Container
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GROUP_INSET),
        ) {
            ReorderableSourceList(
                configs = configs,
                health = health,
                onReorder = { newOrder ->
                    SourceRegistry.reorder(newOrder)
                },
                onEdit = { config ->
                    if (config.kind == SourceKind.TIDAL && !com.music.dhvani.data.sources.zarz.ZarzSessionManager.isSessionValid()) {
                        showTurnstileDialog = true
                    } else {
                        editing = config
                    }
                },
                onToggle = { configId, enabled ->
                    SourceRegistry.setEnabled(configId, enabled)
                },
                onVerifyTidal = {
                    showTurnstileDialog = true
                },
            )
        }

        Spacer(Modifier.height(14.dp))

        // Prominent "Add streaming addon or module" Action Card
        Surface(
            onClick = {
                editing = SourceConfig(kind = SourceKind.CUSTOM_MODULE)
            },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GROUP_INSET),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add source",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Add Streaming Addon or Module",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Connect Eclipse Addon (manifest.json) or custom modular index URL",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Structured "How Fallback & Audio Quality Work" Info Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.035f),
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GROUP_INSET),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "How Fallback & Quality Work",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(Modifier.height(14.dp))

                FallbackRuleItem(
                    number = "1",
                    title = "Strict Priority Cascade",
                    detail = "Sources are queried in order from top to bottom. Drag handles (≡) on any row to elevate your preferred streaming source.",
                )

                Spacer(Modifier.height(10.dp))

                FallbackRuleItem(
                    number = "2",
                    title = "Fault-Tolerant Playback",
                    detail = "If a higher-ranked source doesn't have the song or its server is temporarily unreachable, Dhvani instantly falls through to the next source without pausing.",
                )

                Spacer(Modifier.height(10.dp))

                FallbackRuleItem(
                    number = "3",
                    title = "Seamless Lossless Upgrade",
                    detail = "Playback starts instantly from fast sources like Standard Opus, then silently swaps to lossless studio masters in the background when available.",
                )

                if (cappedByQuality) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFBBF24).copy(alpha = 0.10f),
                        border = BorderStroke(0.6.dp, Color(0xFFFBBF24).copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Note: Your current audio quality setting is set below High. All incoming audio will be transcoded to conserve data.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                } else if (!anyLosslessSource) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Tip: None of your enabled sources currently supply bit-exact FLAC audio. Add a custom module or Eclipse addon above to stream lossless audio.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        }

        // Ample bottom spacer ensuring full clearance above mini-player and nav bar
        Spacer(Modifier.height(110.dp))
    }

    editing?.let { config ->
        ServerEditorDialog(
            config = config,
            onDismiss = { editing = null },
            onSave = { saved ->
                val autoLabeled = if (saved.label.isBlank()) {
                    val friendly = SourceRegistry.friendlyAddonName(saved.baseUrl)
                    if (friendly.isNotBlank()) saved.copy(label = friendly)
                    else if (SourceRegistry.isAddonUrl(saved.baseUrl)) saved.copy(label = "Custom Addon")
                    else saved
                } else saved

                if (SourceRegistry.config(autoLabeled.id) == null) {
                    SourceRegistry.add(autoLabeled)
                } else {
                    SourceRegistry.update(autoLabeled)
                }
                editing = null
            },
            onDelete = {
                SourceRegistry.remove(config.id)
                health.remove(config.id)
                editing = null
            },
            probe = { candidate ->
                withContext(Dispatchers.IO) {
                    runCatching { SourceRegistry.probeCandidate(candidate) }
                        .getOrElse { SourceHealth.Unreachable(it.message ?: "Failed") }
                }
            },
            scope = scope,
        )
    }
}

@Composable
private fun FallbackRuleItem(
    number: String,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        }
    }
}

private const val SWAP_THRESHOLD = 0.6f

@Composable
private fun ReorderableSourceList(
    configs: List<SourceConfig>,
    health: Map<String, SourceHealth>,
    onReorder: (List<SourceConfig>) -> Unit,
    onEdit: (SourceConfig) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onVerifyTidal: (() -> Unit)? = null,
) {
    var liveOrder by remember(configs) { mutableStateOf(configs) }
    var draggedConfig by remember { mutableStateOf<SourceConfig?>(null) }
    var totalDrag by remember { mutableStateOf(0f) }
    var startIndex by remember { mutableStateOf(0) }
    var pitchPx by remember { mutableStateOf(0f) }
    var lockedPitchPx by remember { mutableStateOf(0f) }

    Column {
        liveOrder.forEachIndexed { index, config ->
            key(config.id) {
                val dragging = config == draggedConfig
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 2f else 0f)
                        .onSizeChanged { pitchPx = it.height.toFloat() }
                        .graphicsLayer {
                            translationY = if (dragging) {
                                totalDrag - (liveOrder.indexOf(config) - startIndex) * lockedPitchPx
                            } else {
                                0f
                            }
                        },
                ) {
                    if (index > 0) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 0.6.dp,
                        )
                    }
                    SourceRow(
                        position = liveOrder.indexOf(config) + 1,
                        config = config,
                        health = health[config.id],
                        isDragging = dragging,
                        onClick = when {
                            config.kind == SourceKind.TIDAL -> ({
                                if (!com.music.dhvani.data.sources.zarz.ZarzSessionManager.isSessionValid()) {
                                    onVerifyTidal?.invoke()
                                } else if (!RemoteSourcesManager.isManagedRemote(config)) {
                                    onEdit(config)
                                }
                            })
                            config.kind.needsServer && !RemoteSourcesManager.isManagedRemote(config) -> ({ onEdit(config) })
                            else -> null
                        },
                        onToggle = if (config.kind == SourceKind.YOUTUBE) {
                            null
                        } else {
                            ({ onToggle(config.id, it) })
                        },
                        onDragHandlePointerInput = Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedConfig = config
                                    totalDrag = 0f
                                    startIndex = liveOrder.indexOf(config)
                                    lockedPitchPx = pitchPx
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    val pitch = lockedPitchPx
                                    if (pitch <= 0f) return@detectDragGestures
                                    var currIdx = liveOrder.indexOf(config)
                                    if (currIdx < 0) return@detectDragGestures

                                    totalDrag = (totalDrag + delta.y).coerceIn(
                                        -startIndex * pitch,
                                        (liveOrder.lastIndex - startIndex) * pitch,
                                    )

                                    while (true) {
                                        val travelled = totalDrag / pitch
                                        val moved = (currIdx - startIndex).toFloat()
                                        if (travelled > moved + SWAP_THRESHOLD && currIdx < liveOrder.lastIndex) {
                                            liveOrder = liveOrder.toMutableList().apply {
                                                add(currIdx + 1, removeAt(currIdx))
                                            }
                                            currIdx++
                                        } else if (travelled < moved - SWAP_THRESHOLD && currIdx > 0) {
                                            liveOrder = liveOrder.toMutableList().apply {
                                                add(currIdx - 1, removeAt(currIdx))
                                            }
                                            currIdx--
                                        } else {
                                            break
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedConfig = null
                                    totalDrag = 0f
                                    onReorder(liveOrder)
                                },
                                onDragCancel = {
                                    draggedConfig = null
                                    totalDrag = 0f
                                    liveOrder = configs
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

private data class SourceStatusDisplay(
    val text: String,
    val color: Color,
    val showSpinner: Boolean = false,
    val isError: Boolean = false,
)

@Composable
private fun SourceRow(
    position: Int,
    config: SourceConfig,
    health: SourceHealth?,
    isDragging: Boolean = false,
    onClick: (() -> Unit)?,
    /** Null for a source that cannot be switched off, which gets an "Always on" label instead. */
    onToggle: ((Boolean) -> Unit)?,
    onDragHandlePointerInput: Modifier = Modifier,
) {
    val isEnabled = config.enabled
    val isBuiltin = config.kind == SourceKind.JIOSAAVN || config.kind == SourceKind.YOUTUBE || config.kind == SourceKind.MONOCHROME

    val statusInfo = remember(health, config) {
        when {
            !config.isComplete -> SourceStatusDisplay("Tap to finish setting up", Color(0xFFFBBF24), isError = true)
            health is SourceHealth.Ok -> {
                val rawDetail = health.detail.orEmpty().trim()
                val cleanDetail = if (rawDetail.startsWith(config.displayName, ignoreCase = true)) {
                    rawDetail.removePrefix(config.displayName).trim().removePrefix("-").removePrefix("—").trim()
                } else rawDetail

                val labelStr = config.kind.labels.take(3).joinToString(" • ")
                val text = when {
                    cleanDetail.isNotBlank() && labelStr.isNotBlank() -> "$cleanDetail • $labelStr"
                    cleanDetail.isNotBlank() -> cleanDetail
                    else -> labelStr
                }
                SourceStatusDisplay(text, Color.White.copy(alpha = 0.65f))
            }
            health is SourceHealth.Rejected -> SourceStatusDisplay("Error: ${health.reason}", Color(0xFFF87171), isError = true)
            health is SourceHealth.Unreachable -> {
                if (config.kind == SourceKind.MONOCHROME) {
                    SourceStatusDisplay("Server is temporarily down • Please check back in a few days", Color(0xFFFBBF24), isError = true)
                } else {
                    SourceStatusDisplay("Offline: ${health.reason}", Color(0xFFF87171).copy(alpha = 0.85f), isError = true)
                }
            }
            (config.kind.needsServer || config.kind == SourceKind.MONOCHROME) -> SourceStatusDisplay("Checking status…", Color.White.copy(alpha = 0.45f), showSpinner = true)
            else -> SourceStatusDisplay(config.kind.labels.take(3).joinToString(" • "), Color.White.copy(alpha = 0.55f))
        }
    }

    val iconBgColor = when (config.kind) {
        SourceKind.TIDAL -> Color(0xFF00E5FF).copy(alpha = 0.16f)
        SourceKind.MONOCHROME -> Color(0xFF10B981).copy(alpha = 0.16f)
        SourceKind.KUWO -> Color(0xFFE11D48).copy(alpha = 0.16f)
        SourceKind.NETEASE -> Color(0xFFEF4444).copy(alpha = 0.16f)
        SourceKind.JIOSAAVN -> Color(0xFF0EA5E9).copy(alpha = 0.16f)
        SourceKind.YOUTUBE -> Color(0xFFF59E0B).copy(alpha = 0.16f)
        SourceKind.CUSTOM_MODULE, SourceKind.MODULE -> Color(0xFF8B5CF6).copy(alpha = 0.16f)
    }
    val iconTint = when (config.kind) {
        SourceKind.TIDAL -> Color(0xFF00E5FF)
        SourceKind.MONOCHROME -> Color(0xFF34D399)
        SourceKind.KUWO -> Color(0xFFFB7185)
        SourceKind.NETEASE -> Color(0xFFF87171)
        SourceKind.JIOSAAVN -> Color(0xFF38BDF8)
        SourceKind.YOUTUBE -> Color(0xFFFBBF24)
        SourceKind.CUSTOM_MODULE, SourceKind.MODULE -> Color(0xFFA78BFA)
    }
    val sourceIcon = when (config.kind) {
        SourceKind.TIDAL, SourceKind.KUWO -> Icons.Rounded.Waves
        SourceKind.NETEASE, SourceKind.JIOSAAVN -> Icons.Rounded.GraphicEq
        SourceKind.CUSTOM_MODULE, SourceKind.MODULE -> {
            if (SourceRegistry.isAddonUrl(config.baseUrl)) Icons.Rounded.Waves else Icons.Rounded.Extension
        }
        SourceKind.MONOCHROME -> Icons.Rounded.Waves
        SourceKind.YOUTUBE -> Icons.Rounded.PlayCircle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDragging) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                } else {
                    Modifier
                }
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drag Handle Touch Target
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(onDragHandlePointerInput),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder source $position",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 0.55f else 0.25f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Source Icon Container
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isEnabled) iconBgColor else Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = sourceIcon,
                contentDescription = null,
                tint = if (isEnabled) iconTint else Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        // Middle: Title + Tag pill, and Status line
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (isEnabled) 1f else 0.45f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = config.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Spacer(Modifier.width(6.dp))

                // Kind Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isBuiltin) Color.White.copy(alpha = 0.07f) else Color(0xFF8B5CF6).copy(alpha = 0.14f),
                    border = BorderStroke(0.5.dp, if (isBuiltin) Color.White.copy(alpha = 0.12f) else Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                ) {
                    Text(
                        text = if (isBuiltin) "BUILT-IN" else "ADDON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isBuiltin) Color.White.copy(alpha = 0.65f) else Color(0xFFA78BFA),
                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp),
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            // Status & Format info line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (statusInfo.showSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(9.dp),
                        strokeWidth = 1.3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(5.dp))
                } else if (statusInfo.isError) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusInfo.color),
                    )
                    Spacer(Modifier.width(5.dp))
                } else if (health is SourceHealth.Ok) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399)),
                    )
                    Spacer(Modifier.width(5.dp))
                }

                Text(
                    text = statusInfo.text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                    ),
                    color = statusInfo.color,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Action: Edit button only for user's manual custom sources (hidden for Gist/remote managed sources)
        val isRemoteManaged = RemoteSourcesManager.isManagedRemote(config)
        if (config.kind.needsServer && !isRemoteManaged) {
            IconButton(
                onClick = { onClick?.invoke() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit or configure ${config.displayName}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        // Action: Always-on chip or Switch
        if (onToggle == null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Always on",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White.copy(alpha = 0.70f),
                    )
                }
            }
        } else {
            Switch(
                checked = config.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/**
 * Add or edit the module source.
 *
 * Test is offered rather than required: an index that happens to be asleep is
 * still worth saving, and refusing to store it until it answers would make
 * setting one up from a coffee shop impossible.
 */
@Composable
private fun ServerEditorDialog(
    config: SourceConfig,
    onDismiss: () -> Unit,
    onSave: (SourceConfig) -> Unit,
    onDelete: () -> Unit,
    probe: suspend (SourceConfig) -> SourceHealth,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val isNew = SourceRegistry.config(config.id) == null
    var label by remember { mutableStateOf(config.label) }
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SourceHealth?>(null) }

    val candidate = config.copy(label = label.trim(), baseUrl = baseUrl.trim())

    AlertDialog(
        onDismissRequest = { if (!testing) onDismiss() },
        title = { Text(if (isNew) "Add streaming addon or module" else config.displayName) },
        text = {
            Column {
                if (isNew) {
                    Text(
                        text = "POPULAR ADDONS (1-TAP CONNECT)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SourceRegistry.COMMUNITY_ADDON_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = baseUrl.trimEnd('/') == preset.url.trimEnd('/'),
                                onClick = {
                                    baseUrl = preset.url
                                    label = preset.name
                                    result = null
                                    scope.launch {
                                        testing = true
                                        result = probe(config.copy(label = preset.name, baseUrl = preset.url))
                                        testing = false
                                    }
                                },
                                label = {
                                    Text(preset.name, style = MaterialTheme.typography.bodySmall)
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        result = null
                        if (label.isBlank()) {
                            val friendly = SourceRegistry.friendlyAddonName(it)
                            if (friendly.isNotBlank()) label = friendly
                        }
                    },
                    label = { Text(stringResource(R.string.link)) },
                    placeholder = {
                        Text("https://example.com/manifest.json")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Paste the URL of a compatible module index (index.json) or plugin manifest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.name_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                result?.let { health ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when (health) {
                            is SourceHealth.Ok ->
                                listOfNotNull("Connected", health.detail).joinToString(" — ")
                            is SourceHealth.Rejected -> health.reason
                            is SourceHealth.Unreachable -> health.reason
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (health.isOk) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }

                if (!isNew) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.Start)) {
                        Text(stringResource(R.string.remove_source), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        testing = true
                        result = null
                        scope.launch {
                            val probeResult = probe(candidate)
                            result = probeResult
                            if (probeResult is SourceHealth.Ok && label.isBlank()) {
                                val auto = probeResult.detail?.substringBefore(" v")?.trim().orEmpty()
                                if (auto.isNotBlank()) {
                                    label = auto
                                }
                            }
                            testing = false
                        }
                    },
                    enabled = !testing && candidate.isComplete,
                ) {
                    Text(if (testing) "Testing…" else "Test")
                }
                TextButton(
                    onClick = { onSave(candidate) },
                    enabled = !testing && candidate.isComplete,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !testing) { Text(stringResource(R.string.cancel)) }
        },
    )
}
