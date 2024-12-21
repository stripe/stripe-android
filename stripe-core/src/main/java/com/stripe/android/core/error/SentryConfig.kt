package com.stripe.android.core.error

interface SentryConfig {
    val projectId: String

    val key: String

    val version: String
}
