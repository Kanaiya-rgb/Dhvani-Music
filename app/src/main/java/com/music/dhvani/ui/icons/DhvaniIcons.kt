package com.music.dhvani.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = if (filled) stroke else null,
            ) {
                // Two lobes meeting at the top notch, falling to a single point.
                moveTo(12f, 20f)
                curveTo(12f, 20f, 3.2f, 14.6f, 3.2f, 8.9f)
                arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.8f, -1.5f)
                arcToRelative(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.8f, 1.5f)
                curveTo(20.8f, 14.6f, 12f, 20f, 12f, 20f)
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
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(
                stroke = stroke,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3f, 3f)
                lineTo(9f, 11f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 0f)
                lineTo(15f, 6f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3f, -3f)
                close()
                moveTo(5f, 10f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 14f, 0f)
                moveTo(12f, 17f); lineTo(12f, 21f)
            }
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
}
