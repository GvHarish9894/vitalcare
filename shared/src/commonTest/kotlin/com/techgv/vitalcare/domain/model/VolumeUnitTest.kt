package com.techgv.vitalcare.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeUnitTest {

    @Test
    fun mlIsIdentity() {
        assertEquals(250.0, VolumeUnit.ML.fromMl(250))
        assertEquals(250, VolumeUnit.ML.toMl(250.0))
        assertEquals("250", VolumeUnit.ML.format(250))
    }

    @Test
    fun ozConvertsAndRounds() {
        // 250 mL ≈ 8.45 fl oz → one decimal.
        assertEquals(8.5, VolumeUnit.OZ.fromMl(250))
        // 8 fl oz ≈ 236.6 mL → nearest mL.
        assertEquals(237, VolumeUnit.OZ.toMl(8.0))
        assertEquals("8.5", VolumeUnit.OZ.format(250))
    }

    @Test
    fun ozFormatDropsTrailingZero() {
        // 30 mL ≈ 1.0 fl oz → shown as "1", not "1.0".
        assertEquals("1", VolumeUnit.OZ.format(30))
        // A whole-ounce value displays without a decimal point.
        val whole = VolumeUnit.OZ.toMl(2.0) // 2 fl oz in mL
        assertEquals("2", VolumeUnit.OZ.format(whole))
    }

    @Test
    fun roundTripThroughOzStaysClose() {
        val ml = 500
        val oz = VolumeUnit.OZ.fromMl(ml)
        // Converting back lands within rounding tolerance of the original.
        val backToMl = VolumeUnit.OZ.toMl(oz)
        assertEquals(true, kotlin.math.abs(backToMl - ml) <= 2)
    }
}
