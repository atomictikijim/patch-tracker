package com.prolocity.patchtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.ui.theme.Green40
import com.prolocity.patchtracker.ui.theme.LeagueBlue
import com.prolocity.patchtracker.ui.theme.LeagueGold
import com.prolocity.patchtracker.ui.theme.LeagueGray
import java.io.File

private data class PatchIconSpec(val icon: ImageVector, val color: Color)

private val EIGHT_BALL = PatchIconSpec(Icons.Filled.Adjust, LeagueBlue)
private val NINE_BALL = PatchIconSpec(Icons.Filled.Adjust, Green40)

private val ICON_SPECS: Map<String, PatchIconSpec> = mapOf(
    "division_rep" to PatchIconSpec(Icons.Filled.Groups, LeagueGold),
    "captain" to PatchIconSpec(Icons.Filled.Star, LeagueGold),
    "cocaptain" to PatchIconSpec(Icons.Filled.StarHalf, LeagueGold),

    "on_break_8" to EIGHT_BALL.copy(icon = Icons.Filled.Adjust),
    "break_run_8" to EIGHT_BALL.copy(icon = Icons.Filled.Bolt),
    "mini_slam_8" to EIGHT_BALL.copy(icon = Icons.Filled.LocalFireDepartment),
    "beat_8" to EIGHT_BALL,

    "on_break_9" to NINE_BALL.copy(icon = Icons.Filled.Adjust),
    "break_run_9" to NINE_BALL.copy(icon = Icons.Filled.Bolt),
    "mini_slam_9" to NINE_BALL.copy(icon = Icons.Filled.LocalFireDepartment),
    "shutout_9" to NINE_BALL,
    "beat_9" to NINE_BALL,

    "rackless" to PatchIconSpec(Icons.Filled.Repeat, LeagueBlue),
    "grand_slam" to PatchIconSpec(Icons.Filled.EmojiEvents, LeagueGold),
    "clean_sweep" to PatchIconSpec(Icons.Filled.CleaningServices, LeagueGray),
    "first_win" to PatchIconSpec(Icons.Filled.Celebration, LeagueGray),
    "sportsmanship" to PatchIconSpec(Icons.Filled.Handshake, LeagueGray),
    "beat_operator" to PatchIconSpec(Icons.Filled.SportsBar, LeagueGray),
    "mvp" to PatchIconSpec(Icons.Filled.WorkspacePremium, LeagueGold),
    "milestone" to PatchIconSpec(Icons.Filled.MilitaryTech, LeagueGold)
)

private val FALLBACK_SPEC = PatchIconSpec(Icons.Filled.WorkspacePremium, LeagueGray)

@Composable
fun PatchTypeIcon(patchType: PatchType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    PatchIcon(
        name = patchType.name,
        iconKey = patchType.iconKey,
        badgeText = patchType.badgeText,
        imagePath = patchType.imagePath,
        modifier = modifier,
        size = size
    )
}

@Composable
fun PatchIcon(
    name: String,
    iconKey: String?,
    badgeText: String?,
    imagePath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (!imagePath.isNullOrBlank()) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        return
    }

    val spec = ICON_SPECS[iconKey] ?: FALLBACK_SPEC
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(spec.color),
        contentAlignment = Alignment.Center
    ) {
        if (!badgeText.isNullOrBlank()) {
            // Scale the badge font to the icon size and text length so 4-character
            // badges (e.g. "1000", "20-0") never overflow a small icon.
            val fontSize = when {
                badgeText.length <= 2 -> size.value * 0.42f
                badgeText.length == 3 -> size.value * 0.34f
                else -> size.value * 0.28f
            }.sp
            Text(
                text = badgeText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = TextStyle(fontSize = fontSize)
            )
        } else {
            Icon(
                spec.icon,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}
