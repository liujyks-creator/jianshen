package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin

internal data class TrainingExecutionBottomControlsSpec(
    val primaryButtonMinHeight: Dp,
    val secondaryButtonMinHeight: Dp,
    val rowSpacing: Dp,
    val verticalPadding: Dp,
    val fixedBottomContentReserve: Dp
)

@Composable
internal fun trainingExecutionBottomControlsSpec(): TrainingExecutionBottomControlsSpec {
    val skin = LocalTrainFlowSkin.current
    val primaryButtonMinHeight = maxOf(48.dp, skin.tokens.trainingButtonHeightDp.dp)
    val secondaryButtonMinHeight = maxOf(48.dp, skin.tokens.secondaryButtonHeightDp.dp)
    val rowSpacing = 10.dp
    val verticalPadding = 14.dp
    val bottomSafeAreaReserve = 32.dp
    val contentGapAboveControls = 16.dp
    val calculatedReserve = primaryButtonMinHeight +
        rowSpacing +
        secondaryButtonMinHeight +
        (verticalPadding * 2) +
        bottomSafeAreaReserve +
        contentGapAboveControls

    return TrainingExecutionBottomControlsSpec(
        primaryButtonMinHeight = primaryButtonMinHeight,
        secondaryButtonMinHeight = secondaryButtonMinHeight,
        rowSpacing = rowSpacing,
        verticalPadding = verticalPadding,
        fixedBottomContentReserve = maxOf(skin.tokens.executionControlReserveDp.dp, calculatedReserve)
    )
}
