package com.stripe.android.connect.webview

import android.graphics.Color
import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.util.getContrastingColor

@OptIn(PrivateBetaConnectSDK::class)
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
