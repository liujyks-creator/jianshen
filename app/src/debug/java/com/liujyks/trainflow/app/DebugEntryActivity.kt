package com.liujyks.trainflow.app

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

class DebugEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
    }

    private fun buildView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            addView(debugButton("进入 TrainFlow", MainActivity::class.java))
            addView(debugButton("HR Broadcast Smoke", HeartRateBroadcastSmokeActivity::class.java))
            addView(
                debugButton(
                    "E17-1 Band 9 HRS Revalidation",
                    E17Band9HrsRevalidationActivity::class.java
                )
            )
        }
    }

    private fun debugButton(label: String, activityClass: Class<*>): Button {
        return Button(this).apply {
            text = label
            setAllCaps(false)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                startActivity(Intent(this@DebugEntryActivity, activityClass))
            }
        }
    }
}
