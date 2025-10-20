package com.stripe.android.link

/**
 * Enum representing the different modes for Link Express functionality.
 */
internal enum class LinkExpressMode {
    /**
     * Link Express is disabled.
     */
    DISABLED,

    /**
     * Link Express is enabled.
     */
    ENABLED,

    /**
     * Link Express is enabled, but on attestation failures, we don't want to fall back to Link web
     */
    ENABLED_NO_WEB_FALLBACK
}
