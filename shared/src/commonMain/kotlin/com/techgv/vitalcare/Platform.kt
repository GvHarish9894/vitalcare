package com.techgv.vitalcare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform