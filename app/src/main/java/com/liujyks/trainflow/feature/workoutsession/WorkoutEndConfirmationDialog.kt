package com.liujyks.trainflow.feature.workoutsession

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.liujyks.trainflow.ui.theme.TrainFlowError
import com.liujyks.trainflow.ui.theme.TrainFlowNeutral50

@Composable
internal fun WorkoutEndConfirmationDialog(
    title: String,
    text: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TrainFlowError)
            ) {
                Text(text = "确认结束", color = TrainFlowNeutral50)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "取消")
            }
        },
        containerColor = Color(0xFF182030),
        titleContentColor = TrainFlowNeutral50,
        textContentColor = TrainFlowNeutral50
    )
}
