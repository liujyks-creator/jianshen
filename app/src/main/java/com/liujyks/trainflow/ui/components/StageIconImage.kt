package com.liujyks.trainflow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.liujyks.trainflow.R

@Composable
fun StageIconImage(
    iconKey: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Image(
        painter = painterResource(id = stageIconDrawableRes(iconKey)),
        contentDescription = null,
        modifier = modifier,
        colorFilter = ColorFilter.tint(color)
    )
}

@DrawableRes
fun stageIconDrawableRes(iconKey: String): Int {
    return when (iconKey.trim()) {
        "warmup" -> R.drawable.stage_icon_warmup
        "work" -> R.drawable.stage_icon_work
        "speed_up" -> R.drawable.stage_icon_speed_up
        "sprint" -> R.drawable.stage_icon_sprint
        "rest" -> R.drawable.stage_icon_rest
        "recover_breathe" -> R.drawable.stage_icon_recover_breathe
        "cooldown" -> R.drawable.stage_icon_cooldown
        "strength" -> R.drawable.stage_icon_strength
        "mobility" -> R.drawable.stage_icon_mobility
        "custom" -> R.drawable.stage_icon_custom
        else -> R.drawable.stage_icon_custom
    }
}
