package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

@Parcelize
internal class LinkConfirmationOption(
    val initializationMode: PaymentElementLoader.InitializationMode,
    val shippingDetails: AddressDetails?,
    val configuration: LinkConfiguration,
) : ConfirmationHandler.Option
