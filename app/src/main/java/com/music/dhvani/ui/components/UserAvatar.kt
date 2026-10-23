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
    // ── Animals (from Nuvio catalog) ──
    PresetAvatar(1001, "Bram", listOf(Color(0xFFF45392), Color(0xFFF45392)), Icons.Rounded.Person, R.drawable.avatar_animals_bram, "Animals"),
    PresetAvatar(1002, "Clover", listOf(Color(0xFF7957BD), Color(0xFF7957BD)), Icons.Rounded.Person, R.drawable.avatar_animals_clover, "Animals"),
    PresetAvatar(1003, "Pip", listOf(Color(0xFF45B5EE), Color(0xFF45B5EE)), Icons.Rounded.Person, R.drawable.avatar_animals_pip, "Animals"),
    PresetAvatar(1004, "Otto", listOf(Color(0xFF0BB5AE), Color(0xFF0BB5AE)), Icons.Rounded.Person, R.drawable.avatar_animals_otto, "Animals"),
    PresetAvatar(1005, "Milo", listOf(Color(0xFFB9E53A), Color(0xFFB9E53A)), Icons.Rounded.Person, R.drawable.avatar_animals_milo, "Animals"),
    PresetAvatar(1006, "Miso", listOf(Color(0xFFB9E36A), Color(0xFFB9E36A)), Icons.Rounded.Person, R.drawable.avatar_animals_miso, "Animals"),
    PresetAvatar(1007, "Elio", listOf(Color(0xFFFFD949), Color(0xFFFFD949)), Icons.Rounded.Person, R.drawable.avatar_animals_elio, "Animals"),
    PresetAvatar(1008, "Finn", listOf(Color(0xFF88DDBB), Color(0xFF88DDBB)), Icons.Rounded.Person, R.drawable.avatar_animals_finn, "Animals"),
    PresetAvatar(1009, "Poppy", listOf(Color(0xFFF85A49), Color(0xFFF85A49)), Icons.Rounded.Person, R.drawable.avatar_animals_poppy, "Animals"),
    PresetAvatar(1010, "Bao", listOf(Color(0xFFFF7900), Color(0xFFFF7900)), Icons.Rounded.Person, R.drawable.avatar_animals_bao, "Animals"),

    // ── Minimalist Line Art ──
    PresetAvatar(1011, "Lin", listOf(Color(0xFF008080), Color(0xFF008080)), Icons.Rounded.Person, R.drawable.avatar_linear_lin, "Minimalist"),
    PresetAvatar(1012, "Max", listOf(Color(0xFF6B21A8), Color(0xFF6B21A8)), Icons.Rounded.Person, R.drawable.avatar_linear_max, "Minimalist"),
    PresetAvatar(1013, "Ava", listOf(Color(0xFFE11D48), Color(0xFFE11D48)), Icons.Rounded.Person, R.drawable.avatar_linear_ava, "Minimalist"),
    PresetAvatar(1014, "Theo", listOf(Color(0xFF1E3A5F), Color(0xFF1E3A5F)), Icons.Rounded.Person, R.drawable.avatar_linear_theo, "Minimalist"),
    PresetAvatar(1015, "Zara", listOf(Color(0xFFD97706), Color(0xFFD97706)), Icons.Rounded.Person, R.drawable.avatar_linear_zara, "Minimalist"),
    PresetAvatar(1016, "Kai", listOf(Color(0xFF065F46), Color(0xFF065F46)), Icons.Rounded.Person, R.drawable.avatar_linear_kai, "Minimalist"),
    PresetAvatar(1017, "Nova", listOf(Color(0xFFBE185D), Color(0xFFBE185D)), Icons.Rounded.Person, R.drawable.avatar_linear_nova, "Minimalist"),

    // ── Characters & Heroes ──
    PresetAvatar(1018, "Astro Nova", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_nova, "Characters"),
    PresetAvatar(1019, "Bolt", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_bolt, "Characters"),
    PresetAvatar(1020, "Marina", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_marina, "Characters"),
    PresetAvatar(1021, "Shadow", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_shadow, "Characters"),
    PresetAvatar(1022, "Rook", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_rook, "Characters"),
    PresetAvatar(1023, "Riff", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_riff, "Characters"),
    PresetAvatar(1024, "Clue", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_clue, "Characters"),
    PresetAvatar(1025, "Cedar", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_cedar, "Characters"),
    PresetAvatar(1026, "Ruby", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_ruby, "Characters"),
    PresetAvatar(1027, "Sage", listOf(Color(0xFF6F8EAA), Color(0xFF6F8EAA)), Icons.Rounded.Person, R.drawable.avatar_originals_sage, "Characters"),

    // ── Illustrated Portraits ──
    PresetAvatar(1028, "Quinn", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_quinn, "Portraits"),
    PresetAvatar(1029, "Iris", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_iris, "Portraits"),
    PresetAvatar(1030, "Ari", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_ari, "Portraits"),
    PresetAvatar(1031, "Hugo", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_hugo, "Portraits"),
    PresetAvatar(1032, "Skye", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_skye, "Portraits"),
    PresetAvatar(1033, "Maya", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_maya, "Portraits"),
    PresetAvatar(1034, "Drew", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_drew, "Portraits"),
    PresetAvatar(1035, "Leo", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_leo, "Portraits"),
    PresetAvatar(1036, "Zia", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_zia, "Portraits"),
    PresetAvatar(1037, "Elle", listOf(Color(0xFFE9E8F2), Color(0xFFE9E8F2)), Icons.Rounded.Person, R.drawable.avatar_portraits_elle, "Portraits"),

    // ── Sketches & Manga ──
    PresetAvatar(1038, "Rowan", listOf(Color(0xFF699B99), Color(0xFF699B99)), Icons.Rounded.Person, R.drawable.avatar_sketches_rowan, "Sketches"),
    PresetAvatar(1039, "Silas", listOf(Color(0xFFF37C2F), Color(0xFFF37C2F)), Icons.Rounded.Person, R.drawable.avatar_sketches_silas, "Sketches"),
    PresetAvatar(1040, "Nico", listOf(Color(0xFF9D3E68), Color(0xFF9D3E68)), Icons.Rounded.Person, R.drawable.avatar_sketches_nico, "Sketches"),
    PresetAvatar(1041, "Violet", listOf(Color(0xFFD47AC8), Color(0xFFD47AC8)), Icons.Rounded.Person, R.drawable.avatar_sketches_violet, "Sketches"),
    PresetAvatar(1042, "Moss", listOf(Color(0xFF8CB476), Color(0xFF8CB476)), Icons.Rounded.Person, R.drawable.avatar_sketches_moss, "Sketches"),
)

