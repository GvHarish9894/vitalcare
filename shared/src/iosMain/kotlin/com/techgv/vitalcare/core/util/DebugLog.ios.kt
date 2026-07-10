package com.techgv.vitalcare.core.util

import platform.Foundation.NSLog

actual fun debugLog(tag: String, message: String) {
    NSLog("%s", "$tag: $message")
}
