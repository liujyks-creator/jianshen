package com.liujyks.trainflow.app

import java.util.Locale
import java.util.UUID

internal object E17HrsEvidenceFormatter {
    fun bytes(value: ByteArray): String =
        value.joinToString(separator = " ") { byte ->
            String.format(Locale.US, "%02X", byte.toInt() and 0xFF)
        }

    fun uuid(value: UUID): String {
        val text = value.toString().lowercase(Locale.US)
        return if (text.startsWith("0000") && text.endsWith("-0000-1000-8000-00805f9b34fb")) {
            "0x${text.substring(4, 8).uppercase(Locale.US)}"
        } else {
            text
        }
    }

    fun characteristicProperties(properties: Int): String {
        val modes = buildList {
            if (properties and PROPERTY_BROADCAST != 0) add("broadcast")
            if (properties and PROPERTY_READ != 0) add("read")
            if (properties and PROPERTY_WRITE_NO_RESPONSE != 0) add("write_no_response")
            if (properties and PROPERTY_WRITE != 0) add("write")
            if (properties and PROPERTY_NOTIFY != 0) add("notify")
            if (properties and PROPERTY_INDICATE != 0) add("indicate")
            if (properties and PROPERTY_SIGNED_WRITE != 0) add("signed_write")
            if (properties and PROPERTY_EXTENDED != 0) add("extended")
        }
        return "0x${properties.toString(16).uppercase(Locale.US)} modes=[${modes.joinToString()}]"
    }

    fun notifyLine(
        sourceLabel: String,
        sourceIdentifier: String,
        rawPayload: ByteArray,
        parsedBpm: Int?,
        flags: Int?,
        format: String?
    ): String = buildString {
        append("NOTIFY source_label=")
        append(quoted(sourceLabel))
        append(" source_identifier=")
        append(quoted(sourceIdentifier))
        append(" characteristic=0x2A37 raw=")
        append(quoted(bytes(rawPayload)))
        append(" parsed_bpm=")
        append(parsedBpm?.toString() ?: "null")
        append(" flags=")
        append(flags?.let { "0x${it.toString(16).padStart(2, '0').uppercase(Locale.US)}" } ?: "null")
        append(" format=")
        append(format ?: "unknown")
    }

    private fun quoted(value: String): String = "\"${value.replace("\"", "'")}\""

    private const val PROPERTY_BROADCAST = 0x01
    private const val PROPERTY_READ = 0x02
    private const val PROPERTY_WRITE_NO_RESPONSE = 0x04
    private const val PROPERTY_WRITE = 0x08
    private const val PROPERTY_NOTIFY = 0x10
    private const val PROPERTY_INDICATE = 0x20
    private const val PROPERTY_SIGNED_WRITE = 0x40
    private const val PROPERTY_EXTENDED = 0x80
}
