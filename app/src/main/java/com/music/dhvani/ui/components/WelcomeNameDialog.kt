package com.music.dhvani.ui.components

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
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
import com.music.dhvani.data.settings.AppSettings

/**
 * Ultra-premium, responsive first-launch onboarding dialog for Dhvani Music.
 * Features:
 * - Dynamic ambient aura backlight matching the active avatar's palette
 * - 2-step setup: Avatar Selection & VIP Profile Persona
 * - Real-time Live VIP Profile Preview Card with animated equalizer waves
 * - Quick suggestion tags for effortless name selection
 * - Pinned bottom navigation ensuring action buttons are NEVER cut off
 */
@Composable
fun WelcomeNameDialog(
    onDismissOrCompleted: (String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val defaultName = remember { getSuggestedListenerName(context) }
    var inputName by rememberSaveable { mutableStateOf(defaultName) }
    var selectedAvatarId by rememberSaveable {
        mutableIntStateOf(AppSettings.userPresetAvatarId.value)
    }
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = listOf("All", "3D Characters", "Music & Vibes", "Cyber & Space", "Fun & Playful", "Neon Glyphs")
    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == "All") PRESET_AVATARS else PRESET_AVATARS.filter { it.category == selectedCategory }
    }

    val selectedPreset = remember(selectedAvatarId) {
        getPresetAvatar(selectedAvatarId)
    }

    val completeSetup = {
        keyboardController?.hide()
        val finalName = inputName.trim().ifBlank { defaultName }
        AppSettings.setUserAvatarPreset(selectedAvatarId)
        AppSettings.setUserName(finalName)
        onDismissOrCompleted(finalName)
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            kotlinx.coroutines.delay(200)
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Step back interceptor
    BackHandler(enabled = currentStep > 1) {
        currentStep = 1
    }

    // Dynamic animations
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeAnimations")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "headerShimmer",
    )

    val pulseGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlowScale",
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlowAlpha",
    )

    // Mini animated equalizer waves
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    val eq4 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq4",
    )

    val primaryAccent = selectedPreset.colors.first()
    val secondaryAccent = selectedPreset.colors.last()

    Dialog(
        onDismissRequest = {
            if (currentStep > 1) {
                currentStep = 1
            } else {
                completeSetup()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.89f)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.60f),
                            secondaryAccent.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.08f),
                        )
                    ),
                    shape = RoundedCornerShape(32.dp),
                ),
            color = Color(0xFF0F0E17),
            shadowElevation = 36.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Top Shimmer Animated Accent Line ─────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    primaryAccent,
                                    secondaryAccent,
                                    Color(0xFF06B6D4),
                                    Color(0xFFD946EF),
                                    primaryAccent,
                                ),
                                startX = shimmerOffset,
                                endX = shimmerOffset + 600f,
                                tileMode = TileMode.Repeated,
                            )
                        )
                )

                Spacer(Modifier.height(14.dp))

                // ── Top Header Section: Segmented Step Tracker + Hero Avatar ───────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Segmented Step Indicator Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        primaryAccent.copy(alpha = 0.5f),
                                        secondaryAccent.copy(alpha = 0.25f),
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Step 1 Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentStep == 1) {
                                        Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { currentStep = 1 }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "1. Choose Avatar",
                                fontSize = 11.5.sp,
                                fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentStep == 1) Color.White else Color.White.copy(alpha = 0.6f),
                            )
                        }

                        // Step 2 Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentStep == 2) {
                                        Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { currentStep = 2 }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "2. Profile Name",
                                fontSize = 11.5.sp,
                                fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentStep == 2) Color.White else Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Hero Avatar Preview with Ambient Glow
                    Box(
                        modifier = Modifier.size(88.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Ambient radial glow
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseGlowScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            primaryAccent.copy(alpha = pulseGlowAlpha * 0.55f),
                                            secondaryAccent.copy(alpha = pulseGlowAlpha * 0.20f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                        )

                        // Pulsing sweep glow halo ring
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .scale(pulseGlowScale)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            primaryAccent.copy(alpha = pulseGlowAlpha),
                                            secondaryAccent.copy(alpha = pulseGlowAlpha * 0.5f),
                                            primaryAccent.copy(alpha = pulseGlowAlpha),
                                        )
                                    ),
                                    shape = CircleShape,
                                )
                        )

                        // Main Avatar Circle
                        Crossfade(
                            targetState = selectedPreset,
                            label = "AvatarCrossfade",
                        ) { preset ->
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.90f),
                                                primaryAccent.copy(alpha = 0.60f),
                                                Color.White.copy(alpha = 0.30f),
                                            )
                                        ),
                                        shape = CircleShape,
                                    )
                                    .background(Brush.linearGradient(preset.colors)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (preset.drawableRes != null) {
                                    Image(
                                        painter = painterResource(preset.drawableRes),
                                        contentDescription = preset.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Avatar Vibe Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.8.dp,
                                brush = Brush.horizontalGradient(listOf(primaryAccent.copy(0.6f), secondaryAccent.copy(0.4f))),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = primaryAccent,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = selectedPreset.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "• ${selectedPreset.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Middle Scrollable / Animated Area (WEIGHT 1f) ─────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> width } +
                                        fadeIn(animationSpec = tween(260))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> -width } +
                                            fadeOut(animationSpec = tween(220))
                                )
                            } else {
                                (slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> -width } +
                                        fadeIn(animationSpec = tween(260))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { width -> width } +
                                            fadeOut(animationSpec = tween(220))
                                )
                            }.using(SizeTransform(clip = false))
                        },
                        label = "OnboardingStepTransition",
                        modifier = Modifier.fillMaxSize(),
                    ) { step ->
                        if (step == 1) {
                            // ═════════════════════════════════════════════════════
                            // STEP 1: AVATAR SELECTION GRID
                            // ═════════════════════════════════════════════════════
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Welcome to Dhvani",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "Choose your persona for your music journey",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(10.dp))

                                // Category Filter Chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    categories.forEach { cat ->
                                        val isCatSelected = selectedCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isCatSelected) {
                                                        Brush.horizontalGradient(listOf(primaryAccent.copy(alpha = 0.35f), secondaryAccent.copy(alpha = 0.25f)))
                                                    } else {
                                                        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isCatSelected) primaryAccent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .clickable { selectedCategory = cat }
                                                .padding(horizontal = 11.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }

                                // 4-Column Responsive Grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(filteredPresets, key = { it.id }) { preset ->
                                        val isSelected = selectedAvatarId == preset.id
                                        val animatedScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.08f else 1f,
                                            animationSpec = spring(dampingRatio = 0.6f),
                                            label = "PresetScale",
                                        )

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .scale(animatedScale)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    brush = if (isSelected) {
                                                        Brush.linearGradient(listOf(primaryAccent, secondaryAccent))
                                                    } else {
                                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f)))
                                                    },
                                                    shape = CircleShape,
                                                )
                                                .padding(2.5.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(preset.colors))
                                                .clickable {
                                                    selectedAvatarId = preset.id
                                                },
                                        ) {
                                            if (preset.drawableRes != null) {
                                                Image(
                                                    painter = painterResource(preset.drawableRes),
                                                    contentDescription = preset.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = preset.icon,
                                                    contentDescription = preset.name,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.35f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ═════════════════════════════════════════════════════
                            // STEP 2: NAME ENTRY & LIVE PROFILE CARD PREVIEW
                            // ═════════════════════════════════════════════════════
                            val suggestedNames = remember {
                                val list = mutableListOf<String>()
                                if (defaultName.isNotBlank() && defaultName != "Music Lover") {
                                    list.add(defaultName)
                                }
                                list.addAll(listOf("Music Lover", "Sound Traveler", "Vibe Master", "Night Owl", "Beat Drop"))
                                list.distinct()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Name Your Profile",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "This name appears across your recaps, playlists, and greetings",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )

                                Spacer(Modifier.height(16.dp))

                                // ── Live VIP Profile Card Preview ────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    primaryAccent.copy(alpha = 0.18f),
                                                    secondaryAccent.copy(alpha = 0.08f),
                                                    Color.White.copy(alpha = 0.04f),
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    primaryAccent.copy(alpha = 0.5f),
                                                    secondaryAccent.copy(alpha = 0.25f),
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                        )
                                        .padding(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        // Mini Avatar Preview
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, primaryAccent, CircleShape)
                                                .background(Brush.linearGradient(selectedPreset.colors)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selectedPreset.drawableRes != null) {
                                                Image(
                                                    painter = painterResource(selectedPreset.drawableRes),
                                                    contentDescription = selectedPreset.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = selectedPreset.icon,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp),
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(
                                                text = inputName.trim().ifBlank { "Music Lover" },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = "Dhvani Listener",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = primaryAccent,
                                                )
                                                // Live mini Equalizer Animation
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.Bottom,
                                                    modifier = Modifier.height(14.dp),
                                                ) {
                                                    listOf(eq1, eq2, eq3, eq4).forEach { heightVal ->
                                                        Box(
                                                            modifier = Modifier
                                                                .width(2.dp)
                                                                .height(heightVal.dp)
                                                                .clip(CircleShape)
                                                                .background(primaryAccent.copy(alpha = 0.9f))
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Preview Tag Pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = "PREVIEW",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp,
                                                color = Color.White.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                // Name Input Field
                                OutlinedTextField(
                                    value = inputName,
                                    onValueChange = { inputName = it },
                                    label = {
                                        Text(
                                            text = "Your Name or Nickname",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 13.sp,
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            text = "e.g. DJ Kanaiya, Alex...",
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 13.sp,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = primaryAccent,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    trailingIcon = {
                                        if (inputName.isNotBlank()) {
                                            IconButton(
                                                onClick = { inputName = "" },
                                                modifier = Modifier.size(28.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Clear",
                                                    tint = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { completeSetup() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = primaryAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        cursorColor = primaryAccent,
                                        focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                )

                                Spacer(Modifier.height(14.dp))

                                // Quick Name Suggestions
                                Text(
                                    text = "Quick suggestions:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    suggestedNames.forEach { suggestion ->
                                        val isCurrent = inputName.trim() == suggestion
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isCurrent) primaryAccent.copy(alpha = 0.3f)
                                                    else Color.White.copy(alpha = 0.06f)
                                                )
                                                .border(
                                                    width = 0.8.dp,
                                                    color = if (isCurrent) primaryAccent else Color.White.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .clickable { inputName = suggestion }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = suggestion,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.75f),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }

                // ── Pinned Bottom Action Bar (NEVER CUT OFF) ──────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0E17))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (currentStep == 1) {
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(primaryAccent, secondaryAccent)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = "Next: Set Profile Name",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = Color.White.copy(alpha = 0.06f),
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Back",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                            }

                            Button(
                                onClick = { completeSetup() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                ),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(primaryAccent, secondaryAccent)
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = "Start Listening",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
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
        if (!sysDeviceName.isNullOrBlank() &&
            !sysDeviceName.contains("Redmi", ignoreCase = true) &&
            !sysDeviceName.contains(Build.MODEL, ignoreCase = true)
        ) {
            return@runCatching sysDeviceName
        }
        "Music Lover"
    }.getOrDefault("Music Lover")
}
