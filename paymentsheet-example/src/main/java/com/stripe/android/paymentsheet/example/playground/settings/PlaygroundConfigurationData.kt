package com.stripe.android.paymentsheet.example.playground.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class PlaygroundConfigurationData(
    val integrationType: IntegrationType = IntegrationType.PaymentSheet,
) : Parcelable {
    @Serializable
    enum class IntegrationType {
        @SerialName("paymentSheet")
        PaymentSheet,

        @SerialName("flowController")
        FlowController,

        @SerialName("Embedded")
        Embedded,

        @SerialName("CustomerSheet")
        CustomerSheet;

        fun isPaymentFlow(): Boolean {
            return this in setOf(PaymentSheet, FlowController, Embedded)
        }

        fun isCustomerFlow(): Boolean {
            return this == CustomerSheet
        }
    }
}
