package com.stripe.android.stripe3ds2.transaction

class MessageVersionRegistry {
    val current: String
        get() = CURRENT

    fun isSupported(messageVersion: String?): Boolean {
        return SUPPORTED.contains(messageVersion)
    }

    private companion object {
        private const val CURRENT = "2.1.0"
        private val SUPPORTED = setOf(CURRENT)
    }
}
