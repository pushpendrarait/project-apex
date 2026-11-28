package com.example.projectapex.network

/**
 * Lightweight, zero-dependency RFC-8259 JSON parser and serializer.
 * Avoids Android's org.json unit-test stub ("Method not mocked") so API parsing
 * executes identically on Android devices and offline JVM JUnit tests.
 */
internal object ApexJson {

    fun parse(json: String): Any? {
        val parser = Parser(json)
        val value = parser.parseValue()
        parser.skipWhitespace()
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(json: String): Map<String, Any?> {
        return parse(json) as? Map<String, Any?> ?: emptyMap()
    }

    @Suppress("UNCHECKED_CAST")
    fun parseArray(json: String): List<Any?> {
        return parse(json) as? List<Any?> ?: emptyList()
    }

    fun encode(value: Any?): String = buildString {
        appendJson(value)
    }

    private fun StringBuilder.appendJson(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(if (value) "true" else "false")
            is Number -> append(value.toString())
            is String -> appendQuoted(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) append(',')
                    first = false
                    appendQuoted(k.toString())
                    append(':')
                    appendJson(v)
                }
                append('}')
            }
            is Iterable<*> -> {
                append('[')
                var first = true
                for (item in value) {
                    if (!first) append(',')
                    first = false
                    appendJson(item)
                }
                append(']')
            }
            else -> appendQuoted(value.toString())
        }
    }

    private fun StringBuilder.appendQuoted(str: String) {
        append('"')
        for (ch in str) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch < ' ') {
                        append(String.format("\\u%04x", ch.code))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    private class Parser(private val input: String) {
        private var pos = 0

        fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) return null
            return when (val ch = input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Unexpected character '$ch' at index $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            pos++ // '{'
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (pos < input.length && input[pos] == '}') {
                pos++
                return map
            }
            while (pos < input.length) {
                skipWhitespace()
                if (pos >= input.length || input[pos] != '"') {
                    throw IllegalArgumentException("Expected string key at index $pos")
                }
                val key = parseString()
                skipWhitespace()
                if (pos >= input.length || input[pos] != ':') {
                    throw IllegalArgumentException("Expected ':' at index $pos")
                }
                pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') {
                    pos++
                } else if (pos < input.length && input[pos] == '}') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            pos++ // '['
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (pos < input.length && input[pos] == ']') {
                pos++
                return list
            }
            while (pos < input.length) {
                list.add(parseValue())
                skipWhitespace()
                if (pos < input.length && input[pos] == ',') {
                    pos++
                } else if (pos < input.length && input[pos] == ']') {
                    pos++
                    break
                } else {
                    break
                }
            }
            return list
        }

        private fun parseString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < input.length) {
                val ch = input[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) break
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 <= input.length) {
                                    val hex = input.substring(pos, pos + 4)
                                    sb.append(hex.toInt(16).toChar())
                                    pos += 4
                                }
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }

        private fun parseBoolean(): Boolean {
            return if (input.startsWith("true", pos)) {
                pos += 4
                true
            } else if (input.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at index $pos")
            }
        }

        private fun parseNull(): Any? {
            if (input.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at index $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (input[pos] == '-') pos++
            while (pos < input.length && input[pos].isDigit()) pos++
            var isFloat = false
            if (pos < input.length && input[pos] == '.') {
                isFloat = true
                pos++
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                isFloat = true
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                while (pos < input.length && input[pos].isDigit()) pos++
            }
            val raw = input.substring(start, pos)
            return if (isFloat) raw.toDouble() else raw.toLongOrNull() ?: raw.toDouble()
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.obj(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.arr(key: String): List<Any?>? = this[key] as? List<Any?>

internal fun Map<String, Any?>.str(key: String): String? = when (val v = this[key]) {
    null -> null
    is String -> v
    else -> v.toString()
}

internal fun Map<String, Any?>.bool(key: String, default: Boolean = false): Boolean = when (val v = this[key]) {
    is Boolean -> v
    is String -> v.equals("true", ignoreCase = true)
    else -> default
}

internal fun Map<String, Any?>.lng(key: String, default: Long = 0L): Long = when (val v = this[key]) {
    is Number -> v.toLong()
    is String -> v.toLongOrNull() ?: default
    else -> default
}

internal fun Map<String, Any?>.dbl(key: String, default: Double = 0.0): Double = when (val v = this[key]) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull() ?: default
    else -> default
}

internal fun Map<String, Any?>.intVal(key: String, default: Int = 0): Int = when (val v = this[key]) {
    is Number -> v.toInt()
    is String -> v.toIntOrNull() ?: default
    else -> default
}

internal fun Map<String, Any?>.strList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
