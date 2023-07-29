package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

/**
 * Only common analytics field keys should be declared within this object.
 *
 * SDK-specific analytics field keys should live within the corresponding SDK module.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AnalyticsFields {
    const val ANALYTICS_UA = "analytics_ua"
    const val APP_NAME = "app_name"
    const val APP_VERSION = "app_version"
    const val BINDINGS_VERSION = "bindings_version"
    const val IS_DEVELOPMENT = "is_development"
    const val DEVICE_TYPE = "device_type"
    const val EVENT = "event"
    const val OS_NAME = "os_name"
    const val OS_RELEASE = "os_release"
    const val OS_VERSION = "os_version"
    const val PUBLISHABLE_KEY = "publishable_key"
    const val SESSION_ID = "session_id"
    const val NETWORK_TYPE = "network_type"
}
