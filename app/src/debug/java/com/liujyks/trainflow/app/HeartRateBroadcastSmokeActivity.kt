package com.liujyks.trainflow.app

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Legacy launcher retained so existing debug bookmarks and permission-boundary tests stay valid.
 *
 * E17-7b deliberately removes the Activity-owned BLE runtime. Production validation now uses the
 * same Application owner as the normal TrainFlow UI; this page must never create scanner/GATT
 * resources or become a second owner.
 */
class HeartRateBroadcastSmokeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                addView(TextView(context).apply {
                    text = "心率广播旧测试入口"
                    textSize = 22f
                })
                addView(TextView(context).apply {
                    text = "此入口已停止独立持有蓝牙资源。请使用 TrainFlow 设置页测试同一个 Application 心率连接。"
                    textSize = 16f
                })
            }
        )
    }
}
