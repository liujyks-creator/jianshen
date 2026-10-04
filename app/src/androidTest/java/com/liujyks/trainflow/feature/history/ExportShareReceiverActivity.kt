package com.liujyks.trainflow.feature.history

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

/** Runs as the test package's own UID, outside target instrumentation. */
class ExportShareReceiverActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(intent.action == Intent.ACTION_SEND && intent.type == "application/json")
        val uri = requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        val name = requireNotNull(uri.lastPathSegment)
        val operationId = requireNotNull(Regex("shared--?[0-9]+-([0-9a-f-]{36})\\.json").matchEntire(name)).groupValues[1]
        val folder = File(filesDir, "e21-s04-evidence/$operationId")
        check(folder.mkdirs())
        val bytes = requireNotNull(contentResolver.openInputStream(uri)).use { it.readBytes() }
        val original = File(folder, "received.json")
        original.writeBytes(bytes)
        val writeDenied = try {
            contentResolver.openFileDescriptor(uri, "w").use { }
            false
        } catch (_: SecurityException) { true }
        val counter = File(filesDir, "e21-s04-evidence/receiver-count.txt")
        val count = if (counter.exists()) counter.readText().toInt() + 1 else 1
        counter.writeText(count.toString())
        val targetUid = packageManager.getApplicationInfo("com.liujyks.trainflow", 0).uid
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val record = JSONObject().put("operationId", operationId).put("receiverUid", Process.myUid())
            .put("targetUid", targetUid).put("uri", uri.toString()).put("clipUri", intent.clipData?.getItemAt(0)?.uri.toString())
            .put("flags", intent.flags).put("readSuccess", true).put("writeDenied", writeDenied)
            .put("size", bytes.size).put("sha256", hash).put("count", count)
            .put("originalRelativePath", "files/e21-s04-evidence/$operationId/received.json")
        File(folder, "receiver.json").writeText(record.toString(), Charsets.UTF_8)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@ExportShareReceiverActivity).apply {
                text = "E21-S04 文件接收\nreadSuccess=true\nwriteDenied=$writeDenied\nreceiverUid=${Process.myUid()}\ntargetUid=$targetUid\ncount=$count\noperationId=$operationId\nsize=${bytes.size}\nsha256=$hash"
            })
            addView(Button(this@ExportShareReceiverActivity).apply { text = "返回App"; setOnClickListener { finish() } })
        })
    }
}
