package com.stripe.android.paymentelement.confirmation.shoppay

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayConfirmationOption(
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration,
    val customerSessionClientSecret: String,
    val merchantDisplayName: String
) : ConfirmationHandler.Option
