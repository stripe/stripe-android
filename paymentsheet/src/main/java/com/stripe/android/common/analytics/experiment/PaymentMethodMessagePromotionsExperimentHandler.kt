package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface PaymentMethodMessagePromotionsExperimentHandler {

    /**
     * Logs exposure with full dimensions. Should be called when a promotion is displayed to the user i.e.
     * when PaymentMethodMessagePromotionsHelper.getPromotionIfAvailableForCode is called.
     */
    fun logExposure(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        promotion: PaymentMethodMessagePromotion?
    )
}

internal class DefaultPaymentMethodMessagePromotionsExperimentHandler @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode
) : PaymentMethodMessagePromotionsExperimentHandler {
    private val loggedExposures = mutableSetOf<LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions>()

    override fun logExposure(
        code: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        promotion: PaymentMethodMessagePromotion?
    ) {
        val variant = metadata.experimentsData?.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS
        ] ?: return

        val promotionDisplayedSuccessfully = if (variant == "treatment") {
            promotion != null
        } else {
            null
        }

        if (promotionDisplayedSuccessfully == false) {
            eventReporter.onPaymentMethodMessagePromotionsIncomplete()
        }

        val exposure = LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions(
            experimentsData = metadata.experimentsData,
            group = variant,
            metadata = metadata,
            mode = mode,
            selectedPaymentMethodType = code,
            promotionDisplayedSuccessfully = promotionDisplayedSuccessfully,
            layout = metadata.paymentMethodOrientation().toLayout()
        )

        if (!loggedExposures.contains(exposure)) {
            loggedExposures.add(exposure)
            eventReporter.onExperimentExposure(exposure)
        }
    }

    private fun PaymentMethodOrientation.toLayout(): String = when (this) {
        PaymentMethodOrientation.Vertical -> "vertical"
        PaymentMethodOrientation.Horizontal -> "horizontal"
    }
}
