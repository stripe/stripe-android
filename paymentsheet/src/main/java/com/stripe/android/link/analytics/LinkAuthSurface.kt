package com.stripe.android.link.analytics

/**
 * Where the customer is verifying their Link account. Logged as `link_auth_surface` with Link 2FA events by the
 * flows that set it.
 */
internal enum class LinkAuthSurface(val analyticsValue: String) {
    /**
     * The inline verification card in the Embedded Payment Element.
     */
    EmbeddedInline("embedded_inline"),

    /**
     * The Link dialog opened for the Embedded Payment Element's inline verification.
     */
    EmbeddedDialog("embedded_dialog"),
}
