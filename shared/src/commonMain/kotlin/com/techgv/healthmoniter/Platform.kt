package com.techgv.healthmoniter

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform