package com.music.dhvani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.dhvani.data.settings.AppSettings
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.music.dhvani.R
import java.io.File

/**
 * Curated Preset Avatar definition supporting rich 3D illustrated avatars and neon gradient glyphs.
 */
data class PresetAvatar(
    val id: Int,
    val name: String,
    val colors: List<Color>,
    val icon: ImageVector = Icons.Rounded.Person,
    val drawableRes: Int? = null,
    val category: String = "3D Characters",
)

val PRESET_AVATARS = listOf(
    // ── 3D Premium Illustrated Avatars ──
    PresetAvatar(115, "Neon Panda", listOf(Color(0xFF06B6D4), Color(0xFFD946EF)), Icons.Rounded.Headphones, R.drawable.avatar_neon_panda, "Music & Vibes"),
    PresetAvatar(116, "Cyber Girl", listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)), Icons.Rounded.Headphones, R.drawable.avatar_cyber_girl, "3D Characters"),
    PresetAvatar(117, "Astro Beats", listOf(Color(0xFF38BDF8), Color(0xFF818CF8)), Icons.Rounded.AutoAwesome, R.drawable.avatar_astro_beats, "Cyber & Space"),
    PresetAvatar(118, "Shiba DJ", listOf(Color(0xFFF97316), Color(0xFFFBBF24)), Icons.Rounded.Headphones, R.drawable.avatar_shiba_dj, "Music & Vibes"),
    PresetAvatar(101, "Flower Girl", listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), Icons.Rounded.Person, R.drawable.avatar_flower_girl, "3D Characters"),
    PresetAvatar(102, "Burger Buddy", listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), Icons.Rounded.AutoAwesome, R.drawable.avatar_burger_buddy, "3D Characters"),
    PresetAvatar(103, "Purple Bass", listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)), Icons.Rounded.MusicNote, R.drawable.avatar_purple_guitar, "Music & Vibes"),
    PresetAvatar(104, "DJ Kitten", listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)), Icons.Rounded.Headphones, R.drawable.avatar_dj_cat, "Music & Vibes"),
    PresetAvatar(105, "Mixtape Stereo", listOf(Color(0xFF14B8A6), Color(0xFF06B6D4)), Icons.Rounded.Radio, R.drawable.avatar_boombox, "Music & Vibes"),
    PresetAvatar(106, "Hipster Lion", listOf(Color(0xFF10B981), Color(0xFF059669)), Icons.Rounded.Person, R.drawable.avatar_hipster_lion, "3D Characters"),
    PresetAvatar(107, "Rosy Boy", listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), Icons.Rounded.Person, R.drawable.avatar_rosy_boy, "3D Characters"),
    PresetAvatar(108, "Cosmic Saturn", listOf(Color(0xFFA855F7), Color(0xFF7C3AED)), Icons.Rounded.AutoAwesome, R.drawable.avatar_cosmic_planet, "Cyber & Space"),
    PresetAvatar(109, "Retro Polaroid", listOf(Color(0xFFFACC15), Color(0xFFF59E0B)), Icons.Rounded.AutoAwesome, R.drawable.avatar_retro_camera, "Fun & Playful"),
    PresetAvatar(110, "Woody Bot", listOf(Color(0xFF38BDF8), Color(0xFF0284C7)), Icons.Rounded.AutoAwesome, R.drawable.avatar_woody_bot, "3D Characters"),
    PresetAvatar(111, "Cyber Sneaker", listOf(Color(0xFFF97316), Color(0xFFEC4899)), Icons.Rounded.AutoAwesome, R.drawable.avatar_cyber_sneaker, "Fun & Playful"),
    PresetAvatar(112, "Happy Melon", listOf(Color(0xFF34D399), Color(0xFFF43F5E)), Icons.Rounded.AutoAwesome, R.drawable.avatar_happy_watermelon, "Fun & Playful"),
    PresetAvatar(113, "Red Microbus", listOf(Color(0xFF38BDF8), Color(0xFFEF4444)), Icons.Rounded.AutoAwesome, R.drawable.avatar_red_microbus, "Fun & Playful"),
    PresetAvatar(114, "Yellow Balloon", listOf(Color(0xFF93C5FD), Color(0xFFFBBF24)), Icons.Rounded.AutoAwesome, R.drawable.avatar_yellow_balloon, "Fun & Playful"),

    // ── Classic Neon Glyphs ──
    PresetAvatar(1, "Neon Beats", listOf(Color(0xFF9333EA), Color(0xFFEC4899)), Icons.Rounded.MusicNote, null, "Neon Glyphs"),
    PresetAvatar(2, "Cyber Wave", listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)), Icons.Rounded.Headphones, null, "Neon Glyphs"),
    PresetAvatar(3, "Golden Vinyl", listOf(Color(0xFFF59E0B), Color(0xFFEA580C)), Icons.Rounded.Album, null, "Neon Glyphs"),
    PresetAvatar(4, "Electro Spark", listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)), Icons.Rounded.AutoAwesome, null, "Neon Glyphs"),
    PresetAvatar(5, "Night Pulse", listOf(Color(0xFF6366F1), Color(0xFFA855F7)), Icons.Rounded.GraphicEq, null, "Neon Glyphs"),
    PresetAvatar(6, "Heart Strings", listOf(Color(0xFFE11D48), Color(0xFFF43F5E)), Icons.Rounded.Favorite, null, "Neon Glyphs"),
    PresetAvatar(7, "Radio Star", listOf(Color(0xFF10B981), Color(0xFF0284C7)), Icons.Rounded.Radio, null, "Neon Glyphs"),
    PresetAvatar(8, "Sunset Chill", listOf(Color(0xFFFF5722), Color(0xFFFF9800)), Icons.Rounded.MusicNote, null, "Neon Glyphs"),
)

fun getPresetAvatar(id: Int): PresetAvatar {
    return PRESET_AVATARS.firstOrNull { it.id == id } ?: PRESET_AVATARS[0]
}

/**
 * Universal User Avatar composable that renders:
 * 1. Custom photo from user's gallery (if uploaded and file exists)
 * 2. Selected curated neon preset avatar
 * 3. Fallback stylish initial letter of user's name
 */
@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    onClick: (() -> Unit)? = null,
) {
    val avatarType by AppSettings.userAvatarType.collectAsStateWithLifecycle()
    val presetId by AppSettings.userPresetAvatarId.collectAsStateWithLifecycle()
    val customPath by AppSettings.userCustomAvatarPath.collectAsStateWithLifecycle()
    val userName by AppSettings.userName.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val customFile = if (customPath.isNotBlank()) File(customPath) else null
    val hasValidCustomFile = customFile != null && customFile.exists() && customFile.length() > 0

    val clickableMod = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = clickableMod
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            // 1. Custom uploaded photo
            avatarType == "CUSTOM" && hasValidCustomFile -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(customFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            }

            // 2. Preset Avatars (3D Illustrated & Neon Glyphs)
            avatarType == "PRESET" -> {
                val preset = getPresetAvatar(presetId)
                if (preset.drawableRes != null) {
                    Image(
                        painter = painterResource(preset.drawableRes),
                        contentDescription = preset.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(Brush.linearGradient(preset.colors)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = preset.icon,
                            contentDescription = preset.name,
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.54f),
                        )
                    }
                }
            }

            // 3. Fallback: User Name Initial with Neon Gradient
            userName.isNotBlank() -> {
                val initial = userName.trim().first().uppercase()
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (size.value * 0.44f).sp,
                    )
                }
            }

            // 4. Default sleek person icon
            else -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.52f),
                    )
                }
            }
        }
    }
}
