package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
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

    /** This describes the image in the LPM selector. */
    val paymentMethodIcon: PaymentMethodIcon,

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
        displayName = resolvableString(id = displayNameResource),
        paymentMethodIcon = PaymentMethodIcon.create(
            iconResource = iconResource,
            lightThemeIconUrl = sharedDataSpec?.selectorIcon?.lightThemePng,
            darkThemeIconUrl = sharedDataSpec?.selectorIcon?.darkThemePng,
        ),
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
        displayName = resolvableString(id = displayNameResource),
        paymentMethodIcon = PaymentMethodIcon.create(
            iconResource = iconResource,
            lightThemeIconUrl = lightThemeIconUrl,
            darkThemeIconUrl = darkThemeIconUrl,
        ),
        tintIconOnSelection = tintIconOnSelection,
    )
}
