package com.stripe.android.connect.webview

internal data class StripeConnectWebViewContainerState(
    /**
     * True if we received the 'pageDidLoad' message.
     */
    val receivedPageDidLoad: Boolean = false,

    /**
     * True if we received the 'setOnLoaderStart' message.
     */
    val receivedSetOnLoaderStart: Boolean = false,

    /**
     * True if the native loading indicator should be visible.
     */
    val isNativeLoadingIndicatorVisible: Boolean = false,
)
