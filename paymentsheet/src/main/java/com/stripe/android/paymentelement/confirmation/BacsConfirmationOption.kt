package com.stripe.android.paymentelement.confirmation

import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BacsConfirmationOption(
    val initializationMode: PaymentElementLoader.InitializationMode,
    val shippingDetails: AddressDetails?,
    val createParams: PaymentMethodCreateParams,
    val optionsParams: PaymentMethodOptionsParams?,
    val appearance: PaymentSheet.Appearance,
) : ConfirmationHandler.Option
