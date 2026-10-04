package com.efm.filemanager.ui.feature.statistics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.efm.filemanager.domain.model.FileCategory

/**
 * A fixed hue per [FileCategory] for the by-type stacked-share bar -- color follows the entity,
 * never its rank, so "Images" is always this same blue whether it's the biggest or smallest
 * slice that day. Validated for the adjacent-pairs gate a stacked bar needs (not the stricter
 * all-pairs gate a pie/donut would need, which is why this widget is a bar, not a donut) via the
 * dataviz skill's `validate_palette.js`: worst adjacent CVD ΔE 9.1 light / 8.4 dark, worst
 * adjacent normal-vision ΔE 19.6 light / 19.3 dark -- both clear of the hard-fail floors. Three
 * light-mode slots sit under 3:1 contrast by the palette's own design, which is why every use of
 * these colors ships beside a real text label (the legend rows), never color alone.
 */
private val LIGHT_COLORS =
    mapOf(
        FileCategory.IMAGE to Color(0xFF2A78D6),
        FileCategory.VIDEO to Color(0xFFEB6834),
        FileCategory.AUDIO to Color(0xFF1BAF7A),
        FileCategory.DOCUMENT to Color(0xFFEDA100),
        FileCategory.ARCHIVE to Color(0xFFE87BA4),
        FileCategory.APK to Color(0xFF008300),
        FileCategory.OTHER to Color(0xFF4A3AA7),
    )

private val DARK_COLORS =
    mapOf(
        FileCategory.IMAGE to Color(0xFF3987E5),
        FileCategory.VIDEO to Color(0xFFD95926),
        FileCategory.AUDIO to Color(0xFF199E70),
        FileCategory.DOCUMENT to Color(0xFFC98500),
        FileCategory.ARCHIVE to Color(0xFFD55181),
        FileCategory.APK to Color(0xFF008300),
        FileCategory.OTHER to Color(0xFF9085E9),
    )

@Composable
internal fun FileCategory.chartColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = if (isDark) DARK_COLORS else LIGHT_COLORS
    return palette[this] ?: MaterialTheme.colorScheme.outline
}
