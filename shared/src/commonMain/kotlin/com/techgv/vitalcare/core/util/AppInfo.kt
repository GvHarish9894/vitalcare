package com.techgv.vitalcare.core.util

/** Build metadata for the About section (FR-SE4); provided per platform via Koin. */
data class AppInfo(val versionName: String)

/** Public links surfaced in the UI (FR-SE4). */
object AppLinks {
    const val REPOSITORY = "https://github.com/GvHarish9894/vitalcare"
}
