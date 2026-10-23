package com.music.dhvani.ui.components

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.io.File

/**
 * Single-screen Add / Edit Profile screen for Dhvani Music.
 * Follows the NuvioMobile profile architecture:
 * - Top back navigation with "Add Profile" / "Edit Profile" title
 * - Single scrollable canvas combining Profile Identity, Custom Avatar URL, and 5-Column Avatar Catalog
 * - Real-time responsive avatar preview card reflecting the selected avatar or custom URL
 * - Pinned bottom CTA ("Create Profile" / "Save Profile") anchored above system navigation bars
 */
@Composable
fun WelcomeNameDialog(
    initialName: String? = null,
    initialAvatarId: Int? = null,
    isInitialSetup: Boolean = true,
    title: String? = null,
    onDismiss: (() -> Unit)? = null,
    onDismissOrCompleted: (String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = rememberHaptics()

    val defaultName = remember { initialName?.ifBlank { null } ?: getSuggestedListenerName(context) }
    var inputName by rememberSaveable { mutableStateOf(defaultName) }

    val validInitialAvatarId = remember {
        val candidate = initialAvatarId ?: AppSettings.userPresetAvatarId.value
        if (PRESET_AVATARS.any { it.id == candidate }) candidate else getPresetAvatar(candidate).id
    }
    var selectedAvatarId by rememberSaveable { mutableIntStateOf(validInitialAvatarId) }

    val currentSavedCustom = remember { AppSettings.userCustomAvatarPath.value }
    val initialCustomUrl = remember {
        if (currentSavedCustom.startsWith("http://", ignoreCase = true) ||
            currentSavedCustom.startsWith("https://", ignoreCase = true)
        ) currentSavedCustom else ""
    }
    val initialLocalPath = remember {
        if (!currentSavedCustom.startsWith("http://", ignoreCase = true) &&
            !currentSavedCustom.startsWith("https://", ignoreCase = true)
        ) currentSavedCustom else ""
    }
    var customAvatarUrl by rememberSaveable { mutableStateOf(initialCustomUrl) }
    var customLocalPath by rememberSaveable { mutableStateOf(initialLocalPath) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = listOf("All", "Animals", "Minimalist", "Characters", "Portraits", "Sketches")
    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == "All") PRESET_AVATARS else PRESET_AVATARS.filter { it.category == selectedCategory }
    }

    val selectedPreset = remember(selectedAvatarId) {
        getPresetAvatar(selectedAvatarId)
    }

    val previewAccent = selectedPreset.colors.first()

    // Photo picker launcher for picking from device gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val avatarFile = File(context.filesDir, "dhvani_user_avatar.jpg")
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    customLocalPath = avatarFile.absolutePath
                    customAvatarUrl = ""
                    haptics.play(Haptic.Tap)
                    Toast.makeText(context, "Photo selected!", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(context, "Failed to load photo: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val completeSetup = {
        keyboardController?.hide()
        val finalName = inputName.trim().ifBlank { defaultName }

        if (customLocalPath.isNotBlank() && File(customLocalPath).exists()) {
            AppSettings.setUserCustomAvatar(customLocalPath)
        } else if (customAvatarUrl.isNotBlank() && customAvatarUrl.startsWith("http")) {
            AppSettings.setUserCustomAvatar(customAvatarUrl.trim())
        } else {
            AppSettings.setUserAvatarPreset(selectedAvatarId)
        }

        AppSettings.setUserName(finalName)
        haptics.play(Haptic.Tap)
        onDismissOrCompleted(finalName)
    }

    val handleDismiss = {
        if (onDismiss != null) {
            onDismiss()
        } else {
            completeSetup()
        }
    }

    BackHandler(enabled = true) {
        handleDismiss()
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.DisposableEffect(view) {
            val setupWindow = {
                var p: android.view.ViewParent? = view.parent
                while (p != null) {
                    if (p is androidx.compose.ui.window.DialogWindowProvider) {
                        val window = p.window
                        window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                        window.setBackgroundDrawableResource(android.R.color.transparent)
                        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        break
                    }
                    p = p.parent
                }
            }
            val listener = object : android.view.View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) { setupWindow() }
                override fun onViewDetachedFromWindow(v: android.view.View) {}
            }
            view.addOnAttachStateChangeListener(listener)
            setupWindow()
            onDispose {
                view.removeOnAttachStateChangeListener(listener)
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F0F12),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // ── Top Bar ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { handleDismiss() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = title ?: if (isInitialSetup) "Add Profile" else "Customize Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { completeSetup() },
                        enabled = inputName.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Save",
                            tint = if (inputName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                // ── Scrollable Body ─────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ── Card 1: Profile Identity Card ───────────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF19191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Top Row: Avatar Preview + Name & Subtitle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Dynamic Avatar Preview
                                val scaleAnim by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                                    label = "avatarScale",
                                )

                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .scale(scaleAnim)
                                        .clip(CircleShape)
                                        .background(previewAccent)
                                        .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        customLocalPath.isNotBlank() && File(customLocalPath).exists() -> {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(File(customLocalPath))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Custom Profile Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            )
                                        }
                                        customAvatarUrl.isNotBlank() -> {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(customAvatarUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Custom Avatar URL",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            )
                                        }
                                        selectedPreset.drawableRes != null -> {
                                            Image(
                                                painter = painterResource(selectedPreset.drawableRes),
                                                contentDescription = selectedPreset.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(44.dp),
                                            )
                                        }
                                    }
                                }

                                // Identity Text info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = inputName.ifBlank { "New profile" },
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "New profile  |  Music Listener",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.55f),
                                    )
                                    Text(
                                        text = if (customLocalPath.isNotBlank() || customAvatarUrl.isNotBlank()) {
                                            "Avatar: Custom"
                                        } else {
                                            "Avatar: ${selectedPreset.name}"
                                        },
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            // Profile Name Input Field
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                placeholder = {
                                    Text("Profile name", color = Color.White.copy(alpha = 0.35f))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF131317),
                                    unfocusedContainerColor = Color(0xFF131317),
                                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                                trailingIcon = {
                                    if (inputName.isNotBlank()) {
                                        IconButton(onClick = { inputName = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Clear",
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() },
                                ),
                            )

                            // Quick Suggestions
                            val suggestions = listOf("Audiophile", "Melophile", "Night Owl", "Beats Explorer")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                suggestions.forEach { suggestion ->
                                    val isSelected = inputName.trim().equals(suggestion, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) previewAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) previewAccent else Color.White.copy(alpha = 0.12f),
                                        ),
                                        modifier = Modifier.clickable {
                                            haptics.play(Haptic.Select)
                                            inputName = suggestion
                                        },
                                    ) {
                                        Text(
                                            text = suggestion,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Card 2: Custom Avatar URL / Gallery ─────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF19191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Custom avatar URL",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "Paste an image link, or leave this empty to use the built-in avatar catalog.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                lineHeight = 18.sp,
                            )
                            OutlinedTextField(
                                value = customAvatarUrl,
                                onValueChange = { value ->
                                    customAvatarUrl = value
                                    if (value.isNotBlank()) {
                                        customLocalPath = ""
                                    }
                                },
                                placeholder = {
                                    Text("https://example.com/avatar.png", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF131317),
                                    unfocusedContainerColor = Color(0xFF131317),
                                    focusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                                trailingIcon = {
                                    if (customAvatarUrl.isNotBlank()) {
                                        IconButton(onClick = { customAvatarUrl = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Clear",
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                },
                            )

                            // Pick from gallery action button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (customLocalPath.isNotBlank()) "Photo selected from device" else "Or pick photo from gallery",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }

                    // ── Card 3: Choose an Avatar ────────────────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF19191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Choose an avatar",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = selectedPreset.name,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }

                            // Category filter chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                categories.forEach { cat ->
                                    val isCatSelected = selectedCategory == cat
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier.clickable {
                                            haptics.play(Haptic.Select)
                                            selectedCategory = cat
                                        },
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCatSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }

                            // 5-Column Avatar Catalog Grid (matching Nuvio reference layout)
                            val columns = 5
                            val spacing = 10.dp

                            Column(
                                verticalArrangement = Arrangement.spacedBy(spacing),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                filteredPresets.chunked(columns).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(spacing),
                                    ) {
                                        row.forEach { avatar ->
                                            val isSelected = (avatar.id == selectedAvatarId) && customAvatarUrl.isBlank() && customLocalPath.isBlank()
                                            val itemScale by animateFloatAsState(
                                                targetValue = if (isSelected) 1.08f else 1.0f,
                                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                                                label = "itemScale",
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .scale(itemScale)
                                                    .clip(CircleShape)
                                                    .background(avatar.colors.first())
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                                                        shape = CircleShape,
                                                    )
                                                    .clickable {
                                                        haptics.play(Haptic.Tap)
                                                        customAvatarUrl = ""
                                                        customLocalPath = ""
                                                        selectedAvatarId = avatar.id
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (avatar.drawableRes != null) {
                                                    Image(
                                                        painter = painterResource(avatar.drawableRes),
                                                        contentDescription = avatar.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = avatar.icon,
                                                        contentDescription = avatar.name,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp),
                                                    )
                                                }
                                            }
                                        }

                                        // Fill remaining items in row to maintain grid alignment
                                        repeat(columns - row.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Primary Action Button (inside scroll view — always scrollable to!) ──
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { completeSetup() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inputName.isNotBlank()) Color.White else Color(0xFF28282E),
                            contentColor = if (inputName.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.35f),
                            disabledContainerColor = Color(0xFF202024),
                            disabledContentColor = Color.White.copy(alpha = 0.35f),
                        ),
                        enabled = inputName.isNotBlank(),
                    ) {
                        Text(
                            text = if (isInitialSetup) "Create Profile" else "Save Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Bottom padding ensuring button scrolls comfortably well above system navigation bars
                    Spacer(Modifier.navigationBarsPadding())
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * Resolves a friendly default listener name without using raw hardware model codes.
 */
private fun getSuggestedListenerName(context: Context): String {
    return runCatching {
        val sysDeviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        val isHardwareName = !sysDeviceName.isNullOrBlank() && (
            sysDeviceName.contains(Build.MODEL, ignoreCase = true) ||
            sysDeviceName.contains(Build.MANUFACTURER, ignoreCase = true) ||
            sysDeviceName.contains(Build.BRAND, ignoreCase = true)
        )
        if (!sysDeviceName.isNullOrBlank() && !isHardwareName) {
            return@runCatching sysDeviceName
        }
        "Music Lover"
    }.getOrDefault("Music Lover")
}
