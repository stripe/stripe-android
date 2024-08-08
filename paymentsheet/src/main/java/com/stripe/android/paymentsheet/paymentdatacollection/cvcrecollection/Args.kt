package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand

data class Args(
    val lastFour: String,
    val cardBrand: CardBrand,
    val cvc: String? = null,
    val displayMode: DisplayMode
) {
    sealed interface DisplayMode {
        val isLiveMode: Boolean

        // DisplayMode.Activity will retain TopBar and Confirm button. It will be used in CvcRecollectionActivity.
        data class Activity(override val isLiveMode: Boolean) : DisplayMode

        // DisplayMode.PaymentScreen will omit TopBar and Confirm button because PaymentScreen handles that.
        data class PaymentScreen(override val isLiveMode: Boolean) : DisplayMode
    }
}
