package com.stripe.link.core.data.storage

/**
 * Local (device-only) feature flags that can be toggled without a server-side configuration change.
 * Each entry maps to a boolean preference stored via [LocalFeatureFlagsStore].
 */
enum class LocalFeatureFlag(
    val key: String,
    val displayName: String,
    val sublabel: String,
    val defaultValue: Boolean,
) {
    EmailClickTracking(
        key = "email_click_tracking",
        displayName = "Email Click Tracking",
        sublabel = "POST to /api/email_click on deep link open when ref+eid present",
        defaultValue = false,
    ),
    ;
}

/**
 * Returns true if this flag is currently enabled according to [LocalFeatureFlagsStore].
 */
val LocalFeatureFlag.isEnabled: Boolean
    get() = LocalFeatureFlagsStore.getValue(this)
