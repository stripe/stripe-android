package com.stripe.android.financialconnections.error

import com.stripe.android.core.error.SentryConfig
import java.util.concurrent.TimeUnit

internal object FinancialConnectionsSentryConfig : SentryConfig {
    override val projectId: String = "826"
    override val key: String = "ebeff9f19e5648928a7fdd9eba27d456"
    override val secret: String = "614b2527312a415fad24d1eb6731f935"
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