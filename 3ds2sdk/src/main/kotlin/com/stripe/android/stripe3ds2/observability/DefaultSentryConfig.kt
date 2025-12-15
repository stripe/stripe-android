package com.stripe.android.stripe3ds2.observability

import java.util.concurrent.TimeUnit

/**
 * Configuration for https://sentry.corp.stripe.com/stripe/android-3ds2-sdk/
 */
internal object DefaultSentryConfig : SentryConfig {
    /**
     * Default Sentry configuration for error reporting.
     *
     * Note: These values are intentionally hardcoded and public. They are NOT sensitive credentials.
     *
     * These are write-only keys used exclusively to send error reports from the 3DS2 SDK to
     * Stripe's internal Sentry project for bug monitoring and debugging purposes.
     *
     * Security considerations:
     * - These keys can only be used to log errors to Stripe's Sentry instance (errors.stripe.com)
     * - They cannot be used to access any payment data or user information
     * - The worst-case scenario if compromised would be spam errors sent to Stripe's monitoring
     * - This is no different from bundling a Stripe publishable key (pk_*) with an application
     *
     * This has been reviewed and confirmed as expected behavior multiple times:
     * - RUN_MOBILESDK-517 (2021)
     * - RUN_MOBILESDK-2468 (2023)
     * - RUN_MOBILESDK-4299 (2025)
     *
     * For more information, see: https://forum.sentry.io/t/dsn-private-public/6297/2
     */
    override val key: String = "dcb428fea25c40e7b99f81ae5981ee6a"
    override val secret: String = "deca87e736574c5c83c07314051fd93a"
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
