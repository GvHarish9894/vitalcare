package com.techgv.vitalcare.core.util

import android.util.Log

actual fun debugLog(tag: String, message: String) {
    Log.w(tag, message)
}
