package com.liujyks.trainflow.ui.shell.official

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.liujyks.trainflow.feature.exerciselibrary.ExerciseLibraryRoute

@Composable
fun TrainFlowApp() {
    Surface {
        ExerciseLibraryRoute()
    }
}
