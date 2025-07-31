package com.stripe.android.elements.payment

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Defines a custom payment method type that can be displayed in Payment Element.
 */
@Poko
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomPaymentMethod internal constructor(
    val id: String,
    internal val subtitle: ResolvableString?,
    internal val disableBillingDetailCollection: Boolean,
) : Parcelable {
    @ExperimentalCustomPaymentMethodsApi
    constructor(
        /**
         * The unique identifier for this custom payment method type in the format of "cmpt_...".
         *
         * Obtained from the Stripe Dashboard at https://dashboard.stripe.com/settings/custom_payment_methods
         */
        id: String,

        /**
         * Optional subtitle text to be displayed below the custom payment method's display name.
         */
        @StringRes subtitle: Int?,

        /**
         * When true, Payment Element will not collect billing details for this custom payment method type
         * irregardless of the [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] settings.
         *
         * This has no effect if [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] is not
         * configured.
         */
        disableBillingDetailCollection: Boolean = true,
    ) : this(
        id = id,
        subtitle = subtitle?.resolvableString,
        disableBillingDetailCollection = disableBillingDetailCollection,
    )

    @ExperimentalCustomPaymentMethodsApi
    constructor(
        /**
         * The unique identifier for this custom payment method type in the format of "cmpt_...".
         *
         * Obtained from the Stripe Dashboard at https://dashboard.stripe.com/settings/custom_payment_methods
         */
        id: String,

        /**
         * Optional subtitle text string resource to be resolved and displayed below the custom payment method's
         * display name.
         */
        subtitle: String?,

        /**
         * When true, Payment Element will not collect billing details for this custom payment method type
         * irregardless of the [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] settings.
         *
         * This has no effect if [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] is not
         * configured.
         */
        disableBillingDetailCollection: Boolean = true,
    ) : this(
        id = id,
        subtitle = subtitle?.resolvableString,
        disableBillingDetailCollection = disableBillingDetailCollection,
    )
}
