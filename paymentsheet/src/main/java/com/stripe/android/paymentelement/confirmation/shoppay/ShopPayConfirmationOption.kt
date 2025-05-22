package com.stripe.android.paymentelement.confirmation.shoppay

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayConfirmationOption(
    val checkoutUrl: String,
) : ConfirmationHandler.Option
