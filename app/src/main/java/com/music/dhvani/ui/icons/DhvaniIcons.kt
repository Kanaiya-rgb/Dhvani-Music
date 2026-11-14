package com.music.dhvani.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Thick-stroke, round-capped icons in the spirit of Telegram's modern icon set.
 * Drawn as strokes (no fills) so the 2.2px weight + round joins read as a
 * single polished family. Tint is applied by [androidx.compose.material3.Icon].
 */
object DhvaniIcons {

    private const val STROKE = 2.2f
    private val stroke = SolidColor(Color.Black)

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_home",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 10f)
                lineTo(12f, 3f)
                lineTo(21f, 10f)
                lineTo(21f, 20f)
                lineTo(15f, 20f)
                lineTo(15f, 14f)
                lineTo(9f, 14f)
                lineTo(9f, 20f)
                lineTo(3f, 20f)
                close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_play",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = stroke,
            ) {
                moveTo(6.8f, 4.8f)
                lineTo(19.2f, 12f)
                lineTo(6.8f, 19.2f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_search",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Lens (full circle from two arcs)
                moveTo(4.6f, 11f)
                arcToRelative(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12.8f, 0f)
                arcToRelative(6.4f, 6.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12.8f, 0f)
                // Handle
                moveTo(15.9f, 15.9f)
                lineTo(20.4f, 20.4f)
            }
        }.build()
    }

    val Explore: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_explore",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Compass dial
                moveTo(3.4f, 12f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.2f, 0f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -17.2f, 0f)
                // Needle
                moveTo(15.4f, 8.6f)
                lineTo(13.6f, 13.6f)
                lineTo(8.6f, 15.4f)
                lineTo(10.4f, 10.4f)
                close()
            }
        }.build()
    }

    val Category: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_category",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Top-left
                moveTo(5f, 4f)
                lineTo(9f, 4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                lineTo(11f, 9f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                lineTo(5f, 11f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                lineTo(3f, 6f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()

                // Top-right
                moveTo(15f, 4f)
                lineTo(19f, 4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                lineTo(21f, 9f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                lineTo(15f, 11f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                lineTo(13f, 6f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()

                // Bottom-left
                moveTo(5f, 13f)
                lineTo(9f, 13f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                lineTo(11f, 18f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                lineTo(5f, 20f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                lineTo(3f, 15f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()

                // Bottom-right
                moveTo(15f, 13f)
                lineTo(19f, 13f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                lineTo(21f, 18f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                lineTo(15f, 20f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                lineTo(13f, 15f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()
            }
        }.build()
    }

    val Shuffle: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_shuffle",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Strand that crosses downwards, with its arrow head
                moveTo(3.4f, 7.4f); lineTo(7f, 7.4f); lineTo(16.6f, 16.6f); lineTo(20.6f, 16.6f)
                moveTo(18.1f, 14.1f); lineTo(20.6f, 16.6f); lineTo(18.1f, 19.1f)
                // Strand that crosses upwards, broken around the intersection
                moveTo(3.4f, 16.6f); lineTo(7f, 16.6f); lineTo(9.8f, 13.9f)
                moveTo(13.9f, 10.1f); lineTo(16.6f, 7.4f); lineTo(20.6f, 7.4f)
                moveTo(18.1f, 4.9f); lineTo(20.6f, 7.4f); lineTo(18.1f, 9.9f)
            }
        }.build()
    }

    val Repeat: ImageVector by lazy { repeatLoop("dhvani_repeat") }

    /**
     * Two straight runs joined by semicircles, with the arrow heads lying flat
     * at the ends of the straights. Putting them on the curves instead — as a
     * first pass did — makes the glyph read as a refresh/sync symbol.
     */
    private fun repeatLoop(name: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8.6f, 7.6f)
                lineTo(15.4f, 7.6f)
                arcToRelative(4.4f, 4.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 8.8f)
                lineTo(8.6f, 16.4f)
                arcToRelative(4.4f, 4.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -8.8f)
                close()
                // Direction of travel: right along the top, left along the bottom.
                moveTo(13.5f, 5.7f); lineTo(15.4f, 7.6f); lineTo(13.5f, 9.5f)
                moveTo(10.5f, 14.5f); lineTo(8.6f, 16.4f); lineTo(10.5f, 18.3f)
            }
        }.build()

    /** AutoPlay's lemniscate. */
    val Infinity: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_infinity",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 12f)
                curveTo(10.1f, 9.1f, 8.7f, 8f, 7.1f, 8f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 8f)
                curveTo(8.7f, 16f, 10.1f, 14.9f, 12f, 12f)
                curveTo(13.9f, 9.1f, 15.3f, 8f, 16.9f, 8f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 8f)
                curveTo(15.3f, 16f, 13.9f, 14.9f, 12f, 12f)
            }
        }.build()
    }

    /** Beamed pair of notes, for instrumental stretches in the lyrics. */
    val MusicNote: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_music_note",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Heads are solid; stems and beam keep the family's stroke weight.
            path(fill = stroke) {
                moveTo(4.2f, 17.7f)
                arcToRelative(2.9f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5.8f, 0f)
                arcToRelative(2.9f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5.8f, 0f)
                close()
                moveTo(14.2f, 15.9f)
                arcToRelative(2.9f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5.8f, 0f)
                arcToRelative(2.9f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -5.8f, 0f)
                close()
            }
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 17.7f); lineTo(10f, 6.7f)
                moveTo(20f, 15.9f); lineTo(20f, 4.9f)
                moveTo(10f, 6.7f); lineTo(20f, 4.9f)
            }
        }.build()
    }

    /** Apple Music Style Speech Bubble with stylized double quotation marks for Lyrics. */
    val Lyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_lyrics_quote",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Speech bubble outline
                moveTo(6.5f, 4.5f)
                lineTo(17.5f, 4.5f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 3f)
                lineTo(20.5f, 13.5f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3f, 3f)
                lineTo(10.8f, 16.5f)
                lineTo(6.6f, 19.8f)
                lineTo(6.6f, 16.5f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -3.1f, -3f)
                lineTo(3.5f, 7.5f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, -3f)
                close()

                // Left quotation mark “
                moveTo(9.2f, 8.8f)
                arcToRelative(1.4f, 1.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, 2.6f)
                lineTo(8.2f, 12.8f)

                // Right quotation mark ”
                moveTo(14.8f, 8.8f)
                arcToRelative(1.4f, 1.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, 2.6f)
                lineTo(13.8f, 12.8f)
            }
        }.build()
    }

    /** Plain chevron — a disclosure hint, not a directional arrow. */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_chevron_right",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.5f, 6.2f)
                lineTo(15.3f, 12f)
                lineTo(9.5f, 17.8f)
            }
        }.build()
    }

    /**
     * The player's like control, in two weights.
     *
     * Filled rather than merely tinted when set: the player draws every glyph
     * white on artwork, where a colour change alone is the one signal the
     * backdrop can swallow. A shape change survives any album cover.
     */
    val Heart: ImageVector by lazy { heart("dhvani_heart", filled = false) }

    val HeartFilled: ImageVector by lazy { heart("dhvani_heart_filled", filled = true) }

    private fun heart(name: String, filled: Boolean): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Sleek, standalone 5-pointed star: outlined when unliked, solid filled when liked
            path(
                stroke = stroke,
                strokeLineWidth = if (filled) 1.2f else 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = if (filled) stroke else null,
            ) {
                moveTo(12.0f, 3.2f)
                lineTo(14.65f, 8.58f)
                lineTo(20.59f, 9.44f)
                lineTo(16.29f, 13.63f)
                lineTo(17.31f, 19.55f)
                lineTo(12.0f, 16.76f)
                lineTo(6.69f, 19.55f)
                lineTo(7.71f, 13.63f)
                lineTo(3.41f, 9.44f)
                lineTo(9.35f, 8.58f)
                close()
            }
        }.build()

    /** Adding something — a new playlist, on the library shelf. */
    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_plus",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }.build()
    }

    /**
     * A tick — the other half of [Plus]. Saving something to the library swaps
     * one for the other in place, so the two are drawn on the same 14-unit span
     * and at the same weight; a tick sized to its own bounding box would jump.
     */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_check",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.8f); lineTo(9.6f, 17.4f); lineTo(19f, 6.9f)
            }
        }.build()
    }

    /** Arrow pointing down into a tray — offline download. */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_download",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Vertical stem
                moveTo(12f, 4.5f); lineTo(12f, 15.5f)
                // Arrow head
                moveTo(7.5f, 11f); lineTo(12f, 15.5f); lineTo(16.5f, 11f)
                // Tray base
                moveTo(4.5f, 18f); lineTo(19.5f, 18f)
            }
        }.build()
    }

    /**
     * Clock face with two hands — a download asked for but not yet on disk.
     *
     * The dial is the same 8.6 radius as [Explore]'s, so the two sit at the same
     * optical weight when they appear in the same row of header buttons. The
     * hands are one polyline through the centre rather than two strokes: the
     * round join is then the pivot, which is what stops the middle reading as a
     * pair of lines that happen to cross.
     */
    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_clock",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Dial (full circle from two arcs)
                moveTo(3.4f, 12f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 17.2f, 0f)
                arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -17.2f, 0f)
                // Minute hand up, hour hand down to the right
                moveTo(12f, 7.4f); lineTo(12f, 12f); lineTo(15.4f, 13.8f)
            }
        }.build()
    }

    /**
     * A pushpin, not a map marker: a round head with a straight needle
     * kicked out to one side, the way it sits once it's stuck into something.
     * A teardrop reads as "location" the moment it's this small — the two
     * disjoint strokes here are what keep it read as "pinned" instead.
     */
    val Pin: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_pin",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Round head.
                moveTo(9.6f, 8.4f)
                arcToRelative(3.4f, 3.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.8f, 0f)
                arcToRelative(3.4f, 3.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6.8f, 0f)
                // Needle, kicked out from the underside of the head to a point.
                moveTo(10.8f, 10.8f)
                lineTo(5.6f, 19.6f)
            }
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_library",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Three upright spines + one leaning — Apple Music's library glyph, thickened
                moveTo(4.6f, 4.8f); lineTo(4.6f, 19.2f)
                moveTo(9.2f, 4.8f); lineTo(9.2f, 19.2f)
                moveTo(13.8f, 4.8f); lineTo(13.8f, 19.2f)
                moveTo(17.2f, 5.6f); lineTo(20.6f, 18.9f)
            }
        }.build()
    }

    val Translate: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_translate",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2f, 5f); lineTo(14f, 5f)
                moveTo(8f, 2f); lineTo(8f, 5f)
                moveTo(4f, 7f)
                curveTo(5.5f, 10.5f, 7.5f, 13.5f, 11f, 15f)
                moveTo(12f, 7f)
                curveTo(10.5f, 10.5f, 8.5f, 13.5f, 5f, 15f)
                moveTo(14f, 20f); lineTo(18f, 9f); lineTo(22f, 20f)
                moveTo(15.5f, 16.5f); lineTo(20.5f, 16.5f)
            }
        }.build()
    }

    /** Notification Bell icon matching Stitch mockup. */
    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_bell",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 16f)
                lineTo(18f, 11f)
                arcToRelative(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, -12f, 0f)
                lineTo(6f, 16f)
                lineTo(4f, 18f)
                lineTo(20f, 18f)
                close()
                moveTo(10.3f, 21f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.4f, 0f)
            }
        }.build()
    }

    /** Utsav celebration popper/sparkle icon */
    val Utsav: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_utsav",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Celebration party cone
                moveTo(3f, 21f)
                lineTo(9.5f, 14.5f)
                lineTo(14.5f, 19.5f)
                close()
                // Sparkles / bursts
                moveTo(16f, 8f); lineTo(19f, 5f)
                moveTo(12f, 5f); lineTo(12f, 2f)
                moveTo(19f, 12f); lineTo(22f, 12f)
                moveTo(14f, 11f); lineTo(16f, 10f)
                moveTo(10f, 7f); lineTo(9f, 5f)
            }
        }.build()
    }

    /** Radio boombox / wave receiver icon */
    val Radio: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_radio",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Antenna
                moveTo(3.5f, 3.5f); lineTo(8.5f, 7f)
                // Radio body
                moveTo(3f, 7f)
                lineTo(21f, 7f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 2f)
                lineTo(23f, 19f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                lineTo(3f, 21f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                lineTo(1f, 9f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()
                // Speaker circle
                moveTo(8.5f, 14f)
                arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 7f, 0f)
                arcToRelative(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -7f, 0f)
            }
        }.build()
    }

    val Equalizer: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_equalizer",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Bar 1
                moveTo(5f, 18f); lineTo(5f, 10f)
                // Bar 2
                moveTo(12f, 18f); lineTo(12f, 5f)
                // Bar 3
                moveTo(19f, 18f); lineTo(19f, 13f)
            }
        }.build()
    }

    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_mic",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    MIC_PATH
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    val Speed: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_speed",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 18f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 0f)
                moveTo(12f, 13f); lineTo(16f, 9f)
            }
        }.build()
    }

    val Notes: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_notes",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 4f); lineTo(20f, 4f)
                moveTo(4f, 10f); lineTo(16f, 10f)
                moveTo(4f, 16f); lineTo(12f, 16f)
            }
        }.build()
    }

    /** Smooth, rounded video camera icon with lens */
    val Video: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_video",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Rounded camera body
                moveTo(5f, 6.5f)
                lineTo(13f, 6.5f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2.5f, 2.5f)
                lineTo(15.5f, 15f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2.5f, 2.5f)
                lineTo(5f, 17.5f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2.5f, -2.5f)
                lineTo(2.5f, 9f)
                arcToRelative(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2.5f, -2.5f)
                close()
                // Lens funnel
                moveTo(15.5f, 10f)
                lineTo(20f, 7.5f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1.5f, 0.9f)
                lineTo(21.5f, 15.6f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.5f, 0.9f)
                lineTo(15.5f, 14f)
            }
        }.build()
    }

    /** Vinyl record / album cover icon representing static artwork */
    val Cover: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_cover",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Outer vinyl disc edge
                moveTo(3f, 12f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, -18f, 0f)
                // Inner center label circle
                moveTo(9f, 12f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
                // Center spindle hole dot
                moveTo(12f, 11.5f)
                lineTo(12f, 12.5f)
            }
        }.build()
    }

    /** YouTube play button icon with authentic red badge and white play triangle */
    val YouTube: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_youtube",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFF0000)),
            ) {
                moveTo(21.58f, 7.19f)
                arcToRelative(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.97f, -1.98f)
                curveTo(17.87f, 4.75f, 12f, 4.75f, 12f, 4.75f)
                curveTo(12f, 4.75f, 6.13f, 4.75f, 4.39f, 5.21f)
                arcToRelative(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.97f, 1.98f)
                curveTo(2f, 8.93f, 2f, 12f, 2f, 12f)
                curveTo(2f, 12f, 2f, 15.07f, 2.42f, 16.81f)
                arcToRelative(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.97f, 1.98f)
                curveTo(6.13f, 19.25f, 12f, 19.25f, 12f, 19.25f)
                curveTo(12f, 19.25f, 17.87f, 19.25f, 19.61f, 18.79f)
                arcToRelative(2.8f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.97f, -1.98f)
                curveTo(22f, 15.07f, 22f, 12f, 22f, 12f)
                curveTo(22f, 12f, 22f, 8.93f, 21.58f, 7.19f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
            ) {
                moveTo(10f, 15.5f)
                lineTo(15.5f, 12f)
                lineTo(10f, 8.5f)
                close()
            }
        }.build()
    }

    /** Spotify icon with authentic green circle and sound waves */
    val Spotify: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_spotify",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF1DB954)),
            ) {
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
                close()
            }
            // Top wave
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(6.8f, 9.6f)
                curveTo(10.2f, 8.3f, 14.2f, 8.5f, 17.2f, 10.3f)
            }
            // Middle wave
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(7.4f, 12.6f)
                curveTo(10.2f, 11.6f, 13.8f, 11.8f, 16.5f, 13.3f)
            }
            // Bottom wave
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(8.0f, 15.6f)
                curveTo(10.4f, 14.8f, 13.4f, 15f, 15.7f, 16.2f)
            }
        }.build()
    }

    /** Apple Music icon with authentic red badge and white double musical note */
    val AppleMusic: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_apple_music",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Background circle
            path(
                fill = SolidColor(Color(0xFFFA2D48)),
            ) {
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
                close()
            }
            // Apple Music Double Note
            path(
                fill = SolidColor(Color.White),
            ) {
                // Top connecting beam and vertical stems
                moveTo(16.5f, 6.2f)
                lineTo(10.5f, 7.8f)
                curveTo(9.9f, 8.0f, 9.5f, 8.5f, 9.5f, 9.1f)
                lineTo(9.5f, 15.2f)
                // Left note head
                curveTo(9.1f, 14.9f, 8.5f, 14.7f, 7.8f, 14.7f)
                curveTo(6.3f, 14.7f, 5.0f, 15.6f, 5.0f, 16.8f)
                curveTo(5.0f, 18.0f, 6.3f, 19.0f, 7.8f, 19.0f)
                curveTo(9.4f, 19.0f, 10.7f, 18.0f, 10.7f, 16.8f)
                lineTo(10.7f, 10.6f)
                lineTo(15.3f, 9.4f)
                lineTo(15.3f, 13.7f)
                // Right note head
                curveTo(14.9f, 13.4f, 14.3f, 13.2f, 13.6f, 13.2f)
                curveTo(12.1f, 13.2f, 10.8f, 14.1f, 10.8f, 15.3f)
                curveTo(10.8f, 16.5f, 12.1f, 17.5f, 13.6f, 17.5f)
                curveTo(15.2f, 17.5f, 16.5f, 16.5f, 16.5f, 15.3f)
                lineTo(16.5f, 6.2f)
                close()
            }
        }.build()
    }

    /** TIDAL icon with authentic dark badge and cyan 4-diamond mark */
    val Tidal: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_tidal",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Background circle
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
                close()
            }
            // 4 TIDAL Diamonds
            path(
                fill = SolidColor(Color(0xFF00E5FF)),
            ) {
                // Diamond 1 (Top-Left)
                moveTo(3.9f, 9.5f)
                lineTo(6.6f, 6.8f)
                lineTo(9.3f, 9.5f)
                lineTo(6.6f, 12.2f)
                close()

                // Diamond 2 (Top-Middle)
                moveTo(9.3f, 9.5f)
                lineTo(12.0f, 6.8f)
                lineTo(14.7f, 9.5f)
                lineTo(12.0f, 12.2f)
                close()

                // Diamond 3 (Top-Right)
                moveTo(14.7f, 9.5f)
                lineTo(17.4f, 6.8f)
                lineTo(20.1f, 9.5f)
                lineTo(17.4f, 12.2f)
                close()

                // Diamond 4 (Bottom-Middle)
                moveTo(9.3f, 14.9f)
                lineTo(12.0f, 12.2f)
                lineTo(14.7f, 14.9f)
                lineTo(12.0f, 17.6f)
                close()
            }
        }.build()
    }

    /** Community icon with purple badge and clean multi-user group silhouette */
    val Community: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_community",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            // Background circle
            path(
                fill = SolidColor(Color(0xFFA78BFA)),
            ) {
                moveTo(2f, 12f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 0f)
                arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, -20f, 0f)
                close()
            }
            // Clean Community People silhouette in crisp white
            path(
                fill = SolidColor(Color.White),
            ) {
                // Primary center user head
                moveTo(12f, 8.5f)
                arcToRelative(2.2f, 2.2f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, -4.4f)
                arcToRelative(2.2f, 2.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 4.4f)
                close()

                // Primary center user body / shoulders
                moveTo(12f, 10.2f)
                curveTo(10.2f, 10.2f, 7.8f, 11.1f, 7.8f, 12.8f)
                lineTo(7.8f, 14f)
                lineTo(16.2f, 14f)
                lineTo(16.2f, 12.8f)
                curveTo(16.2f, 11.1f, 13.8f, 10.2f, 12f, 10.2f)
                close()

                // Left secondary user head
                moveTo(7.5f, 9.8f)
                arcToRelative(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, -3.4f)
                arcToRelative(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 3.4f)
                close()

                // Left secondary user body
                moveTo(6.5f, 11.8f)
                curveTo(5.3f, 12.2f, 4.5f, 13.0f, 4.5f, 14.2f)
                lineTo(4.5f, 15f)
                lineTo(7.2f, 15f)
                lineTo(7.2f, 13.8f)
                curveTo(7.2f, 12.9f, 7.6f, 12.1f, 8.2f, 11.5f)
                curveTo(7.6f, 11.5f, 7.0f, 11.6f, 6.5f, 11.8f)
                close()

                // Right secondary user head
                moveTo(16.5f, 9.8f)
                arcToRelative(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0f, -3.4f)
                arcToRelative(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 3.4f)
                close()

                // Right secondary user body
                moveTo(17.5f, 11.8f)
                curveTo(17.0f, 11.6f, 16.4f, 11.5f, 15.8f, 11.5f)
                curveTo(16.4f, 12.1f, 16.8f, 12.9f, 16.8f, 13.8f)
                lineTo(16.8f, 15f)
                lineTo(19.5f, 15f)
                lineTo(19.5f, 14.2f)
                curveTo(19.5f, 13.0f, 18.7f, 12.2f, 17.5f, 11.8f)
                close()
            }
        }.build()
    }

    val Drums: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_drums",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Drumsticks crossed above
                moveTo(5.5f, 4.5f); lineTo(14.5f, 9.5f)
                moveTo(18.5f, 4.5f); lineTo(9.5f, 9.5f)

                // Snare top head rim (ellipse)
                moveTo(4.5f, 11f)
                arcToRelative(7.5f, 2.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 15f, 0f)
                arcToRelative(7.5f, 2.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, -15f, 0f)

                // Drum shell sides and bottom rim
                moveTo(4.5f, 11f); lineTo(4.5f, 17f)
                arcToRelative(7.5f, 2.8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15f, 0f)
                lineTo(19.5f, 11f)

                // Tension lugs / vertical struts
                moveTo(8.5f, 13.5f); lineTo(8.5f, 19.2f)
                moveTo(12f, 13.8f); lineTo(12f, 19.8f)
                moveTo(15.5f, 13.5f); lineTo(15.5f, 19.2f)
            }
        }.build()
    }

    val Guitar: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_guitar",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Guitar Pick (Plectrum outline)
                moveTo(6.5f, 5.5f)
                curveTo(10f, 4f, 14f, 4f, 17.5f, 5.5f)
                curveTo(19.5f, 6.5f, 19.5f, 10f, 18f, 13.5f)
                curveTo(16.5f, 16.5f, 14f, 19.5f, 12f, 21f)
                curveTo(10f, 19.5f, 7.5f, 16.5f, 6f, 13.5f)
                curveTo(4.5f, 10f, 4.5f, 6.5f, 6.5f, 5.5f)
                close()

                // 3 Guitar Strings through pick
                moveTo(9.5f, 8.5f); lineTo(9.5f, 14.5f)
                moveTo(12f, 8f); lineTo(12f, 17f)
                moveTo(14.5f, 8.5f); lineTo(14.5f, 14.5f)
            }
        }.build()
    }

    val Bass: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_bass",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Outer Subwoofer Rim
                moveTo(3f, 12f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 0f)
                arcToRelative(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, -18f, 0f)

                // Inner Woofer Cone
                moveTo(6.5f, 12f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 0f)
                arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -11f, 0f)

                // Center Dust Cap (Bass Dome)
                moveTo(10f, 12f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, -4f, 0f)
            }
        }.build()
    }

    /** Standard fidelity audio — simple, clean 3-bar audio waveform */
    val QualityStandard: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_quality_standard",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 9.5f); lineTo(6.5f, 14.5f)
                moveTo(12f, 6f); lineTo(12f, 18f)
                moveTo(17.5f, 9.5f); lineTo(17.5f, 14.5f)
            }
        }.build()
    }

    /** High Quality audio — dynamic 5-band frequency waveform */
    val QualityHigh: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_quality_high",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 10.5f); lineTo(4f, 13.5f)
                moveTo(8f, 7f); lineTo(8f, 17f)
                moveTo(12f, 4.5f); lineTo(12f, 19.5f)
                moveTo(16f, 7f); lineTo(16f, 17f)
                moveTo(20f, 10.5f); lineTo(20f, 13.5f)
            }
        }.build()
    }

    /** Apple Lossless logo SVG (Lossless audio) */
    val QualityLossless: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_quality_lossless",
            defaultWidth = 20.dp, defaultHeight = 12.dp,
            viewportWidth = 15f, viewportHeight = 9f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M8.184,0.35C9.944,0.35 10.703,3.296 11.338,5.238C11.673,3.842 11.497,3.542 11.857,3.542C11.99,3.542 12.126,3.633 12.126,3.798C12.126,3.809 12.123,3.839 12.117,3.883L12.091,4.058C12.02,4.522 11.845,5.494 11.654,6.144C13.198,10.191 14.345,4.861 14.474,3.772C14.493,3.615 14.612,3.542 14.731,3.542C14.891,3.542 15.022,3.662 14.997,3.843C14.72,5.605 14.295,8.35 12.547,8.35C11.582,8.35 11.04,7.595 10.611,6.73C9.54,4.626 9.047,1.093 7.997,1.093C7.66,1.093 7.411,1.444 7.394,1.444C7.362,1.444 7.337,1.301 7.023,0.909C7.322,0.567 7.734,0.35 8.184,0.35ZM2.458,0.354C5.211,0.354 5.456,7.618 7.014,7.618C7.197,7.618 7.394,7.507 7.61,7.256C7.729,7.458 7.851,7.638 7.978,7.796C7.667,8.151 7.28,8.35 6.795,8.35C5.054,8.349 4.306,5.434 3.663,3.466C3.511,4.097 3.432,4.669 3.402,4.925C3.382,5.088 3.263,5.163 3.143,5.163C3.009,5.163 2.874,5.071 2.874,4.908L2.874,4.908L2.877,4.87C2.966,4.223 3.146,3.243 3.347,2.56C3.079,1.858 2.745,1.091 2.252,1.091C1.257,1.091 0.687,3.591 0.527,4.925C0.508,5.088 0.388,5.163 0.268,5.163C0.135,5.163 0,5.071 0,4.908C0,4.896 0.001,4.883 0.002,4.87C0.283,2.836 0.808,0.354 2.458,0.354ZM5.315,0.35C5.809,0.35 6.339,0.608 6.797,1.211C6.822,1.241 7.078,1.639 7.159,1.777C8.277,3.802 8.818,7.627 9.881,7.627C10.065,7.627 10.264,7.513 10.484,7.256C10.604,7.458 10.726,7.638 10.852,7.796C10.542,8.15 10.155,8.35 9.67,8.35C6.933,8.349 6.636,1.09 5.128,1.09C4.788,1.09 4.536,1.444 4.519,1.444C4.487,1.444 4.462,1.301 4.148,0.909C4.455,0.558 4.87,0.35 5.315,0.35Z"
                ).toNodes(),
                fill = stroke,
            )
        }.build()
    }

    /** Apple Lossless logo SVG (Hi-Res Lossless audio) */
    val QualityHiRes: ImageVector by lazy {
        QualityLossless
    }

    // --- Additional device, action & media icons ---
    val QualityDolbyAtmos: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_quality_dolby_atmos",
            defaultWidth = 17.dp, defaultHeight = 12.dp,
            viewportWidth = 103f, viewportHeight = 74f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M0.06,72.72C0.06,48.82 0.06,24.92 0.06,1.02C10.99,0.29 22.44,0.58 31.63,7.21C60.68,28.18 45.74,73 10.17,73C6.93,73 3.2,73.51 0.06,72.72ZM102.83,1.05C102.83,24.98 102.83,48.91 102.83,72.83C95.29,73.72 86.47,73.53 79.34,70.82C54.93,61.51 49.09,31.94 65.33,12.5C74.04,2.07 89.91,-1.06 102.83,1.05Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Official Dolby Atmos Horizontal logo SVG (Double-D + Dolby Atmos wordmark) */
    val QualityDolbyAtmosHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_quality_dolby_atmos_horizontal",
            defaultWidth = 84.dp, defaultHeight = 12.dp,
            viewportWidth = 665f, viewportHeight = 95f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M0.06 72.72C0.06 48.82 0.06 24.92 0.06 1.02C10.99 0.29 22.44 0.58 31.63 7.21C60.68 28.18 45.74 73 10.17 73C6.93 73 3.2 73.51 0.06 72.72ZM102.83 1.05C102.83 24.98 102.83 48.91 102.83 72.83C95.29 73.72 86.47 73.53 79.34 70.82C54.93 61.51 49.09 31.94 65.33 12.5C74.04 2.07 89.91 -1.06 102.83 1.05ZM124.49 72.51C124.49 48.73 124.49 24.95 124.49 1.17C130.14 -0.61 140.94 0.83 147.17 0.83C156.42 0.83 165.18 2.36 172.62 8.22C189.68 21.65 191.7 49.07 174.88 63.72C165.4 71.98 155.02 73 142.83 73C138.79 73 127.62 74.22 124.49 72.51ZM259.99 0.83C259.99 24.94 259.99 49.06 259.99 73.17C256.33 73.17 252.67 73.17 249.01 73.17C249.01 49.06 249.01 24.94 249.01 0.83C252.67 0.83 256.33 0.83 259.99 0.83ZM279.83 0.84C280.06 9.49 280.28 18.14 280.5 26.8C285.87 24.63 290.17 22.78 296.17 23C325.79 24.07 326.78 70.72 297.5 72.98C291.11 73.48 286.13 71.89 280.5 69.23C280.32 70.54 280.13 71.85 279.95 73.15C276.3 73.15 272.66 73.15 269.01 73.15C269.01 49.05 269.01 24.94 269.01 0.84C272.62 0.84 276.22 0.84 279.83 0.84ZM455.83 72.5C453.72 72.7 451.61 72.89 449.5 73.09C446.61 69.41 443.85 56.24 441.02 54.15C439.43 52.97 434.79 53.83 432.83 53.83C425.61 53.83 418.39 53.83 411.17 53.83C409.19 53.83 404.02 52.89 402.55 54.37C399.83 57.11 397.33 69.04 394.5 73.05C392.36 72.87 390.21 72.68 388.07 72.5C398.55 48.63 409.02 24.75 419.5 0.88C421.02 0.8 424.02 0.23 425.27 1.22C426.78 2.4 431.05 14.64 432.21 17.27C440.29 35.55 446.84 54.63 455.83 72.5ZM469.82 0.88C469.82 8.09 469.82 15.29 469.82 22.5C474.27 22.83 478.72 23.16 483.17 23.49C483.17 24.83 483.17 26.17 483.17 27.51C478.73 27.84 474.28 28.17 469.83 28.5C469.83 37.17 469.83 45.83 469.83 54.5C469.83 58.1 469.02 62.75 471.34 65.81C473.99 69.31 479.05 67.93 482.5 66.93C482.71 68.45 482.91 69.98 483.12 71.5C478.36 75.24 467.59 73.32 465.15 67.4C461.38 58.23 465.36 38.75 463.83 28.01C460.57 28.01 457.31 28.01 454.04 28.01C454.04 26.34 454.04 24.67 454.04 23C457.19 23 460.35 23 463.5 23C463.68 15.63 463.87 8.25 464.05 0.88C465.97 0.88 467.9 0.88 469.82 0.88ZM422.5 8.51C420 9.36 418.85 15.57 417.77 17.92C413.34 27.56 410.36 38.2 405.23 47.5C406.9 48.91 410.68 48.17 412.83 48.17C419.06 48.17 425.28 48.17 431.5 48.17C433.6 48.17 437.01 48.85 438.69 47.5C433.3 34.5 427.9 21.51 422.5 8.51ZM135.51 61.5C138.31 63.07 143.96 62 147.17 62C153.85 62 160.19 60.87 165.62 56.78C186.14 41.32 173.11 12 148.5 12C144.98 12 138.57 10.79 135.51 12.5C135.51 28.83 135.51 45.17 135.51 61.5ZM213.85 23.1C245.88 19.13 252.83 67.32 221.4 72.84C188.05 78.7 180.64 27.22 213.85 23.1ZM332.72 94.94C330.91 94.94 329.09 94.94 327.28 94.94C324.95 93.8 322.63 92.65 320.3 91.5C321.7 88.43 323.1 85.35 324.5 82.28C326.51 81.69 328.06 83.52 330.22 82.76C335.32 80.96 336.65 72.85 338.83 68.5C332.19 53.5 325.56 38.5 318.93 23.5C322.88 23.39 326.83 23.28 330.79 23.17C334.44 28.5 338.5 39.02 340.89 45.24C341.67 47.27 343.33 53.27 345.5 53.64C350.07 43.48 354.64 33.32 359.21 23.17C363.17 23.28 367.12 23.39 371.07 23.5C369.29 30.82 363.42 40.55 360.22 47.74C354.43 60.75 349.57 74.7 342.7 87.2C341.53 89.33 339.47 91.63 337.34 92.83C336.17 93.49 333.28 93.86 332.72 94.94ZM497.91 23.07C498.44 26.05 498.97 29.02 499.5 32C504.46 24.06 515.1 19.95 523.63 25.2C526.8 27.15 527.68 31.61 530.5 33.25C532.17 30.56 533.71 28.08 536.32 26.16C543.8 20.67 556.94 21.96 561.88 30.27C566.66 38.31 565 49.19 565 58.17C565 61.22 565.93 71.01 564.51 73.15C562.84 73.15 561.17 73.15 559.5 73.15C558.66 70.56 559.17 67.23 559.17 64.5C559.17 53.26 563.26 28.19 546.4 27.83C528.6 27.46 531.83 52.82 531.83 64.17C531.83 66.99 532.37 70.47 531.5 73.15C529.83 73.15 528.16 73.15 526.49 73.15C525.08 71.02 526 66.08 526 63.5C526 52.85 530.07 28.48 514.4 27.84C496.88 27.12 499 51.34 499 62.5C499 65.28 500.03 70.85 498.51 73.15C496.72 73.15 494.93 73.15 493.14 73.15C493.14 56.45 493.14 39.76 493.14 23.07C494.73 23.07 496.32 23.07 497.91 23.07ZM594.85 23.11C603.48 22.28 611.98 24.83 617.8 31.37C630.41 45.53 621.65 69.27 603.6 72.81C599.82 73.56 595.16 73.49 591.5 72.32C565.42 63.98 567.87 25.73 594.85 23.11ZM663 31.17C661.83 32.12 660.67 33.08 659.5 34.04C657.3 33.6 655.52 30.31 653.28 29.21C647.36 26.31 636.04 28.01 636.52 36.5C636.81 41.61 641.51 43.02 645.68 44.17C653.08 46.2 663.62 48.05 664.17 57.6C665.23 76.23 637.26 77.64 628.96 64.5C630.47 63.37 631.99 62.24 633.5 61.11C635.51 63.33 637.48 65.64 640.31 66.84C646.74 69.57 658.49 58.83 658.49 58.83C658.17 53.39 652.75 52.05 648.5 50.69C639.24 47.72 628.82 46.07 631.15 33.27C633.06 22.76 648.09 21.02 656.02 24.81C658.49 25.99 662.53 28.18 663 31.17ZM595.86 28.1C570.74 31.24 575.52 72.04 601.58 67.81C625.68 63.89 621.13 24.95 595.86 28.1ZM214.87 33.13C195.65 35.67 199.89 65.73 219.17 62.74C238.39 59.76 234.19 30.58 214.87 33.13ZM292.24 33.14C273.07 35.61 277.17 65.78 296.5 62.75C315.59 59.75 311.5 30.67 292.24 33.14Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Dual in-ear stem AirPods Pro style vector (from airpods-pro-icon-4097161-512.svg) */
    val Earbuds: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_earbuds",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            group(
                name = "earbuds_scaled",
                rotate = 0f,
                pivotX = 252.6f,
                pivotY = 247.8f,
                scaleX = 2.2f,
                scaleY = 2.2f,
                translationX = 0f,
                translationY = 0f,
                clipPathData = emptyList<PathNode>(),
            ) {
                addPath(
                    pathData = PathParser().parsePathString(
                        "M209.17,257.87C208.99,254.36 210.22,250.81 209.94,247.12C209.26,238.37 204.13,228.83 197.39,223.21C195.18,221.37 192.78,219.53 190.22,218.2C188.19,217.15 185.73,216.86 184.1,215.32C197.16,199.5 228.01,182.69 242.75,205.89C254.46,224.33 246.06,241.6 229.02,252.18C223.66,255.52 215.66,258.87 209.17,257.87ZM322.8,215.32C320.08,217.78 313.51,219.69 309.44,223.2C301.89,229.73 296.59,240.36 296.64,250.39C296.66,252.98 297.52,255.42 297.69,257.87C291.22,258.88 283.16,255.54 277.82,252.21C260.76,241.59 252.08,224.03 264.24,205.58C279.26,182.81 309.74,199.59 322.8,215.32ZM231.32,208.23C227.36,209.33 213.63,219.11 218.82,223.75C220.05,224.85 221.61,225.38 223.25,224.9C225.24,224.32 236.05,216.43 236.82,214.64C238.29,211.19 234.95,207.22 231.32,208.23ZM272.97,208.27C270.36,209.08 268.9,212.44 270.23,214.87C271.24,216.71 281.55,224.26 283.62,224.85C285.24,225.32 287.24,224.9 288.39,223.62C293.23,218.21 276.8,207.08 272.97,208.27ZM178.96,220.89C192.56,218.55 203.06,235.18 205.16,246.66C206.29,252.88 204.59,261.04 198.11,263.84C183.42,270.2 167.55,261.29 164.23,245.69C162.23,236.26 168.67,222.66 178.96,220.89ZM324.33,220.89C334.61,220.23 342.98,232.05 343.02,241.51C343.09,256.02 327.79,269.61 313.31,265.79C289.47,259.5 306.27,222.04 324.33,220.89ZM182.59,258.06C183.56,257.03 184.11,256.09 183.85,254.6C183.47,252.45 175.58,241.51 173.67,240.34C172.76,239.77 172.01,239.94 171.01,240.1C165.08,245.49 176.14,258.05 182.59,258.06ZM323.35,257.4C328.79,261.06 335.71,251.27 336.84,246.78C337.3,244.99 337.74,241.52 335.91,240.36C333.72,238.99 332,241.8 330.92,243.27C328.29,246.85 321.88,252.71 323.35,257.4ZM224.67,261.3C226.25,264.15 225.24,269.56 225.24,272.83C225.24,282.65 225.25,292.47 225.24,302.29C225.24,306.7 224.1,311.59 218.84,312.11C215.43,312.45 210.03,313.03 207.28,310.57C203.94,307.61 204.88,302.52 204.88,298.55C204.88,287.78 203.51,275.95 205.03,265.3C211.57,263.7 218.14,263.45 224.67,261.3ZM301.82,265.2C303.5,273.12 302.06,288.43 302.06,297.15C302.06,300.95 303.04,306.04 300.63,309.27C297.83,313.04 291.88,312.65 287.7,312.08C281.71,311.26 281.68,305.14 281.68,300.42C281.68,291.69 281.68,282.96 281.68,274.24C281.68,270.13 281.19,265.65 281.87,261.61C284.56,261.37 287.88,263.17 290.58,263.83C294.26,264.73 298.12,264.57 301.82,265.2Z"
                    ).toNodes(),
                    fill = stroke,
                    pathFillType = PathFillType.EvenOdd,
                )
            }
        }.build()
    }

    /** Over-Ear Headphones vector (from head-phone-icon-5719280-512.svg) */
    val Headphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_headphone",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M398.07,103.57C402.93,111.75 407.78,119.93 412.64,128.11C415.29,126.97 417.94,125.83 420.59,124.7C423.34,126.92 425.17,140 426.23,143.98C431.03,162.02 435.04,180.83 437.1,199.42C439.18,218.19 443.06,238.92 437.82,257.52C436.75,261.34 435.18,265.27 432.84,268.51C432.1,269.53 430.89,270.42 430.38,271.59C427.69,277.69 426.28,288.04 423.82,295C422.06,299.93 416.79,310.06 416.16,314.46C415.01,322.45 421.62,331.78 421.13,339.7C420.6,348.36 417.07,357.42 413.75,365.35C412.63,368.04 410.73,371.02 410.13,373.85C408.85,379.92 409.31,387.01 408.9,393.22C407.96,407.34 406.85,422.65 402.79,436.23C399.12,448.53 389.78,457.09 380.33,465.19C376.64,468.35 373.02,472.31 368.29,473.79C361.13,476.02 351.18,473.14 346.13,467.7C344.77,466.23 344.46,464.33 343.44,462.76C339.05,463.84 334.93,465.13 330.35,465.13C308.66,465.13 307.22,444.87 306.97,428.26C306.51,397.73 310.66,366.02 316.78,336.2C318.71,326.76 321.35,305.27 327.38,298.17C331.42,293.41 340.43,294.02 344.79,297.89C346.82,299.69 347.61,302.34 349.52,304.11C354.42,301.45 360.45,298.14 366.33,299.31C370.49,300.14 374.27,302.73 376.62,306.27C377.32,307.33 377.84,308.86 378.81,309.69C381.14,311.68 394.34,316.23 397.43,316.32C402.84,308.65 408.52,291.08 411.17,281.82C411.97,278.98 414.86,273.55 414.06,270.61C413.5,268.54 409.56,265.77 408.17,263.98C404.35,259.09 400.88,252.45 400.37,246.17C400.06,242.49 400.63,238.65 400.58,234.96C400.45,225.15 400.24,215.3 399.88,205.5C399.26,188.78 398.04,171.18 393.82,154.98C392.59,150.25 390.46,145.99 388.88,141.44C391.9,139.11 394.91,136.77 397.93,134.43C394.42,128.31 390.9,122.18 387.39,116.06C385.52,117.46 383.65,118.86 381.78,120.26C377.32,117.5 371.06,107.65 366.75,103.39C358.81,95.53 349.62,89.99 339.59,85.22C320.2,75.99 298.02,73.17 276.76,71.78C239.05,69.31 196.89,70.38 162.81,88.22C153.3,93.19 145.54,99.26 138.17,107.07C134.37,111.1 130.98,117.24 126.48,120.36C124.77,118.89 123.05,117.42 121.34,115.95C118.44,118.94 110.52,131.59 111.01,135.36C113.92,137.39 116.83,139.42 119.74,141.44C114.84,149.74 113.27,160.11 111.85,169.5C109.34,186.16 108.18,203.13 107.99,220C107.9,227.63 109.26,241.05 107.82,247.94C105.91,257.12 100.57,263.92 94.2,270.49C96.14,282.48 101.26,294.25 105.85,305.39C107.23,308.73 108.6,314.3 111.52,316.56C115.64,314.79 126.72,312.33 129.59,309.83C130.74,308.83 131.39,306.76 132.26,305.5C134.52,302.21 138.89,299.62 142.85,299.2C149.26,298.5 153.59,302.69 159.21,303.94C161.03,301.9 161.76,298.99 164.28,297.31C169.24,294.02 178.15,293.45 181.76,299.15C187.46,308.15 189.28,324.09 191.18,334.55C196.2,362.18 200.52,390.55 201.02,418.72C201.25,431.72 203.63,447.38 194.61,458.09C189.4,464.28 180.98,465.9 173.24,464.85C170.41,464.47 167.52,462.8 164.68,463.14C163.91,464.65 163.6,466.17 162.45,467.54C157.05,473.97 144.57,476.6 137.13,472.85C134.2,471.37 132.11,468.4 129.56,466.38C119.15,458.15 109.3,448.49 105.26,435.52C100.79,421.21 100.24,404.63 99.19,389.73C98.84,384.78 99.09,379.68 98.27,374.77C97.71,371.34 95.59,367.86 94.27,364.67C91.05,356.93 87.58,347.69 87.13,339.23C86.7,330.93 92.94,323.12 92.15,314.45C91.76,310.28 87.74,303.54 86.21,299.36C83.94,293.18 81.99,286.84 80.23,280.49C79.42,277.58 79.21,274.29 77.97,271.52C77.37,270.18 75.93,269.09 75.11,267.86C72.76,264.29 71.22,260.11 70.24,255.97C63.55,227.67 72.26,185.89 78.81,157.66C80.42,150.71 82.2,143.83 83.89,136.9C84.82,133.04 85.33,127.83 87.65,124.61C90.3,125.79 92.96,126.97 95.62,128.15C100.51,119.96 105.4,111.76 110.28,103.57C109.09,101.22 105.11,98.18 104.95,95.56C104.79,92.98 112.84,84.01 114.82,81.87C126.93,68.76 142.3,58.68 158.61,51.74C194.99,36.26 234.84,32.33 274.24,34.76C298.18,36.23 323.06,40.04 345.09,49.76C362.22,57.33 377.62,66.26 391.07,79.57C393.69,82.17 403.45,92.65 403.14,96.09C402.93,98.4 399.4,101.67 398.07,103.57Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Wired In-Ear Earphones vector (from earphones-icon-4740774-512.svg) */
    val WiredEarphones: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_wired_earphones",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M105.29,209.71C103.71,213.8 104.88,220.26 104.88,224.67C104.88,236.05 104.88,247.43 104.88,258.81C104.88,297.61 104.88,336.42 104.88,375.23C104.88,388.01 104.88,400.79 104.88,413.57C104.88,416.95 106.49,425.19 101.23,425.1C96.29,425.01 97.95,416.82 97.95,413.57C97.95,400.64 97.95,387.7 97.95,374.77C97.95,336.27 97.95,297.77 97.95,259.27C97.95,247.9 97.95,236.52 97.95,225.14C97.95,220.44 99.1,213.9 97.49,209.51C93.65,208.61 88.42,209.28 85.47,206.06C81.12,201.3 80.23,192.46 78.85,186.42C75.14,170.24 72.11,153.88 70.88,137.26C69.83,123.05 68.88,108.96 68.72,94.68C68.64,88.55 68.1,82.06 69.17,75.98C72.15,59.06 85.33,42.69 102.3,38.07C113.75,34.96 125.93,36 137.7,36C146.35,36 162.52,34.28 170.13,37.23C180.22,41.15 179.94,50.25 179.94,59.62C179.94,76.45 179.94,93.28 179.94,110.12C179.94,117.59 181.46,127.3 175.09,132.79C167.31,139.49 156.09,136.79 146.6,137.18C138.79,137.51 131.8,141.23 127.21,147.58C120.81,156.44 123.45,178.17 123.39,189.14C123.36,194.5 124.38,202.56 119.65,206.38C115.83,209.46 109.83,208.87 105.29,209.71ZM208.87,36.19C214.09,35.12 220.29,36 225.61,36C239.24,36 253.74,35.07 263.84,45.93C274.04,56.89 273.29,69.77 273.29,83.93C273.29,103.61 275.53,123.34 255.2,133.89C248.63,137.31 240.76,137.17 233.56,137.17C226.24,137.17 211.56,139.06 205.33,135.76C197.89,131.83 198.03,124.52 198.03,117.13C198.03,104.5 198.03,91.88 198.03,79.25C198.03,73.18 198.03,67.1 198.03,61.02C198.03,51.58 196.81,38.68 208.87,36.19ZM186.1,48.54C188.04,48.54 189.97,48.54 191.9,48.54C191.9,73.85 191.9,99.15 191.9,124.46C189.97,124.46 188.04,124.46 186.1,124.46C186.1,99.15 186.1,73.85 186.1,48.54ZM268.24,150.28C272.51,149.68 276.95,150 281.25,150C290.76,150 305.53,146.82 311.7,156.1C314.77,160.72 313.97,166.56 313.97,171.84C313.97,182.12 313.97,192.41 313.97,202.7C313.97,212.98 313.97,223.27 313.97,233.56C313.97,240.59 312.6,248.33 304.85,250.83C299.82,252.46 292.59,251.17 287.33,251.17C271.15,251.17 254.11,251.93 244.12,236.77C237.58,226.83 238.71,215.02 238.71,203.63C238.71,195 237.33,184.94 239.27,176.49C242.57,162.11 253.77,152.32 268.24,150.28ZM414.51,323.48C413.33,326.89 414.05,331.41 414.05,335.02C414.05,343.13 414.05,351.23 414.05,359.34C414.05,386.3 414.05,413.26 414.05,440.23C414.05,449.42 414.05,458.62 414.05,467.81C414.05,470.63 414.93,475.83 410.77,475.95C406.41,476.08 407.12,470.71 407.12,467.81C407.12,458.77 407.12,449.73 407.12,440.69C407.12,413.73 407.12,386.77 407.12,359.8C407.12,351.7 407.12,343.59 407.12,335.49C407.12,331.89 407.89,327.2 406.78,323.8C402.14,322.63 396.4,323.53 392.33,320.49C387.5,316.9 388.61,308.44 388.59,303.23C388.53,292.28 391.11,270.49 384.8,261.66C377.73,251.74 367.8,251.17 356.53,251.17C350.84,251.17 344.34,251.95 339.31,248.71C331.31,243.57 332.06,234.96 332.06,226.54C332.06,208.93 332.06,191.32 332.06,173.71C332.06,164.38 331.7,155.18 341.77,151.26C349.43,148.28 365.6,150 374.3,150C385.85,150 397.99,149.12 409.25,152.15C413.35,153.26 417.31,155.54 420.93,157.7C441.97,170.23 443.47,189.86 443.21,212.05C443.06,225.06 442.01,237.91 441.11,250.87C439.88,268.37 437.22,286.65 432.3,303.48C430.8,308.63 430.5,316.85 425.94,320.5C422.65,323.13 418.33,322.5 414.51,323.48ZM320.09,162.54C322.03,162.54 323.96,162.54 325.9,162.54C325.9,187.84 325.9,213.14 325.9,238.44C323.96,238.44 322.03,238.44 320.09,238.44C320.09,213.14 320.09,187.84 320.09,162.54Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Bluetooth Speaker vector (from speaker-icon-6980389-512.svg) */
    val BluetoothSpeaker: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_bluetooth_speaker",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M174.27,79.69C189.28,78.3 204.92,79.5 220,79.5C249.45,79.5 278.91,79.5 308.37,79.5C326.63,79.5 347.91,76.02 361.76,90.61C371.8,101.17 371.8,114.6 371.8,128.35C371.8,140.35 371.79,152.35 371.79,164.35C371.78,227.79 371.86,291.22 371.78,354.66C371.77,363.86 371.77,373.05 371.77,382.25C371.77,401.35 371.55,419.23 351.83,428.77C342.24,433.41 331.86,432.5 321.46,432.5C305.25,432.5 289.04,432.5 272.83,432.5C247.43,432.5 222.02,432.5 196.62,432.5C183.01,432.5 168.49,434.43 156.49,426.82C137.07,414.5 140.2,390.93 140.19,371.02C140.18,323.49 140.19,275.95 140.19,228.41C140.19,196.31 140.19,164.2 140.19,132.09C140.19,123.86 139.21,114.9 141.19,106.85C145.08,91.08 158.6,81.14 174.27,79.69ZM252.37,113.37C244.87,113.95 237.7,116 231.15,119.66C192.38,141.37 197.14,198.01 238.67,213.24C244.04,215.2 250.24,216.5 256,216.45C263.44,216.38 271.27,214.54 277.89,211.24C292.61,203.9 303.65,190.57 306.76,174.17C312.91,141.72 285.16,110.85 252.37,113.37ZM252.85,128.8C258.06,128.31 263.02,129.01 267.93,130.67C304.17,142.91 297.44,196.64 259.53,200.82C221.48,205.02 204.62,152.77 237.67,133.66C242.49,130.87 247.28,129.32 252.85,128.8ZM253.29,143.7C225.37,148.47 231.49,189.41 258.32,185.96C286.8,182.29 280.07,139.12 253.29,143.7ZM254.2,159.13C262.52,157.17 264.85,169.69 256.46,170.68C249.73,171.48 247.51,160.71 254.2,159.13ZM87.79,177.91C93.47,177.74 97.43,183.44 94.94,188.66C92.35,194.11 84.85,200.13 81.31,205.91C72.02,221.08 66.85,237.61 66.66,255.53C66.46,273.72 71.99,290.73 81.28,306.12C84.69,311.77 92.62,318.36 94.95,323.33C97.46,328.67 93.49,334.31 87.69,334.17C83.86,334.07 81.27,330.74 78.84,328.18C72.31,321.31 66.92,313 62.71,304.58C46.79,272.79 47.67,234.9 64.72,203.84C68.64,196.68 73.5,190.44 78.79,184.23C81.25,181.33 83.69,178.02 87.79,177.91ZM423.5,177.9C427.9,177.74 430.62,181.11 433.24,184.19C439.4,191.42 445.02,199.03 449.14,207.59C464.57,239.58 464.42,276.79 447.22,308.08C443.31,315.17 438.59,321.69 433.18,327.74C430.53,330.71 428.22,334.11 423.86,334.1C418.11,334.09 414.35,328.13 417.18,322.97C420.3,317.29 427.12,311.82 430.84,305.78C440.1,290.74 445.34,273.79 445.32,256C445.3,238.21 440.07,221.28 430.85,206.22C427.21,200.27 420.06,194.37 417.18,189.03C414.52,184.1 417.94,178.1 423.5,177.9ZM113.81,203.63C119.35,203.61 122.94,209.13 120.94,214.27C119.43,218.15 114.55,221.6 112.21,225.17C106.22,234.3 102.96,245.04 103,256C103.04,266.85 106.18,277.81 112.21,286.85C114.62,290.46 119.28,293.77 120.89,297.76C122.97,302.89 119.31,308.38 113.81,308.37C107.14,308.36 99.95,296.19 96.85,291.02C84.62,270.6 85.15,244.03 95.72,223.14C98.54,217.58 106.99,203.65 113.81,203.63ZM398.19,203.63C405.08,203.66 413.29,217.64 416.23,223.19C427.17,243.87 426.88,270.32 415.22,290.64C412.17,295.96 405.02,308.34 398.19,308.37C392.72,308.39 388.96,302.88 391.1,297.76C392.76,293.8 397.31,290.37 399.74,286.78C405.74,277.91 409,266.71 408.97,256C408.95,245.23 405.76,234.14 399.75,225.2C397.4,221.71 392.52,218.14 391.05,214.29C389.09,209.12 392.62,203.61 398.19,203.63ZM255.15,232.57C238.78,233.57 224.29,237.24 210.42,246.2C201.7,251.83 193.42,259.66 187.67,268.33C179.99,279.91 174.87,292.69 173.2,306.58C167.05,357.86 209.88,400.56 260.21,398.82C271.88,398.42 284.35,394.91 294.49,389.21C318.88,375.49 335.98,351.94 338.78,323.69C342.49,286.4 319.44,250.82 284.76,237.74C275.58,234.29 265.07,231.96 255.15,232.57ZM254.67,248.04C263.56,247.76 272.29,249.51 280.46,252.77C318.63,267.96 334.03,310.73 315.83,347.4C309.24,360.67 297.29,370.64 284.32,377.15C276.09,381.28 266.17,383.46 256.94,383.45C247.49,383.43 237.45,381.92 228.83,377.9C186.77,358.26 174.44,304.48 206.28,269.64C212.13,263.24 219.11,258.46 226.82,254.63C235.45,250.34 245.04,248.34 254.67,248.04ZM255.58,284.52C250.63,284.76 245.6,285.97 241.19,288.21C211.16,303.44 225.66,348.86 258.81,346.81C263.51,346.52 267.95,344.63 271.93,342.27C298.45,326.55 286.95,282.98 255.58,284.52ZM252.39,300.36C271.1,294.87 279.8,325.12 260.53,330.84C240.64,336.75 231.71,306.44 252.39,300.36Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Car Bluetooth / Android Auto vector (from car-icon-4494615-512.svg) */
    val CarAudio: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_car_audio",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M249.03,23.57C272.33,22.23 297.99,30.71 316.4,44.82C321.98,49.09 335.12,56.39 331.74,65.03C329.98,69.52 324.59,71.11 320.52,68.77C314.34,65.2 309.71,59.08 303.58,55.29C287.37,45.26 270.09,39.46 250.86,39.19C224.74,38.82 202.17,50.04 182.95,66.77C178.94,70.27 173.32,70.6 170.08,65.81C164.75,57.92 178.01,49.44 183.35,45.64C203.89,31.02 223.94,25.02 249.03,23.57ZM250,52.57C267.47,51.71 286.51,58.3 300.31,68.84C305.48,72.79 314.89,78.53 310.81,86.15C308.91,89.71 304.31,91.24 300.6,89.57C295.74,87.39 292.07,82.13 287.45,79.37C276.96,73.09 265.61,68.79 253.19,68.5C239.18,68.19 225.94,71.87 214.15,79.27C210.26,81.71 207.22,85.19 203.49,87.75C199.79,90.28 194.62,89.73 192.06,85.9C187.26,78.69 196.06,73.11 201.01,69.39C215.41,58.57 232.1,53.45 250,52.57ZM248.59,82.05C261.52,81.68 273.66,85.89 283.91,93.65C288.75,97.32 293.67,102.14 289.59,108.4C287.65,111.38 283.05,112.22 279.93,110.76C277.13,109.45 275,106.82 272.49,105.05C266.44,100.78 258.29,98 250.86,98.01C243.72,98.02 236.07,100.2 230.13,104.11C227.2,106.03 224.65,109.51 221.32,110.73C218.33,111.83 214.08,111 212.19,108.32C208.02,102.38 212.26,97.46 216.96,93.75C225.88,86.69 237.18,82.38 248.59,82.05ZM248.56,111.44C264.8,108.96 277.06,125.58 274.85,140.5C273.18,151.82 262.08,160.63 250.86,160.79C233.4,161.03 220.76,141.91 228.11,125.99C232.03,117.49 239.51,112.81 248.56,111.44ZM419.19,234.86C424.45,235.62 430.22,235 435.55,235C446.93,235 458.31,235 469.68,235C476.01,235 485.38,233.18 487.68,241.06C489.01,245.64 488,251.71 488,256.47C488,262.34 489.76,276.03 486,280.48C482.53,284.59 475,283.74 470.15,284.16C461.27,284.94 452.39,285.88 443.52,286.8C439.13,287.26 434.4,287.06 430.16,288.26C430.16,291.07 430.7,296.42 432.21,298.86C433.1,300.31 442.36,305.41 444.66,307.45C452.23,314.17 458.15,322.5 461.72,331.96C465.32,341.46 465.23,351.69 466.3,361.67C468.61,383.39 469.29,404.82 469.29,426.67C469.29,435.24 469.29,443.81 469.29,452.38C469.29,456.88 469.8,461.9 468.81,466.32C462.78,493.43 432.64,488 411.7,488C400.93,488 389.29,489.57 380.15,482.76C373.47,477.79 369.41,470.58 369,462.2C368.84,459 369.3,455.64 368.62,452.51C293.54,452.51 218.46,452.51 143.38,452.51C142.69,455.64 143.16,459 143,462.2C142.59,470.55 138.52,477.81 131.85,482.76C122.58,489.63 110.74,488 99.83,488C78.19,488 49.52,493.2 43.29,465.65C42.38,461.61 42.93,456.96 42.93,452.85C42.93,444.12 42.93,435.39 42.93,426.67C42.93,405.29 43.77,385.27 45.76,364.01C46.75,353.42 46.74,342.54 50.27,332.43C53.62,322.86 59.78,314.19 67.38,307.48C69.6,305.52 78.98,300.22 79.79,298.86C81.28,296.39 81.76,291.09 81.79,288.26C77.69,287.09 73.18,287.26 68.95,286.79C59.81,285.77 50.57,284.52 41.38,284C36.46,283.72 28.18,284.15 25.29,279.31C22.42,274.5 24,262.27 24,256.47C24,251.76 22.94,245.58 24.36,241.06C26.81,233.23 35.9,235 42.32,235C53.69,235 65.07,235 76.45,235C81.77,235 87.57,235.64 92.81,234.85C95.83,229.79 95.3,222.3 97.14,216.68C101.53,203.29 110.32,191.23 122.8,184.26C141.23,173.96 167.66,177.5 188.2,177.5C233.09,177.5 277.98,177.5 322.86,177.5C343,177.5 371.37,174.09 389.17,184.29C401.59,191.41 410.09,202.98 414.8,216.25C416.82,221.94 415.99,229.77 419.19,234.86ZM414.04,292.83C414.61,286.58 408.93,265.61 407.25,258.33C403.26,241.07 402.76,219.01 390.54,205.39C377.88,191.27 358.76,193.5 341.57,193.5C304.78,193.5 268,193.5 231.22,193.5C208.93,193.5 186.64,193.5 164.35,193.5C151.24,193.5 137.07,192.58 126.13,201.19C113.8,210.9 111.8,224.63 108.8,239.11C107.14,247.07 105.47,255.04 103.77,263C101.79,272.22 97.94,283.52 97.85,292.83C203.25,292.83 308.65,292.83 414.04,292.83ZM102.72,334.07C78.32,335.23 67.42,365.34 82.2,383.75C90.91,394.58 104.02,394 116.66,394C124.61,394 132.56,394 140.51,394C156.39,394 172.14,393.85 180.76,377.88C186.74,366.79 184.09,354.1 172.41,348.23C167.09,345.56 160.65,344.98 154.87,343.87C145.03,341.97 135.15,339.95 125.36,337.8C118.02,336.19 110.28,333.71 102.72,334.07ZM411.37,334.08C406.52,334.27 401.57,335.85 396.83,336.83C386.99,338.87 377.11,340.67 367.28,342.78C359.61,344.43 350.4,344.7 343.3,348.21C332.61,353.48 329.36,367.73 335.1,377.7C347.59,399.41 377.07,394 398.14,394C412.35,394 426.54,395.04 435.71,382.16C449.78,362.4 436.55,333.06 411.37,334.08ZM309.27,420.12C307.18,418.87 303.79,419.5 301.36,419.5C295.43,419.5 289.51,419.5 283.59,419.5C264.42,419.5 245.25,419.5 226.07,419.5C221.07,419.5 206.26,418.31 202.5,420.12C202.5,425.58 202.5,431.03 202.5,436.49C238.09,436.49 273.68,436.49 309.27,436.49C309.27,431.03 309.27,425.58 309.27,420.12Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Single-Ear Bluetooth Headset vector (from wireless-headset-icon-5683678-512.svg) */
    val SingleEarHeadset: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_single_ear_headset",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M303.19 316.32C312.68 317.8 323.07 316.24 332.68 316.51C356.61 317.19 392.35 319.8 413.78 308.79C446.05 292.21 458.02 259.99 454.2 225.11C449.87 185.65 431.71 151.58 404.71 122.95C394.93 112.58 382.93 103.81 370.66 96.69C332.71 74.69 292.56 67.07 250.44 83.27C231.69 90.48 214.5 101.26 199.19 114.33C194.75 118.13 190.39 122.04 186.45 126.36C184.53 128.47 182.74 131.34 180.36 132.93C172.58 138.15 161.44 130.77 164.27 121.44C166.03 115.6 172.51 110.92 176.7 106.78C190.9 92.78 208.36 80.23 226.39 71.81C247.02 62.17 268.43 54.94 291.52 54.03C356.56 51.45 421.42 94.06 451.88 150.6C478.2 199.48 490.64 265.25 448.27 309.15C442.58 315.06 435.58 320.44 428.38 324.35C418.62 329.65 408.62 333.6 397.7 335.86C385.95 338.3 373.61 337.83 361.67 337.83C343.13 337.83 324.58 337.5 306.03 337.5C298.71 337.5 291.37 337.68 284.05 337.48C280.95 337.39 276.44 336.36 273.5 337.37C269.05 338.89 264.32 343.99 260.53 346.78C250.56 354.12 240.55 361.42 230.54 368.71C204.2 387.89 177.92 407.17 151.45 426.17C143.11 432.15 134.88 438.27 126.56 444.26C124.58 445.67 122.89 447.57 120.78 448.79C92.7 465.04 55.28 459.47 41.14 427.59C33.96 411.39 34.66 393.24 43.25 377.78C46.42 372.06 50.88 366.92 54.69 361.62C60.99 352.84 67.41 344.13 73.82 335.42C107.59 289.51 140.65 243.08 174.75 197.44C182.16 187.52 189.44 177.51 196.8 167.56C199.53 163.87 202.02 159.65 205.18 156.32C222.28 138.38 246.95 129.07 271.43 128.86C342.19 128.23 387.68 204.68 354.71 267.06C346.02 283.51 334.89 292.04 320.35 302.8C316.33 305.77 312.34 308.88 308.26 311.76C306.63 312.91 303.51 314.41 303.19 316.32ZM267.72 182.58C261.47 182.66 255.28 184.12 249.69 186.78C215.5 202.99 215.71 250.77 250.06 266.82C255.51 269.36 261.68 270.4 267.69 270.46C274.05 270.52 281.02 269.36 286.65 266.32C298.68 259.84 307.32 250.17 310.79 236.74C317.77 209.71 295.33 182.21 267.72 182.58ZM264.54 190.06C270.29 189.67 276.16 190.27 281.56 192.36C315.74 205.59 309.9 257.21 273.77 262.95C268.87 263.73 263.3 263.32 258.59 262.08C219.63 251.77 224.11 192.8 264.54 190.06ZM198.61 291.44C196.47 292.06 194.5 295.06 192.97 296.56C188.87 300.56 184.92 304.7 180.82 308.71C165.88 323.35 151.02 338.15 136.4 353.12C131.7 357.93 126.92 362.66 122.14 367.39C120.44 369.06 118.91 371.71 121.3 373.64C123.87 375.71 126.31 373.1 128.04 371.42C132.85 366.72 137.53 361.89 142.3 357.15C158.47 341.05 174.51 324.82 190.7 308.75C193.74 305.74 201 300.06 202.68 296.45C203.81 294.01 201.31 290.65 198.61 291.44Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Bluetooth Neckband earphones vector (from modern-sport-wireless-headphones-with-volume-control_133472-1172.svg) */
    val Neckband: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_neckband",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 1095f, viewportHeight = 1095f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M869.09 559.5C871.09 561.63 873.17 563.72 873.55 566.79C874.17 571.88 865.74 584.74 863.32 589.79C862.63 591.24 861.31 592.99 861.02 594.56C860.4 597.91 863.83 605.91 863.52 611.19C862.9 621.68 857.09 630.87 852.53 640.06C820.17 705.32 787.71 770.55 755.39 835.84C750.87 844.97 746.16 854 741.57 863.09C732.87 880.34 725.04 895.51 702.5 891.44C696.65 900.42 693.14 910.88 687.45 919.97C685.33 923.37 681.63 923.17 679.43 925.9C676.19 929.92 674.2 935.5 671.4 939.88C664 951.45 655.64 962.67 645.57 972.06C635.39 981.56 620.46 991.65 605.81 987.58C591.76 983.67 587.23 967.34 586.96 954.5C586.39 926.72 597.13 898.82 608.26 873.75C639.94 802.37 681.67 736.35 717.77 667.27C742.06 620.78 764.22 572.32 781.2 522.65C797.82 474.03 814.44 423.37 815.7 371.5C817.06 315.69 784.31 243.22 720.5 241.26C711.81 241 702.64 242.34 694.31 244.74C684.03 247.69 674.37 251.85 665.58 258.03C635.4 279.25 617.37 314.66 608.18 349.56C604.52 363.43 603.5 377.44 601.23 391.5C602.26 392.35 604.32 392.59 605.08 393.63C606.27 395.25 605.45 405.95 605.45 408.5C605.45 419.5 605.36 430.5 605.46 441.5C607.07 442.17 608.67 442.83 610.27 443.5C610.27 445.9 610.27 448.31 610.27 450.71C620.79 450.71 631.32 450.71 641.84 450.71C641.84 452.55 641.84 454.39 641.84 456.24C644.51 456.28 647.86 456.83 650.45 456.07C654.48 454.89 658.78 449.68 664.65 448.26C676.33 445.44 688.74 448.58 696.93 457.57C698.96 459.8 701.24 462.48 701.71 465.56C702.52 470.83 702.85 491 700.97 495.47C695.77 507.89 679.62 512.7 667.5 510.98C662.7 510.3 658.13 508.14 654.24 505.29C653.01 504.39 651.72 502.81 650.23 502.4C647.91 501.75 644.88 502.28 642.5 502.35C641.96 504.24 641.43 506.13 640.89 508.03C627.76 508.03 614.63 508.03 601.5 508.03C597.92 503.73 595.94 504.79 590.5 504.78C589.96 505.84 589.51 507.31 588.33 507.81C585.25 509.1 575.33 508.19 571.95 507.49C571.95 488.56 571.95 469.63 571.95 450.7C577.47 450.7 583 450.7 588.53 450.7C588.53 448.3 588.53 445.9 588.53 443.5C590.13 442.83 591.73 442.17 593.33 441.5C593.33 425.5 593.33 409.5 593.33 393.5C594.21 393.2 596.21 392.92 596.78 392.2C597.79 390.93 597.32 388.03 597.4 386.52C597.64 381.66 597.95 376.73 598.48 371.9C600.79 350.68 605.25 329.22 614.21 309.71C635.61 263.16 672.46 229.15 726.5 230.97C738.08 231.37 749.86 234.06 760.25 239.27C770.01 244.17 778.58 250.49 786.23 258.26C796.78 268.96 804.73 282.02 810.81 295.7C833.72 347.32 824.88 407.04 810.22 459.64C795.05 514.09 774.33 566.59 750.2 617.69C741.4 636.34 732.71 655.19 722.79 673.28C704.04 707.43 684.96 741.37 665.78 775.28C642.96 815.6 619.94 856.87 603.86 900.41C596.85 919.38 589.12 942.07 594.16 962.44C598.65 980.59 613.36 984.69 628.83 975.34C648.79 963.28 661.12 941.36 671.86 921.5C669.09 915.75 671.9 911.23 674.38 905.86C677.81 898.4 681.35 891 684.69 883.5C681.51 879.85 677.85 877.19 676.11 872.42C673.41 864.98 675.24 856.53 677.84 849.37C683.1 834.84 691.22 821.32 697.93 807.43C720.41 760.89 744.14 714.94 767.17 668.67C776.36 650.23 786.16 631.98 794.7 613.23C798.61 604.64 802.23 594.34 809.66 588.11C813.13 585.21 821.47 582.98 823.35 580.83C826.03 577.76 832.23 562.68 834.51 558.01C835.85 555.28 837.17 551.05 840.09 549.59C842.65 548.31 845.76 548.96 848.5 549.08C864.81 514 877.33 476.25 886.28 438.66C897.99 389.5 907.11 340.28 906.8 289.5C906.47 236.97 890.72 186.22 842.98 158.52C800.68 133.97 751.01 133.06 703.5 131.83C647.18 130.37 590.84 129.14 534.5 129.52C485.14 129.85 435.85 130.9 386.5 131.96C327.61 133.21 258.64 137.26 218.56 187.08C190.58 221.87 186.38 269.53 188.71 312.5C193.04 392.04 213.59 476.3 246.5 548.98C249.23 548.95 252.38 548.32 254.93 549.58C258.06 551.12 259.41 555.72 260.84 558.63C263.15 563.31 269 577.6 271.71 580.76C273.57 582.94 281.98 585.22 285.41 588.06C292.86 594.23 296.4 604.66 300.35 613.2C330.62 678.57 365.76 741.67 396.8 806.67C402.7 819.03 409.23 831.08 414.91 843.55C418.69 851.87 421.82 861.64 419.41 870.82C417.92 876.52 413.88 879.26 410.38 883.5C413.76 890.96 417.25 898.37 420.66 905.81C423.16 911.25 425.93 915.68 423.24 921.5C434.02 941.43 446.25 963.16 466.22 975.3C481.75 984.75 496.31 980.58 500.88 962.45C505.98 942.25 498.32 919.14 491.24 900.3C475.48 858.4 453.66 818.66 431.77 779.71C409.56 740.2 386.21 701.16 365.77 660.7C333.04 595.89 304.87 529.4 284.83 459.58C269.85 407.39 261.37 346.95 284.23 295.73C290.22 282.29 298.04 269.4 308.37 258.84C316.2 250.84 324.74 244.34 334.77 239.3C345.17 234.08 356.92 231.38 368.5 230.98C422.56 229.11 460.13 263.5 481.15 310.39C489.78 329.62 494.42 350.74 496.61 371.63C497.12 376.54 497.47 381.56 497.71 386.5C497.78 387.98 497.27 390.96 498.24 392.2C498.78 392.89 500.93 393.22 501.77 393.5C501.77 409.5 501.77 425.5 501.77 441.5C503.36 442.17 504.95 442.83 506.54 443.5C506.54 445.9 506.54 448.3 506.54 450.69C512.05 450.69 517.57 450.69 523.09 450.69C523.09 469.68 523.09 488.66 523.09 507.65C519.49 508.06 509.86 509.17 506.68 507.79C505.59 507.31 505.03 505.76 504.5 504.78C499.06 504.78 497.13 503.77 493.5 508.01C480.39 508.01 467.29 508.01 454.18 508.01C453.62 506.1 453.06 504.2 452.5 502.29C450.12 502.27 447.08 501.76 444.77 502.41C442.18 503.15 436.29 508.52 431.52 510.1C419.44 514.08 395.96 507.67 393.38 493.28C392.5 488.37 392.26 467.73 393.96 463.51C398.59 452.04 413.9 446.05 425.36 447.37C429.84 447.89 434.07 449.26 437.99 451.51C440.13 452.75 442.22 455.35 444.55 456.05C446.97 456.77 450.01 456.3 452.5 456.28C452.81 454.97 452.93 452.47 454.03 451.55C456 449.89 460.11 450.67 462.5 450.67C469.87 450.67 477.6 451.32 484.89 450.31C484.89 447.71 484.89 445.1 484.89 442.5C485.81 442.48 488.1 442.9 488.82 442.25C490.45 440.78 489.66 431.05 489.66 428.5C489.66 420.17 489.66 411.83 489.66 403.5C489.66 401.19 488.62 395.55 489.98 393.63C490.72 392.59 492.76 392.34 493.75 391.5C488.06 341.81 472.61 288.36 429.45 258.01C420.66 251.82 411.01 247.66 400.71 244.72C392.07 242.25 382.51 240.87 373.5 241.3C310.61 244.31 279.83 313.43 279.46 368.5C279.13 418.45 297.49 475.65 313.86 522.6C331.2 572.34 352.74 621.41 377.61 667.86C413.18 734.31 453.05 798.68 484.04 867.5C496.03 894.12 508.58 923.83 508.12 953.5C507.91 966.63 503.74 983.58 489.22 987.64C474.67 991.72 459.55 981.48 449.47 972.03C439.39 962.59 431.1 951.4 423.65 939.82C420.85 935.46 418.86 929.84 415.61 925.86C413.42 923.16 409.99 923.68 407.58 919.92C401.81 910.88 398.4 900.37 392.5 891.43C370.09 895.75 362.09 880.06 353.47 863.05C348.86 853.97 344.19 844.92 339.67 835.79C320.3 796.57 300.54 757.57 281.1 718.39C268.05 692.09 255.53 665.49 242.17 639.35C237.54 630.28 231.88 620.94 231.56 610.5C231.42 605.64 234.69 598.01 234.06 594.55C233.78 592.98 232.43 591.19 231.73 589.75C229.35 584.79 220.92 571.87 221.51 566.89C221.88 563.76 223.92 561.67 225.97 559.5C224.21 552.29 220.69 545.37 218.19 538.37C212.36 522.09 206.34 505.81 201.31 489.26C181.5 423.99 165 356.24 168.82 287.5C170.79 252 178.34 214.12 198.64 184.17C241.81 120.49 314.63 112.38 385.5 109.76C502.09 105.45 618.95 105.08 735.5 110.88C793.45 113.76 854.61 128.88 891.45 177.11C915.05 208.01 923.98 248.45 926.21 286.5C930.22 355.04 914.48 424.38 893.73 489.3C888.47 505.79 882.75 522.13 876.87 538.41C874.35 545.39 870.85 552.3 869.09 559.5ZM259.79 634.35C255.41 635.81 254.74 640.55 256.22 644.28C260.15 654.16 266.26 667.58 272 676.45C274.41 680.19 278.72 682.43 283.09 680.55C288.02 678.43 287.26 671.63 285.91 667.55C283.64 660.69 275.29 644.33 270.89 638.66C268.27 635.29 264.12 632.91 259.79 634.35ZM298.64 713.18C294.17 714.34 293.29 719.19 294.51 723C296.79 730.15 306.39 749.6 310.67 755.79C313.02 759.18 317.11 761.43 321.27 759.8C325.99 757.95 325.88 751.66 324.79 747.65C323.05 741.24 313.59 723.39 309.5 718C306.92 714.6 303.09 712.03 298.64 713.18ZM340.68 793.39C335.96 793.79 333.45 797.83 334.34 802.26C335.13 806.18 337.93 810.22 339.65 813.8C343.24 821.27 346.3 830.52 351.44 837.05C353.82 840.07 357.9 841.88 361.6 840.17C366.42 837.94 366.17 831.89 364.88 827.56C362.8 820.56 354.55 805.38 350.35 799.17C348.28 796.13 344.61 793.06 340.68 793.39Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Ringtone notification bell vector (from notification-icon-8420908-512.svg) */
    val Ringtone: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_ringtone",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M387.82 323.8C384.75 328.15 377.38 330.86 372.76 333.23C360.01 339.76 346.78 344.24 333.08 348.22C285.34 362.1 219.37 361.7 172.05 346.27C160.08 342.37 148.63 338.04 137.4 332.29C133.22 330.15 127.07 327.61 124.18 323.8C127.5 316.54 131.76 309.85 134.79 302.4C141.55 285.83 144.95 267.23 146.82 249.45C148 238.28 148.82 227.03 149.05 215.79C149.25 205.64 148.65 195.13 150.3 185.07C152.03 174.64 155.06 164.84 159.18 155.16C166.31 138.38 180.11 122.35 195.34 112.34C199.66 109.5 204.13 106.52 208.79 104.26C212.63 102.41 216.93 101.33 219.76 97.94C224.8 91.92 219.29 82.21 220.3 75.05C223.32 53.74 244.33 37.84 265.92 44.28C280.23 48.55 290.21 61.29 291.77 75.98C292.64 84.19 286.18 95.15 295.17 100.62C297.67 102.14 300.58 102.99 303.2 104.27C307.87 106.56 312.31 109.5 316.66 112.34C335.44 124.62 349.47 144.41 356.85 165.27C365.24 189.01 362.2 214.88 364.16 239.63C366.04 263.35 370.35 287.99 380.2 309.71C382.42 314.59 385.36 319.06 387.82 323.8ZM48.93 158.24C56.31 157.79 61.24 165.65 58.84 172.25C56.91 177.57 49.47 183.47 46.14 188.54C37.43 201.83 32.97 217.21 32.89 233.09C32.81 248.58 37.21 263.53 45.29 276.64C48.31 281.55 57.48 290.3 58.88 294.37C61.19 301.06 56.46 308.11 49.33 308.11C44.4 308.11 41.74 304.82 38.69 301.47C32.28 294.42 26.89 286.67 22.79 278.09C8.4 247.96 8.62 211.65 25.75 182.61C29.9 175.56 40.08 158.79 48.93 158.24ZM462.26 158.24C466.65 157.96 469.81 161.11 472.57 164.04C479.27 171.13 484.88 179.4 489.17 188.13C504.29 218.91 502.72 255.94 485.23 285.34C481.19 292.13 471.27 308.1 462.67 308.11C455.56 308.12 450.83 301.04 453.13 294.37C454.79 289.58 463.47 281.77 466.68 276.6C474.7 263.69 479.17 248.84 479.12 233.56C479.07 217.66 474.58 201.84 465.86 188.54C462.5 183.43 455.17 177.6 453.18 172.24C450.75 165.72 455.35 158.67 462.26 158.24ZM83.03 194.19C88.69 193.14 94.64 197.34 95.05 203.16C95.47 209.21 91.22 211.62 88.3 216.17C85.46 220.6 83.43 225.92 83.14 231.22C82.81 237.08 84.15 243.7 87.24 248.73C89.67 252.69 94.56 256.3 95.01 261.14C95.57 267.1 91.17 272.09 85.23 272.38C77.04 272.79 70.73 261.91 67.84 255.53C60.72 239.88 61.25 222.44 69.75 207.45C72.49 202.62 77.12 195.29 83.03 194.19ZM424.92 194.24C433.56 192.27 439.85 202.92 443.25 209.24C451.47 224.52 451.37 244.67 442.11 259.51C438.81 264.8 434.14 272.3 427.13 272.39C421.31 272.46 416.8 267.8 416.93 262.08C417.06 256.97 421.33 254.13 423.8 250.09C427.35 244.28 429.62 237.18 428.73 230.28C428.08 225.24 426.42 220.46 423.7 216.17C421.68 212.98 418.12 210.67 417.16 206.88C415.73 201.26 419.33 195.52 424.92 194.24ZM111.52 340.21C126.92 352.36 146 359.07 164.29 365.27C212.87 381.72 280.19 382.51 329.86 370.24C354 364.28 380.71 356.02 400.48 340.2C402.82 341.39 404.56 343.83 406.35 345.73C415.53 355.47 415.17 363.39 406.57 373.61C395.43 386.84 373.07 395.72 356.99 400.74C294.28 420.31 222.05 418.11 158.97 401.88C141.86 397.48 117.08 387.46 105.39 373.66C96.58 363.25 96.24 355.6 105.64 345.73C107.44 343.84 109.17 341.38 111.52 340.21ZM301.43 432.75C302.29 439 294.62 449.81 290.34 454.24C272.37 472.81 242.9 473.58 223.81 456.28C218.71 451.66 209.64 439.87 210.64 432.76C215.31 432.55 220.02 433.52 224.67 433.81C235.09 434.48 245.56 435 256 435C266.59 435 277.23 434.49 287.8 433.78C292.32 433.48 296.89 432.61 301.43 432.75Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Sleep timer moon vector (from moon-icon-8465257-512.svg) */
    val SleepTimer: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_sleep_timer",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M279.38 301.5C270.64 306.93 252.97 304.26 244.3 299.75C239.82 297.41 235.45 291.85 232.16 290.34C230.2 289.45 227.62 290.84 227.15 292.95C226.21 297.23 234.32 302.45 237.39 304.31C248.18 310.89 259.91 312.07 272.37 311C276.13 310.67 279.82 308.8 283.59 308.99C288.14 316.49 292.91 323.85 298.39 330.72C317.07 354.18 351.2 374.22 380.12 380.84C388.94 382.86 397.92 385.06 407.03 385.5C416.76 385.96 426.41 383.87 436.07 386.05C444.37 387.93 446.88 395.61 442.72 402.72C438.21 410.42 430.47 416.11 423.41 421.28C406.04 434.03 387.16 442.18 366.64 448.22C277.23 474.52 180.29 426.05 137.25 346.02C125.84 324.81 118.84 301.65 115.22 277.86C111.87 255.78 113.5 232.85 117.84 211.11C132.95 135.5 191.44 74.09 265.84 54.81C286.78 49.38 317.81 46.4 338.98 51.22C346.05 52.83 362.67 58.11 358.77 68.61C357.17 72.91 352.75 75.68 349.24 78.29C341.04 84.41 331.08 87.57 322.61 93.27C302.66 106.72 285.5 126.58 274.26 147.76C264.97 165.26 259.99 183.49 257.76 203.16C256.34 215.68 256.74 230.32 263.26 241.46C266.18 246.46 270.72 250.13 275.88 252.7C281.68 255.59 288.8 257.48 290.62 264.58C292.05 270.2 288.88 278.32 284.3 281.73C282.17 283.32 277.23 285.65 276.19 288.13C274.38 292.43 278.38 297.56 279.38 301.5ZM99.49 51.57C102.3 50.69 104.85 53.68 106.14 55.87C109.25 61.12 109.15 68.73 112.81 73.54C116.5 78.41 130.73 75.29 132.85 83.01C135.37 92.2 117.71 91.88 113.43 96.84C109.35 101.57 107.27 119.74 99.83 119.46C96.72 119.35 95.55 115.96 94.71 113.55C92.97 108.5 92.94 101.53 89.6 97.2C85.9 92.39 69.71 91.88 68.44 85.21C67.02 77.79 84.78 76.68 88.44 73.24C93.1 68.86 92.87 53.65 99.49 51.57ZM360.28 126.01C369.72 125.92 366.55 140.44 358.87 139.78C350.78 139.09 351.09 126.1 360.28 126.01ZM310.82 162.86C316.3 161.09 316.47 170.88 318.34 173.8C320.59 177.31 327.71 176.16 328.66 179.83C329.15 181.7 327.93 183.09 326.54 184.17C324.33 185.86 321.16 186.45 319.35 188.66C317.11 191.38 316.44 199.67 312.11 199.79C306.54 199.93 308.05 191.25 305.71 188.31C303.29 185.26 296.42 186.25 294.86 182.28C293.18 177.99 302.81 176.92 304.94 174.72C307.78 171.77 306.31 164.32 310.82 162.86ZM416.05 169.4C419.61 167.99 421.77 172.05 423.1 174.66C425.55 179.44 426.92 184.61 428.7 189.67C429.32 191.42 429.64 194.38 431.11 195.66C436.07 199.97 454.96 195.68 458.81 203.06C460.27 205.86 457.68 208.76 455.68 210.44C451.75 213.77 442.19 218.47 440.09 222.88C438.05 227.16 441.68 239.32 442.47 244.29C442.94 247.25 442.61 251.66 439.15 252.75C430.27 255.53 424.42 241.67 417.64 240.08C415.58 239.6 413.34 242.59 411.87 243.77C407.65 247.17 399.4 255.81 393.31 252.64C390.99 251.43 391.81 247.36 391.95 245.25C392.3 240.13 396.86 228.13 395.76 224.04C394 217.57 376.03 212.78 375.58 204.08C375.38 200.37 378.97 200.15 381.76 199.57C388.09 198.26 396.28 198.72 402.04 195.6C409.99 191.27 407.74 172.67 416.05 169.4ZM72.92 206.44C72.47 210.89 66.22 213.11 63.38 216.04C59.8 219.72 59.13 225.13 54.47 227.93C48.94 226.17 50.08 219.18 47.4 215.15C45.03 211.57 38.24 214.05 36.16 210.05C32.79 203.57 42.74 203.65 45.76 200.73C49.19 197.42 48.52 189.33 53.22 187.23C58.47 184.89 59.34 195.67 61.32 198.86C64.05 203.25 69.83 202.59 72.92 206.44ZM188.23 233.96C182.14 234.75 187.24 245.97 189.3 248.59C198.85 260.74 218.76 255.8 221.84 240.73C222.5 237.48 220.6 231.1 216.15 234.62C214.34 236.05 214.69 239.32 213.94 241.36C212.29 245.9 207.76 248.2 203.17 248.11C198.69 248.02 194.81 245.11 193.31 240.87C192.24 237.85 192.94 233.35 188.23 233.96ZM338.29 243.04C342.57 244.25 342.75 250.66 345.77 253.43C348.01 255.49 355.69 256.66 354.62 260.87C353.45 265.42 347.42 263.72 344.91 266.62C342.72 269.16 344.43 276.86 339.69 277.21C333.99 277.63 334.79 269.88 332.44 266.76C330.34 263.96 24.01 264.83 322.81 261.47C322.25 259.91 322.95 258.11 324.29 257.16C326.59 255.53 329.69 255.53 331.66 253.34C334.44 250.23 334.99 245.79 338.29 243.04ZM175.24 259.68C168.7 260.79 161.62 262.91 157.31 268.35C142.42 287.19 160.44 314.74 183.53 310.89C194.98 308.98 205.41 298.72 205.63 286.86C205.89 271.89 190.48 257.09 175.24 259.68ZM174.72 267.16C186.93 265.06 201.18 277.19 197.83 290.12C195.95 297.4 188.24 302.58 181.18 303.89C166.45 306.61 154.87 288.34 161.18 275.52C163.67 270.46 169.47 268.07 174.72 267.16ZM71.87 284.39C80.12 282.12 82.83 297.04 75.17 298.98C65.89 301.33 62.78 286.89 71.87 284.39ZM458.46 304.26C463.06 307.2 461.4 312.75 464.52 316.57C467.5 320.22 473.39 319.02 476.08 323.33C474.76 327.48 468.21 328.43 465.24 331.29C462.27 334.15 462.9 341.42 458.37 342.63C452.75 344.14 452.81 334.34 450.71 331.35C448.16 327.71 441.96 328.64 439.91 324.73C440.45 319.07 447.6 320.21 450.81 317.33C454.76 313.77 453 306.59 458.46 304.26ZM95.75 347.63C98.41 347.39 99.55 350.52 100.41 352.44C102.87 357.96 103.37 364.56 106.44 369.8C110.21 376.24 125.4 373.57 127.88 380.57C128.91 383.47 126.38 385.1 124.25 386.32C119.66 388.95 111.66 390.13 108.04 393.5C103.23 397.97 104.02 416.04 96.56 416.09C87.95 416.15 88.54 398.62 84.87 393.68C81.19 388.69 64.98 390.59 63.37 382.96C62.85 380.47 66.02 378.97 67.66 377.93C72.21 375.06 80.54 374 84.13 370.52C88.37 366.41 88.47 358.08 90.82 352.87C91.81 350.68 92.99 347.88 95.75 347.63ZM187.27 431C192.45 431.08 190.58 437.6 193.42 440.4C195.63 442.58 203.77 441.92 202.86 446.75C202.13 450.56 195.35 450.26 193.38 453.11C191.78 455.43 191.49 462.42 187.73 462.52C183.65 462.62 184.04 454.68 181.76 452.51C179.5 450.34 174.34 451.1 172.92 448.21C172.23 446.83 172.99 445.02 174.08 444.12C176.32 442.28 179.69 442.31 181.59 439.94C183.83 437.17 184.31 433.21 187.27 431Z"
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    /** Apple style share box with upward arrow */
    /** Extension / Puzzle piece icon (for Sources and Addon modules) */
    val Extension: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_extension",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M5,19H18V14.4l1.13-0.55q0.4-0.2 0.64-0.55T20,12.5q0-0.43-0.24-0.79t-0.64-0.56L18,10.63V6H13.2L12.95,4.3q-0.08-0.55-0.49-0.92T11.5,3q-0.57,0-0.99,0.38T10.03,4.3L9.78,6H5V8.15q1.4,0.53 2.2,1.7Q8,11.02 8,12.5Q8,14 7.2,15.18T5,16.88V19Zm0,2q-0.85,0-1.42-0.57T3,19V15.2q1.2,0 2.1-0.76T6,12.5q0-1.15-0.9-1.9T3,9.8V6Q3,5.18 3.59,4.59T5,4H8.05Q8.23,2.72 9.2,1.86T11.5,1q1.3,0 2.28,0.86T14.95,4H18q0.82,0 1.41,0.59Q20,5.18 20,6V9.35q0.9,0.45 1.45,1.3T22,12.5q0,1.02-0.55,1.88T20,15.65V19q0,0.85-0.59,1.43T18,21H5Zm7.5-8.5Z"
                ).toNodes(),
                fill = stroke,
            )
        }.build()
    }

    /** Slowed & Nightcore tempo / speed stopwatch dial icon */
    val SpeedDial: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_speed_dial",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M23,20H15V18h8v2ZM9,3V1h6V3H9Zm3,19q-1.88,0-3.51-0.71T5.64,19.36T3.71,16.51T3,13T3.71,9.49T5.64,6.64T8.49,4.71T12,4q1.6,0 3.03,0.52t2.6,1.45l1.43-1.43l1.4,1.4L19.05,7.4q0.98,1.23 1.49,2.73T21,13.33q-0.35-0.1-0.77-0.22T19,12.98q0-0.25-0.01-0.49T18.93,12H14.9l-4.5,7.82q0.4,0.1 0.79,0.14T12,20q0.28,0 0.53-0.01t0.53-0.06q0.07,0.5 0.24,1t0.41,0.93q-0.43,0.07-0.85,0.11T12,22Zm0-11h6.7q-0.45-1.55-1.54-2.73T14.6,6.5L12,11Zm-1.75,1L13.6,6.2q-0.4-0.1-0.81-0.15T11.95,6Q10.83,6 9.73,6.36Q8.63,6.72 7.65,7.5l2.6,4.5ZM5.1,14h5.15L6.9,8.2Q5.85,9.38 5.34,10.86T5.1,14Zm4.3,5.5L12,15H5.3q0.45,1.55 1.54,2.73T9.4,19.5ZM12,13Zm1.05,6.93ZM19,12.98Z"
                ).toNodes(),
                fill = stroke,
            )
        }.build()
    }

    val ShareBox: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_share_box",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    "M6,22q-0.82,0-1.41-0.59T4,20V10Q4,9.17 4.59,8.59T6,8H9v2H6V20H18V10H15V8h3q0.82,0 1.41,0.59T20,10V20q0,0.82-0.59,1.41T18,22H6Zm5-6V4.82L9.4,6.43L8,5l4-4l4,4L14.6,6.43L13,4.82V16H11Z"
                ).toNodes(),
                fill = stroke,
            )
        }.build()
    }

    /** Album icon vector (from album-icon-8278985-512.svg) */
    val Album: ImageVector by lazy {
        ImageVector.Builder(
            name = "dhvani_album",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 512f, viewportHeight = 512f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(
                    ALBUM_PATH
                ).toNodes(),
                fill = stroke,
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()
    }

    private const val MIC_PATH = "M395.92 25.93C440.52 22.56 480.65 56.03 485.87 100.3C490.15 136.54 468.44 173.97 434.31 187.24C426.84 190.14 418.03 193.42 409.83 193.25C407.32 193.2 400.71 185.34 398.64 183.26C388.69 173.26 378.76 163.23 368.71 153.33C356.94 141.72 345.3 129.98 333.63 118.27C330.92 115.56 320.04 106.18 318.97 103.49C317.11 98.83 322.73 82.09 324.77 77.23C336.79 48.54 364.87 28.27 395.92 25.93ZM309.3 123.38C335.74 149.82 362.17 176.26 388.6 202.7C331.62 252.77 274.64 302.85 217.66 352.93C198.17 333.56 178.69 314.18 159.2 294.81C209.24 237.67 259.27 180.53 309.3 123.38ZM305.64 192.82C301.01 193.48 298.04 197.56 294.87 200.66C288.41 206.98 282.05 213.41 275.69 219.82C272.88 222.64 268.62 225.66 267.17 229.47C265.3 234.41 266.82 240.68 271.4 243.65C275.43 246.25 280.16 246.29 284.25 243.8C287.25 241.98 289.7 238.75 292.16 236.29C297.86 230.61 303.54 224.91 309.25 219.23C312.45 216.04 317.23 212.6 318.78 208.22C321.84 199.53 314.16 191.61 305.64 192.82ZM145.65 310.77C164.19 329.29 182.73 347.82 201.27 366.35C200.35 368.88 197.33 370.55 195.34 372.32C190.38 376.73 185.41 381.1 180.37 385.41C171.6 392.91 157.27 408.36 147.4 412.74C143.73 414.36 138.83 414.68 134.91 413.79C128.78 412.4 125.09 407.74 120.4 404.06C111.41 409.26 106.51 420.18 96.92 424.9C88.43 429.07 78.76 429.84 69.44 430.11C64.49 430.25 59.29 429.28 54.58 431.23C43.83 435.69 39.54 449.59 45.28 459.48C53.48 473.6 74.05 469.5 87.67 469.5C113.86 469.5 140.04 469.5 166.22 469.5C173.86 469.5 181.5 469.5 189.14 469.5C192.83 469.5 197.26 468.81 200.72 470.15C200.72 475.32 200.72 480.49 200.72 485.66C196.87 487.43 191.47 486.51 187.27 486.51C177.45 486.51 167.63 486.51 157.81 486.51C132.25 486.51 106.69 486.51 81.13 486.51C73.19 486.51 64.65 487.33 56.81 485.91C45.13 483.79 34.23 475.13 29.15 464.42C20.72 446.64 29.51 421.81 48.9 415.27C61.21 411.12 74.81 415.3 87.01 410.77C92.53 408.73 96.64 403.3 100.7 399.25C102.99 396.96 106.55 394.52 107.95 391.6C104.93 387.76 101.08 385 99.3 380.24C97.16 374.5 97.55 367.8 100.29 362.34C102.43 358.07 106.56 354.3 109.73 350.78C115.42 344.48 121.06 338.07 126.54 331.59C130.82 326.53 135.28 321.62 139.66 316.65C141.4 314.67 143.1 311.56 145.65 310.77Z"
    private const val ALBUM_PATH = "M159.35 25.93C165.65 24.84 172.46 25.49 178.85 25.49C190.85 25.49 202.85 25.49 214.85 25.49C251.79 25.49 288.73 25.49 325.67 25.49C341.07 25.49 366.48 21.68 370.05 42.68C370.47 45.09 371.02 47.31 370.75 49.8C368.09 73.97 341.63 69.83 324.27 69.83C285.77 69.83 247.27 69.83 208.77 69.83C197.4 69.83 186.02 69.83 174.64 69.83C168.91 69.83 162.28 70.78 156.79 68.87C135.31 61.37 136.34 29.9 159.35 25.93ZM125.68 87.17C132.73 85.73 141.26 87 148.46 87C164.2 87 179.94 87 195.68 87C245.09 87 294.5 87 343.91 87C355.22 87 380.37 85.13 390 88.14C399.77 91.19 405.73 101.63 404.92 111.52C404.17 120.75 396.59 128.63 387.83 130.85C381.12 132.55 363 131.17 355.13 131.17C350.45 131.17 345.78 131.17 341.1 131.17C292.16 131.17 243.22 131.17 194.28 131.17C178.85 131.17 163.42 131.17 147.99 131.17C140.6 131.17 131.89 132.43 124.65 130.8C100.66 125.41 101.66 92.08 125.68 87.17ZM129.8 151.24C154.02 150.26 178.46 151.16 202.7 151.16C250.23 151.16 297.77 151.16 345.31 151.16C360.37 151.16 376.54 149.52 391.41 152.19C412.6 155.99 431.2 168.97 441.72 187.87C451.75 205.92 450.56 226.16 450.57 246.18C450.6 276.42 450.54 306.65 450.58 336.89C450.6 355.44 450.57 373.99 450.57 392.53C450.57 421.05 451.35 444.39 429.63 465.87C407.93 487.32 384.61 486.51 356.06 486.51C336.27 486.51 316.47 486.51 296.68 486.51C251.64 486.51 206.59 486.51 161.55 486.51C146.88 486.51 131.6 487.67 117.13 484.88C99.6 481.49 83.03 469.75 73.27 454.87C62.06 437.79 61.45 419.43 61.45 399.55C61.45 388.33 61.45 377.1 61.45 365.88C61.45 350.14 61.45 334.4 61.45 318.66C61.45 294.34 61.45 270.03 61.45 245.71C61.45 231.16 60.21 216 64.23 201.87C72.44 172.96 100.09 152.45 129.8 151.24ZM252.81 209.69C235.89 210.62 219.68 214.09 204.73 222.28C192.17 229.16 180.26 238.31 171.28 249.58C125.21 307.36 147.37 393.59 216.32 420.76C230.5 426.35 246.38 428.63 261.61 427.99C276.28 427.37 291.9 423.23 304.8 416.34C338.03 398.61 360.19 367.43 364.76 329.88C372.49 266.37 316.72 206.18 252.81 209.69ZM254.66 245.22C264.46 244.76 274.93 246.95 283.85 250.82C298.18 257.02 310.46 267.04 318.78 280.42C341.57 317.05 325.96 367.11 287.26 385.18C277.59 389.7 266.75 392.38 256 392.47C211.87 392.81 176.09 351 183.13 307.41C186.8 284.7 200.98 265.65 220.73 254.17C230.97 248.22 242.91 245.78 254.66 245.22ZM252.81 289.18C221.18 291.74 216.82 337.01 246.4 347.2C250.38 348.57 254.58 349.17 258.81 348.79C289.2 346.08 295.68 302.35 267.34 291.15C262.71 289.32 257.78 288.77 252.81 289.18Z"
}
