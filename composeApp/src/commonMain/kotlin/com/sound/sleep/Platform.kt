package com.sound.sleep

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform