package com.stripe.android.stripe3ds2.observability

interface SentryConfig {
    val projectId: String

    val key: String

    val version: String
}
