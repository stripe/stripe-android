package com.stripe.android.lpmfoundations.luxe

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.uicore.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SupportedPaymentMethod(
    /**
     * This describes the PaymentMethod Type as described
     * https://stripe.com/docs/api/payment_intents/create#create_payment_intent-payment_method_types
     */
    val code: PaymentMethodCode,

    /** This describes if the LPM requires a mandate see [ConfirmPaymentIntentParams.mandateDataParams]. */
    val requiresMandate: Boolean,

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

    /**
     * This describes the requirements of the LPM including if it is supported with
     * PaymentIntents w/ or w/out SetupFutureUsage set, SetupIntent, or on-session when attached
     * to the customer object.
     */
    val requirement: PaymentMethodRequirements,

    /**
     * This describes how the UI should look.
     */
    val formSpec: LayoutSpec,

    /**
     * This forces the UI to render the required fields
     */
    val placeholderOverrideList: List<IdentifierSpec> = emptyList()
) {
    /**
     * Returns true if the payment method supports confirming from a saved
     * payment method of this type.  See [PaymentMethodRequirements] for
     * description of the values
     */
    fun supportsCustomerSavedPM() = requirement.getConfirmPMFromCustomer(code)
}
