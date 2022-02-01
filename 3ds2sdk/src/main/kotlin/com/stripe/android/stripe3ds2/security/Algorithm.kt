package com.stripe.android.stripe3ds2.security

internal enum class Algorithm(private val key: String) {
    EC("EC"),
    RSA("RSA");

    override fun toString(): String {
        return key
    }
}
