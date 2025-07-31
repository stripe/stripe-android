package com.stripe.android.paymentelement.confirmation.shoppay

import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayConfirmationOption(
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration,
    val customerSessionClientSecret: String,
    val businessName: String
) : ConfirmationHandler.Option
