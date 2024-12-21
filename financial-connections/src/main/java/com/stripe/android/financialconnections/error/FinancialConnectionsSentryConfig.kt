package com.stripe.android.financialconnections.error

import com.stripe.android.core.error.SentryConfig
import com.stripe.android.financialconnections.BuildConfig

internal object FinancialConnectionsSentryConfig : SentryConfig {
    override val projectId: String = "826"
    override val key: String = BuildConfig.FC_SENTRY_KEY
    override val version: String = "7"
}
