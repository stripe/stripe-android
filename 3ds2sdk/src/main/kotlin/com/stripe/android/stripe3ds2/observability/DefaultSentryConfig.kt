package com.stripe.android.stripe3ds2.observability

import java.util.concurrent.TimeUnit

/**
 * Configuration for https://sentry.corp.stripe.com/stripe/android-3ds2-sdk/
 */
internal object DefaultSentryConfig : SentryConfig {
    override val projectId: String = "426"
    override val key: String = "dcb428fea25c40e7b99f81ae5981ee6a"
    override val secret: String = "deca87e736574c5c83c07314051fd93a"
    override val version: String = "7"

    /**
     * If [System.currentTimeMillis] returns `1600285647423`, this method will return
     * `"1600285647.423"`.
     */
    override fun getTimestamp(): String {
        val timestamp = System.currentTimeMillis()
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timestamp)
        val fraction = timestamp - TimeUnit.SECONDS.toMillis(seconds)
        return "$seconds.$fraction"
    }
}
