package com.techgv.vitalcare.core.util

/**
 * Minimal RFC 4180 CSV encoder (06 §3). No external library so the core stays
 * dependency-free (D-027). `\n` line endings, UTF-8 is the caller's concern.
 */
class CsvEncoder {

    /**
     * Encodes a header plus rows. Null cells become empty fields (never "0"),
     * and any field containing a comma, double quote, or line break is wrapped
     * in double quotes with embedded quotes doubled.
     */
    fun encode(header: List<String>, rows: List<List<String?>>): String = buildString {
        appendRow(header)
        rows.forEach { row ->
            append('\n')
            appendRow(row)
        }
        append('\n')
    }

    private fun StringBuilder.appendRow(row: List<String?>) {
        row.forEachIndexed { index, field ->
            if (index > 0) append(',')
            append(encodeField(field))
        }
    }

    private fun encodeField(field: String?): String {
        if (field.isNullOrEmpty()) return ""
        val needsQuoting = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return field
        return "\"${field.replace("\"", "\"\"")}\""
    }
}
