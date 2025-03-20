package com.stripe.android.connect.webview

import android.graphics.Color
import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.util.getContrastingColor

@OptIn(PrivateBetaConnectSDK::class)
internal data class StripeConnectWebViewContainerState(
    /**
     * Non-null if we received the 'pageDidLoad' message,
     * null otherwise.
     */
    val pageViewId: String? = null,

    /**
     * The time the webview began loading, in milliseconds from midnight, January 1, 1970 UTC.
     */
    val didBeginLoadingMillis: Long? = null,

    /**
     * True if we received the 'setOnLoaderStart' message.
     */
    val receivedSetOnLoaderStart: Boolean = false,

    /**
     * True if the native loading indicator should be visible.
     */
    val isNativeLoadingIndicatorVisible: Boolean = false,

    /**
     * True if we received the 'closeWebView' message.
     */
    val receivedCloseWebView: Boolean = false,

    /**
     * The appearance to use for the view.
     */
    val appearance: Appearance? = null
) {
    /**
     * The background color of the view.
     */
    @ColorInt
    val backgroundColor: Int =
        appearance?.colors?.background ?: Color.WHITE

    /**
     * The color of the native loading indicator.
     */
    @ColorInt
    val nativeLoadingIndicatorColor: Int =
        appearance?.colors?.secondaryText ?: getContrastingColor(backgroundColor, 4.5f)
}
