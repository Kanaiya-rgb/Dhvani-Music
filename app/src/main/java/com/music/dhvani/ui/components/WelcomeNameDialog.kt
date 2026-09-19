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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.data.settings.AppSettings

/**
 * Premium, responsive first-launch onboarding dialog.
 * Features a 2-step flow with a pinned bottom action bar so buttons are NEVER cut off,
 * a scrollable/paged avatar grid, pulsing hero glow, and animated shimmer accents.
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

    val categories = listOf("All", "3D Characters", "Music & Vibes", "Fun & Playful", "Neon Glyphs")
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

    // Intercept back presses in Step 2 to return to Step 1
    BackHandler(enabled = currentStep > 1) {
        currentStep = 1
    }

    // Continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeAnimations")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -250f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "headerShimmer",
    )

    val pulseGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlowScale",
    )
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlowAlpha",
    )

    // Mini music note wave heights
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1",
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 13f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2",
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3",
    )

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
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f),
                        )
                    ),
                    shape = RoundedCornerShape(28.dp),
                ),
            color = Color(0xFF13111A).copy(alpha = 0.98f),
            shadowElevation = 32.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Top Shimmer Gradient Header Strip ─────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    selectedPreset.colors.first().copy(alpha = 0.16f),
                                    Color.Transparent,
                                )
                            )
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFFD946EF),
                                        Color(0xFF06B6D4),
                                        Color(0xFF8B5CF6),
                                        Color(0xFF7C3AED),
                                    ),
                                    startX = shimmerOffset,
                                    endX = shimmerOffset + 600f,
                                    tileMode = TileMode.Repeated,
                                )
                            )
                    )

                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(wave1, wave2, wave3, wave2, wave1).forEachIndexed { idx, heightVal ->
                            val barColor = if (idx % 2 == 0) selectedPreset.colors.first() else selectedPreset.colors.last()
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(heightVal.dp)
                                    .clip(CircleShape)
                                    .background(barColor.copy(alpha = 0.85f))
                            )
                        }
                    }
                }

                // ── Top Header Section: Step Pill + Hero Avatar + Vibe Chip ───────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Step Counter Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        selectedPreset.colors.first().copy(alpha = 0.65f),
                                        selectedPreset.colors.last().copy(alpha = 0.35f),
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(selectedPreset.colors.first())
                            )
                            Text(
                                text = if (currentStep == 1) "1 of 2 • Choose your vibe" else "2 of 2 • Your name",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f),
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Hero Avatar Preview (76dp + Pulsing Glow)
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Ambient radial glow
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .scale(pulseGlowScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            selectedPreset.colors.first().copy(alpha = pulseGlowAlpha * 0.45f),
                                            selectedPreset.colors.last().copy(alpha = pulseGlowAlpha * 0.12f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                        )

                        // Pulsing sweep glow ring
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(pulseGlowScale)
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            selectedPreset.colors.first().copy(alpha = pulseGlowAlpha),
                                            selectedPreset.colors.last().copy(alpha = pulseGlowAlpha * 0.4f),
                                            selectedPreset.colors.first().copy(alpha = pulseGlowAlpha),
                                        )
                                    ),
                                    shape = CircleShape,
                                )
                        )

                        // Main 72dp Avatar Circle
                        Crossfade(
                            targetState = selectedPreset,
                            label = "AvatarCrossfade",
                        ) { preset ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.85f),
                                                Color.White.copy(alpha = 0.25f),
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
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    // Avatar Vibe Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 3.5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = selectedPreset.colors.first(),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = selectedPreset.name,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.95f),
                            )
                        }
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
                                    text = "Choose your music vibe & style",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(8.dp))

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
                                                .background(if (isCatSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                                                .border(
                                                    width = 0.8.dp,
                                                    color = if (isCatSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .clickable { selectedCategory = cat }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCatSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }

                                // 4-Column Responsive Grid (Takes remaining height, scrolls smoothly)
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
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
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
                                                    .background(Color.Black.copy(alpha = 0.32f)),
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
                            // STEP 2: NAME ENTRY & CONFIRMATION
                            // ═════════════════════════════════════════════════════
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "What should we call you?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Dhvani personalizes your playback profile, listening recaps, and greetings with this name.",
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )

                                Spacer(Modifier.height(26.dp))

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
                                            text = "Enter your name...",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = selectedPreset.colors.first(),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    },
                                    trailingIcon = {
                                        if (inputName.isNotBlank()) {
                                            IconButton(
                                                onClick = { inputName = "" },
                                                modifier = Modifier.size(24.dp),
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
                                        focusedBorderColor = selectedPreset.colors.first(),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        cursorColor = selectedPreset.colors.first(),
                                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                )
                            }
                        }
                    }
                }

                // ── Pinned Bottom Action Bar (GUARANTEED NEVER CUT OFF!) ──────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13111A))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (currentStep == 1) {
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
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
                                            colors = listOf(
                                                Color(0xFF7C3AED),
                                                Color(0xFFD946EF),
                                            )
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
                                        text = "Continue",
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
                                modifier = Modifier.height(50.dp),
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
                                    .height(50.dp),
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
                                                colors = listOf(
                                                    Color(0xFF7C3AED),
                                                    Color(0xFFD946EF),
                                                )
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
