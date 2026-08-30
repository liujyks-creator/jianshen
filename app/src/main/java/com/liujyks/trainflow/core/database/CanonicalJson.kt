package com.liujyks.trainflow.core.database

import java.math.BigDecimal

internal sealed interface CanonicalJsonValue {
    data class Obj(val fields: LinkedHashMap<String, CanonicalJsonValue>) : CanonicalJsonValue
    data class Arr(val values: List<CanonicalJsonValue>) : CanonicalJsonValue
    data class Str(val value: String) : CanonicalJsonValue
    data class Num(val value: BigDecimal) : CanonicalJsonValue
    data class Bool(val value: Boolean) : CanonicalJsonValue
    data object Null : CanonicalJsonValue
}

internal fun parseCanonicalJson(source: String): CanonicalJsonValue? = try {
    CanonicalJsonParser(source).parse()
} catch (_: CanonicalJsonParseException) {
    null
}

internal fun CanonicalJsonValue.renderCanonicalJson(): String = when (this) {
    is CanonicalJsonValue.Obj -> fields.entries.joinToString(
        separator = ",",
        prefix = "{",
        postfix = "}"
    ) { (key, value) -> "${key.escapeCanonicalJson().quoted()}:${value.renderCanonicalJson()}" }

    is CanonicalJsonValue.Arr -> values.joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]"
    ) { value -> value.renderCanonicalJson() }

    is CanonicalJsonValue.Str -> value.escapeCanonicalJson().quoted()
    is CanonicalJsonValue.Num -> value.canonicalNumber()
    is CanonicalJsonValue.Bool -> value.toString()
    CanonicalJsonValue.Null -> "null"
}

private fun BigDecimal.canonicalNumber(): String {
    val normalized = stripTrailingZeros()
    return if (normalized.compareTo(BigDecimal.ZERO) == 0) "0" else normalized.toPlainString()
}

private fun String.quoted(): String = "\"$this\""

private fun String.escapeCanonicalJson(): String = buildString {
    this@escapeCanonicalJson.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char.code < 0x20) {
                append("\\u")
                append(char.code.toString(16).padStart(4, '0'))
            } else {
                append(char)
            }
        }
    }
}

private class CanonicalJsonParser(
    private val source: String
) {
    private var index = 0

    fun parse(): CanonicalJsonValue {
        val value = parseValue()
        skipWhitespace()
        if (index != source.length) fail()
        return value
    }

    private fun parseValue(): CanonicalJsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> CanonicalJsonValue.Str(parseString())
            't' -> consumeLiteral("true", CanonicalJsonValue.Bool(true))
            'f' -> consumeLiteral("false", CanonicalJsonValue.Bool(false))
            'n' -> consumeLiteral("null", CanonicalJsonValue.Null)
            '-', in '0'..'9' -> parseNumber()
            else -> fail()
        }
    }

    private fun parseObject(): CanonicalJsonValue.Obj {
        consume('{')
        skipWhitespace()
        val fields = linkedMapOf<String, CanonicalJsonValue>()
        if (peek() == '}') {
            consume('}')
            return CanonicalJsonValue.Obj(fields)
        }
        while (true) {
            if (peek() != '"') fail()
            val key = parseString()
            if (fields.containsKey(key)) fail()
            skipWhitespace()
            consume(':')
            fields[key] = parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> {
                    consume(',')
                    skipWhitespace()
                }

                '}' -> {
                    consume('}')
                    return CanonicalJsonValue.Obj(fields)
                }

                else -> fail()
            }
        }
    }

    private fun parseArray(): CanonicalJsonValue.Arr {
        consume('[')
        skipWhitespace()
        val values = mutableListOf<CanonicalJsonValue>()
        if (peek() == ']') {
            consume(']')
            return CanonicalJsonValue.Arr(values)
        }
        while (true) {
            values += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> consume(',')
                ']' -> {
                    consume(']')
                    return CanonicalJsonValue.Arr(values)
                }

                else -> fail()
            }
        }
    }

    private fun parseString(): String {
        consume('"')
        return buildString {
            while (index < source.length) {
                when (val char = source[index++]) {
                    '"' -> return@buildString
                    '\\' -> append(parseEscape())
                    else -> {
                        if (char.code < 0x20) fail()
                        append(char)
                    }
                }
            }
            fail()
        }
    }

    private fun parseEscape(): Char = when (val escaped = source.getOrNull(index++)) {
        '"' -> '"'
        '\\' -> '\\'
        '/' -> '/'
        'b' -> '\b'
        'f' -> '\u000C'
        'n' -> '\n'
        'r' -> '\r'
        't' -> '\t'
        'u' -> parseUnicodeEscape()
        else -> fail()
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > source.length) fail()
        val hex = source.substring(index, index + 4)
        if (hex.any { char -> char !in HEX_DIGITS }) fail()
        index += 4
        return hex.toInt(16).toChar()
    }

    private fun parseNumber(): CanonicalJsonValue.Num {
        val start = index
        if (peek() == '-') index++
        when (peek()) {
            '0' -> {
                index++
                if (peek() in '0'..'9') fail()
            }

            in '1'..'9' -> while (peek() in '0'..'9') index++
            else -> fail()
        }
        if (peek() == '.') {
            index++
            if (peek() !in '0'..'9') fail()
            while (peek() in '0'..'9') index++
        }
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            if (peek() !in '0'..'9') fail()
            while (peek() in '0'..'9') index++
        }
        val raw = source.substring(start, index)
        return try {
            CanonicalJsonValue.Num(BigDecimal(raw))
        } catch (_: NumberFormatException) {
            fail()
        }
    }

    private fun <T : CanonicalJsonValue> consumeLiteral(
        literal: String,
        value: T
    ): T {
        if (!source.startsWith(literal, index)) fail()
        index += literal.length
        return value
    }

    private fun consume(expected: Char) {
        skipWhitespace()
        if (peek() != expected) fail()
        index++
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun skipWhitespace() {
        while (peek() == ' ' || peek() == '\n' || peek() == '\r' || peek() == '\t') index++
    }

    private fun fail(): Nothing = throw CanonicalJsonParseException()

    private companion object {
        val HEX_DIGITS = ('0'..'9') + ('a'..'f') + ('A'..'F')
    }
}

private class CanonicalJsonParseException : RuntimeException()