fun getPresetAvatar(id: Int): PresetAvatar {
    // 1. Direct match in current catalog (1001..1042)
    val exact = PRESET_AVATARS.firstOrNull { it.id == id }
    if (exact != null) return exact

    // 2. Backward compatibility for legacy neon presets (v2.3.0 IDs 1..8) & legacy defaults
    val mappedId = when (id) {
        1 -> 1001 // Neon Beats -> Bram
        2 -> 1003 // Cyber Wave -> Pip
        3 -> 1010 // Golden Vinyl -> Bao
        4 -> 1002 // Electro Spark -> Clover
        5 -> 1014 // Night Pulse -> Theo
        6 -> 1009 // Heart Strings -> Poppy
        7 -> 1004 // Radio Star -> Otto
        8 -> 1007 // Sunset Chill -> Elio
        else -> 1001 // Default fallback to Bram
    }
    return PRESET_AVATARS.firstOrNull { it.id == mappedId } ?: PRESET_AVATARS[0]
}

/**
 * Dedicated composable to render the inner content of a [PresetAvatar],
 * whether it is an illustrated drawable or a styled glyph.
 */
@Composable
fun PresetAvatarContent(
    preset: PresetAvatar,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (preset.drawableRes != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(preset.colors.first()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(preset.drawableRes),
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        }
    } else {
        Box(
            modifier = modifier
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

/**
 * Universal User Avatar composable that renders:
 * 1. An explicitly provided [PresetAvatar] if given
 * 2. Custom photo from user's gallery (if uploaded and file exists)
 * 3. Selected curated neon preset avatar from AppSettings
 * 4. Fallback stylish initial letter of user's name
 */
@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    preset: PresetAvatar? = null,
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
            // Explicitly passed preset (e.g. preview card, selection item)
            preset != null -> {
                PresetAvatarContent(preset = preset, size = size)
            }

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
                val currentPreset = getPresetAvatar(presetId)
                PresetAvatarContent(preset = currentPreset, size = size)
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
