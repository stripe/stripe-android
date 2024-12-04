package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentMethodConfirmationOption : ConfirmationHandler.Option {
    val initializationMode: PaymentElementLoader.InitializationMode
    val shippingDetails: AddressDetails?

    @Parcelize
    data class Saved(
        override val initializationMode: PaymentElementLoader.InitializationMode,
        override val shippingDetails: AddressDetails?,
        val paymentMethod: com.stripe.android.model.PaymentMethod,
        val optionsParams: PaymentMethodOptionsParams?,
    ) : PaymentMethodConfirmationOption

    @Parcelize
    data class New(
        override val initializationMode: PaymentElementLoader.InitializationMode,
        override val shippingDetails: AddressDetails?,
        val createParams: PaymentMethodCreateParams,
        val optionsParams: PaymentMethodOptionsParams?,
        val shouldSave: Boolean
    ) : PaymentMethodConfirmationOption
}
