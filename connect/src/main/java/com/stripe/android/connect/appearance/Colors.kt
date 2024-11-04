package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
class Colors(
    /**
     * The primary color used throughout the components. If null the default will be used.
     */
    @ColorInt val primary: Int? = null,

    /**
     * The background color for components. If null the default will be used.
     */
    @ColorInt val background: Int? = null,

    /**
     * The primary text color used for regular content. If null the default will be used.
     */
    @ColorInt val text: Int? = null,

    /**
     * The secondary text color used for less emphasized content. If null the default will be used.
     */
    @ColorInt val secondaryText: Int? = null,

    /**
     * The color used to indicate errors or destructive actions. If null the default will be used.
     */
    @ColorInt val danger: Int? = null,

    /**
     * The color used for component borders. If null the default will be used.
     */
    @ColorInt val border: Int? = null,

    /**
     * The color used for primary actions and link text. If null the default will be used.
     */
    @ColorInt val actionPrimaryText: Int? = null,

    /**
     * The color used for secondary actions and link text. If null the default will be used.
     */
    @ColorInt val actionSecondaryText: Int? = null,

    /**
     * The background color used to highlight information. If null the default will be used.
     */
    @ColorInt val offsetBackground: Int? = null,

    /**
     * The background color used for form fields. If null the default will be used.
     */
    @ColorInt val formBackground: Int? = null,

    /**
     * The border color used to highlight form items when focused. If null the default will be used.
     */
    @ColorInt val formHighlightBorder: Int? = null,

    /**
     * The accent color used for filling form elements like checkboxes or radio buttons. If null the default will be used.
     */
    @ColorInt val formAccent: Int? = null,
) : Parcelable {
    internal companion object {
        internal val default = Colors()
    }
}
