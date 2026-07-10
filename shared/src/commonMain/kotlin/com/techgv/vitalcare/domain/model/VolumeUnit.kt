package com.techgv.vitalcare.domain.model

import kotlin.math.roundToInt

/**
 * Display unit for fluid volumes (FR-SE6, D-032). Amounts are always stored in
 * canonical millilitres ([FluidEntry.amountMl]); this only governs entry and
 * display, so switching units never migrates data. US fluid ounce.
 */
enum class VolumeUnit {
    ML,
    OZ;

    /** Canonical mL → a value in this unit (mL stays integer; oz keeps 1 decimal). */
    fun fromMl(ml: Int): Double = when (this) {
        ML -> ml.toDouble()
        OZ -> ((ml / ML_PER_OZ) * 10.0).roundToInt() / 10.0
    }

    /** A value in this unit → canonical mL (rounded to the nearest mL). */
    fun toMl(value: Double): Int = when (this) {
        ML -> value.roundToInt()
        OZ -> (value * ML_PER_OZ).roundToInt()
    }

    /** Number-only display of a canonical mL amount in this unit (no unit word). */
    fun format(ml: Int): String = when (this) {
        ML -> ml.toString()
        OZ -> {
            val oz = fromMl(ml)
            if (oz % 1.0 == 0.0) oz.toInt().toString() else oz.toString()
        }
    }

    companion object {
        const val ML_PER_OZ = 29.5735
    }
}
