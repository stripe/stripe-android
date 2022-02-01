package com.stripe.android.stripe3ds2.observability

interface SentryConfig {
    val projectId: String

    val key: String

    val secret: String

    val version: String

    fun getTimestamp(): String
}
