package com.stripe.android.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

internal class StripeIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API
    override val issues = listOf(
        ComposeCollectAsStateUsageDetector.ISSUE,
        DangerousManifestConfigurationDetector.ISSUE,
        ComposeCleanupRuleUsageDetector.ISSUE,
    )

    override val vendor = Vendor(
        vendorName = "Stripe Android SDK",
        identifier = "com.stripe.android",
        feedbackUrl = "https://github.com/stripe/stripe-android/issues/new/choose"
    )
}
