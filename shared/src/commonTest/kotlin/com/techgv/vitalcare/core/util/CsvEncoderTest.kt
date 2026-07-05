package com.techgv.vitalcare.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvEncoderTest {

    private val encoder = CsvEncoder()
    private val header = listOf(
        "date", "time", "spo2", "heart_rate", "systolic", "diastolic",
        "remarks", "created_at", "updated_at",
    )

    @Test
    fun encodesHeaderAndPlainRowInColumnOrder() {
        val csv = encoder.encode(
            header = header,
            rows = listOf(
                listOf("2026-07-03", "08:30", "98", "72", "120", "80", "resting", "1719990600000", "1719990600000"),
            ),
        )
        assertEquals(
            "date,time,spo2,heart_rate,systolic,diastolic,remarks,created_at,updated_at\n" +
                "2026-07-03,08:30,98,72,120,80,resting,1719990600000,1719990600000\n",
            csv,
        )
    }

    @Test
    fun nullFieldsBecomeEmptyCellsNotZero() {
        val csv = encoder.encode(
            header = listOf("a", "b", "c"),
            rows = listOf(listOf(null, "68", null)),
        )
        assertEquals("a,b,c\n,68,\n", csv)
    }

    @Test
    fun quotesFieldContainingComma() {
        val csv = encoder.encode(
            header = listOf("remarks"),
            rows = listOf(listOf("Morning, resting")),
        )
        assertEquals("remarks\n\"Morning, resting\"\n", csv)
    }

    @Test
    fun doublesEmbeddedQuotesAndWrapsField() {
        val csv = encoder.encode(
            header = listOf("remarks"),
            rows = listOf(listOf("felt \"dizzy\" today")),
        )
        assertEquals("remarks\n\"felt \"\"dizzy\"\" today\"\n", csv)
    }

    @Test
    fun quotesFieldContainingNewline() {
        val csv = encoder.encode(
            header = listOf("remarks"),
            rows = listOf(listOf("line1\nline2")),
        )
        assertEquals("remarks\n\"line1\nline2\"\n", csv)
    }

    @Test
    fun emptyRowsProducesHeaderOnly() {
        val csv = encoder.encode(header = listOf("a", "b"), rows = emptyList())
        assertEquals("a,b\n", csv)
    }
}
