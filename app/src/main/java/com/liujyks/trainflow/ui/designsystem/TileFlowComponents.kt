package com.liujyks.trainflow.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liujyks.trainflow.ui.theme.LocalTrainFlowSkin

internal data class TileFlowMetric(
    val label: String,
    val value: String
)

@Composable
internal fun currentPageHorizontalPadding(): Dp {
    return LocalTrainFlowSkin.current.tokens.pageHorizontalPaddingDp.dp
}

@Composable
internal fun currentSectionSpacing(): Dp {
    return LocalTrainFlowSkin.current.tokens.sectionSpacingDp.dp
}

@Composable
internal fun currentCardCorner(): Dp {
    return LocalTrainFlowSkin.current.tokens.cardCornerDp.dp
}

@Composable
internal fun currentProminentCardCorner(): Dp {
    return LocalTrainFlowSkin.current.tokens.prominentCardCornerDp.dp
}

@Composable
internal fun TileFlowCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    prominent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(
        if (prominent) currentProminentCardCorner() else currentCardCorner()
    )
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(if (prominent) 22.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            content = cardContent
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            content = cardContent
        )
    }
}

@Composable
internal fun TileFlowMetricStrip(
    metrics: List<TileFlowMetric>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMetrics.forEach { metric ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = metric.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (rowMetrics.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}
