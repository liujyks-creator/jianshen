package com.liujyks.trainflow.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.liujyks.trainflow.core.model.HeartRateState
import kotlinx.coroutines.launch

/**
 * Debug entry retained for compatibility. It only observes the same Application owner used by
 * MainActivity and never creates a scanner, callback, attempt, or GATT resource.
 */
class HeartRateBroadcastSmokeActivity : ComponentActivity() {
    private val sharedOwner
        get() = (application as TrainFlowApplication).heartRateRuntimeOwner

    private lateinit var stateView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedOwner.heartRateState.collect(::render)
            }
        }
    }

    private fun buildView(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        root.addView(TextView(this).apply {
            text = "TrainFlow shared heart-rate owner"
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = """
                This page is observation-only. It uses the same process owner as TrainFlow and
                cannot scan, connect, disconnect, or allocate BLE resources. Use 心率与设备 in
                TrainFlow settings for explicit permission, scan, saved-device, and manual
                connection actions.
            """.trimIndent()
            textSize = 14f
        })
        stateView = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
        }
        root.addView(stateView)
        return ScrollView(this).apply {
            addView(
                root,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun render(state: HeartRateState) {
        stateView.text = buildString {
            append("fact=")
            append(state.fact?.contractValue ?: "invalid")
            state.sourceLabel?.let {
                append("\nsource=")
                append(it)
            }
            state.bpm?.let {
                append("\nbpm=")
                append(it)
            }
            state.measuredAt?.let {
                append("\nmeasuredAt=")
                append(it)
            }
        }
    }
}
