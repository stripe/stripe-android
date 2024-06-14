package com.stripe.android.financialconnections.error

import com.stripe.android.core.error.SentryConfig
import com.stripe.android.financialconnections.BuildConfig
import java.util.concurrent.TimeUnit

internal object FinancialConnectionsSentryConfig : SentryConfig {
    override val projectId: String = "826"
    override val key: String = BuildConfig.FC_SENTRY_KEY
    override val secret: String = BuildConfig.FC_SENTRY_SECRET
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