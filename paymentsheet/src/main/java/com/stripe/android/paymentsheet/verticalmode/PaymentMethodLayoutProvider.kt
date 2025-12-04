package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.common.analytics.experiment.CommonElementsDimensions
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState

internal data class PaymentMethodLayoutProvider(
    val layout: PaymentMethodLayout,
    val customerState: CustomerState?,
    val paymentMethodMetadata: PaymentMethodMetadata?,
    val eventReporter: EventReporter,
) {
    fun getAndLogExposure(): PaymentMethodLayout {
        if (paymentMethodMetadata == null) {
            return layout
        }

        if (layout == PaymentMethodLayout.Automatic) {
            paymentMethodMetadata.experimentsData?.experimentAssignments?.get(ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_AA)
                ?.let { experimentAssignment ->
                    if (experimentAssignment == "control" || experimentAssignment == "treatment") {
                        val loggableExperiment = LoggableExperiment.OcsMobileHorizontalAA(
                            experiment = ElementsSession.ExperimentAssignment.OCS_MOBILE_HORIZONTAL_AA,
                            group = experimentAssignment,
                            experimentsData = paymentMethodMetadata.experimentsData,
                            commonElementsDimensions = CommonElementsDimensions.create(
                                paymentMethodMetadata
                            ),
                            numSavedPaymentMethods = customerState?.paymentMethods?.size,
                        )
                        // TODO: make sure this isn't blocking etc.
                        eventReporter.onExperimentExposure(loggableExperiment)
                    }
                }
        }
        return layout
    }
}