package com.stripe.android.paymentsheet.example.playground.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaygroundConfigurationData(
    val integrationType: IntegrationType = IntegrationType.PaymentSheet,
) {
    @Serializable
    enum class IntegrationType {
        @SerialName("paymentSheet")
        PaymentSheet,

        @SerialName("flowController")
        FlowController,

        @SerialName("CustomerSheet")
        CustomerSheet;

        fun isPaymentFlow(): Boolean {
            return this == PaymentSheet || this == FlowController
        }
    }
}
