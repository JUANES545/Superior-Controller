package com.example.superiorcontroller.bluetooth

data class KnownDevice(
    val name: String,
    val alias: String = "",
    val address: String,
    val lastUsedAt: Long = 0L,
    val lastProfile: String = ""
) {
    val displayName: String
        get() = alias.ifBlank { name }
}
