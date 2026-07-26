package com.liujyks.trainflow.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Historical E17-1 evidence entry retained for existing debug navigation.
 *
 * Its recorded evidence remains bound to the original immutable APK/source identity. E17-7b
 * removes every Activity-owned BLE resource so debug and production cannot run competing owners.
 */
class E17Band9HrsRevalidationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                addView(TextView(context).apply {
                    text = "E17-1 Band 9 历史证据"
                    textSize = 22f
                })
                addView(TextView(context).apply {
                    text =
                        "此入口不再扫描、连接或持有 GATT。历史证据仍绑定原测试 APK；" +
                            "当前 production 验证请使用 TrainFlow 设置页中的同一个 Application 心率连接。"
                    textSize = 16f
                })
            }
        )
    }
}
