package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodRegistry
import com.stripe.android.model.PaymentMethodCode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SupportedPaymentMethod(
    /**
     * This describes the PaymentMethod Type as described
     * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
     */
    val code: PaymentMethodCode,

    /** This describes the name that appears under the selector. */
    @StringRes val displayNameResource: Int,

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
    internal fun paymentMethodDefinition(): PaymentMethodDefinition {
        return requireNotNull(PaymentMethodRegistry.definitionsByCode[code])
    }
}
