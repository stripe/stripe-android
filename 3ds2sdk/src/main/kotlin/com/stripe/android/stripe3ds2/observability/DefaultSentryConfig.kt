package com.stripe.android.stripe3ds2.observability

import java.util.concurrent.TimeUnit

/**
 * Configuration for https://sentry.corp.stripe.com/stripe/android-3ds2-sdk/
 */
internal object DefaultSentryConfig : SentryConfig {
    /**
     * Default Sentry configuration for error reporting.
     *
     * ## What this is
     * This is a write-only DSN (Data Source Name) key that allows the 3DS2 SDK to send error reports
     * to Stripe's internal Sentry instance for monitoring and debugging.
     *
     * It is equivalent to exposing a Stripe publishable key
     *
     * ## Security
     * The key can ONLY be used to send error events to errors.stripe.com. It cannot:
     * - Access or read any data from Sentry
     * - Access payment information or user data
     * - Be used for authentication to any Stripe services
     *
     * This has been reviewed and confirmed as expected behavior multiple times:
     * - RUN_MOBILESDK-517 (2021)
     * - RUN_MOBILESDK-2468 (2023)
     * - RUN_MOBILESDK-4299 (2025)
     * - RUN_MOBILESDK-4960 (2025)
     *
     * For more information, see:
     * https://docs.sentry.io/concepts/key-terms/dsn-explainer/
     */
    override val publicKey: String = "dcb428fea25c40e7b99f81ae5981ee6a"
    override val projectId: String = "426"
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
