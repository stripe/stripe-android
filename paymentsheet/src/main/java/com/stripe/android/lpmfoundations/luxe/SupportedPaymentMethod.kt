package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.core.strings.IdentifierResolvableString
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.ui.core.elements.SharedDataSpec

internal data class SupportedPaymentMethod(
    /**
     * This describes the PaymentMethod Type as described
     * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
     */
    val code: PaymentMethodCode,

    /** This describes the name that appears under the selector. */
    val displayName: ResolvableString,

    /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
    @DrawableRes val iconResource: Int,

    /** An optional light theme icon url if it's supported. */
    val lightThemeIconUrl: String?,

    /** An optional dark theme icon url if it's supported. */
    val darkThemeIconUrl: String?,

    /** Indicates if the lpm icon in the selector is a single color and should be tinted
     * on selection.
     */
    val tintIconOnSelection: Boolean,
) {
    constructor(
        paymentMethodDefinition: PaymentMethodDefinition,
        sharedDataSpec: SharedDataSpec? = null,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        tintIconOnSelection: Boolean = false,
    ) : this(
        code = paymentMethodDefinition.type.code,
        displayName = IdentifierResolvableString(id = displayNameResource),
        iconResource = iconResource,
        lightThemeIconUrl = sharedDataSpec?.selectorIcon?.lightThemePng,
        darkThemeIconUrl = sharedDataSpec?.selectorIcon?.darkThemePng,
        tintIconOnSelection = tintIconOnSelection,
    )

    constructor(
        code: PaymentMethodCode,
        @StringRes displayNameResource: Int,
        @DrawableRes iconResource: Int,
        tintIconOnSelection: Boolean = false,
        lightThemeIconUrl: String?,
        darkThemeIconUrl: String?,
    ) : this(
        code = code,
        displayName = IdentifierResolvableString(id = displayNameResource),
        iconResource = iconResource,
        lightThemeIconUrl = lightThemeIconUrl,
        darkThemeIconUrl = darkThemeIconUrl,
        tintIconOnSelection = tintIconOnSelection,
    )
}
