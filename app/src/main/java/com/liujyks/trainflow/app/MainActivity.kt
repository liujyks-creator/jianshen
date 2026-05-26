package com.liujyks.trainflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liujyks.trainflow.ui.shell.official.TrainFlowApp
import com.liujyks.trainflow.ui.theme.TrainFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrainFlowTheme {
                TrainFlowApp()
            }
        }
    }
}
