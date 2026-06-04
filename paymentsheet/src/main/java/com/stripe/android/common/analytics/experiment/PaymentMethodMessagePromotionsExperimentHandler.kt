package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface PaymentMethodMessagePromotionsExperimentHandler {

    fun logExposure(metadata: PaymentMethodMetadata)
}

internal class DefaultPaymentMethodMessagePromotionsExperimentHandler @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode
) : PaymentMethodMessagePromotionsExperimentHandler {

    override fun logExposure(metadata: PaymentMethodMetadata) {
        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
        ] ?: return

        val exposure = LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions(
            experimentsData = metadata.experimentsData,
            group = variant,
            metadata = metadata,
            mode = mode,
            layout = metadata.paymentMethodOrientation().toLayout(),
        )

        println("PMM Experiment exposure: $exposure")

        eventReporter.onExperimentExposure(exposure)
    }

    private fun PaymentMethodOrientation.toLayout(): String = when (this) {
        PaymentMethodOrientation.Vertical -> "vertical"
        PaymentMethodOrientation.Horizontal -> "horizontal"
    }
}
