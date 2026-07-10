package com.techgv.vitalcare.core.util

import android.util.Log

actual fun debugLog(tag: String, message: String) {
    // Log.w throws "not mocked" under JVM unit tests; fall back to stdout there.
    try {
        Log.w(tag, message)
    } catch (_: Throwable) {
        println("$tag: $message")
    }
}
