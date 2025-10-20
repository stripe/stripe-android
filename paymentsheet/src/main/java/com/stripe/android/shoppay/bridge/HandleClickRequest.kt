package com.stripe.android.shoppay.bridge

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class HandleClickRequest(
    val eventData: EventData,
) : StripeModel {
    @Parcelize
    data class EventData(
        val expressPaymentType: String
    ) : StripeModel
}
