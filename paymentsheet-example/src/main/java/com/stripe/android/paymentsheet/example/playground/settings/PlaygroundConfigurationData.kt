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
        CustomerSheet,

        @SerialName("FlowControllerWithSpt")
        FlowControllerWithSpt,

        @SerialName("LinkController")
        LinkController;

        @SerialName("ridesharingApp")
        RidesharingApp,


        fun isPaymentFlow(): Boolean {
            return this in paymentFlows()
        }

        fun isSptFlow(): Boolean {
            return this in sptFlows()
        }

        fun isCustomerFlow(): Boolean {
            return this == CustomerSheet
        }

        companion object {
            fun paymentFlows(): Set<IntegrationType> {
                return setOf(PaymentSheet, FlowController, Embedded, RidesharingApp, LinkController)
            }

            fun sptFlows(): Set<IntegrationType> {
                return setOf(FlowControllerWithSpt)
            }
        }
    }
}
