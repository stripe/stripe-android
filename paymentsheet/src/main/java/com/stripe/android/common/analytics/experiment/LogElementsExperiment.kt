package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface LogElementsExperiment {
    fun logHorizontalModeAndroidAAExposure(
        experimentsData: ElementsSession.ExperimentsData,
        variant: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethod: Boolean,
    )
}

internal class DefaultLogElementsExperiment @Inject constructor(
    private val eventReporter: EventReporter,
): LogElementsExperiment {
    override fun logHorizontalModeAndroidAAExposure(
        experimentsData: ElementsSession.ExperimentsData,
        variant: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethod: Boolean,
    ) {
        val dimensions = CommonElementsDimensions.getDimensions(paymentMethodMetadata) +
            mapOf("has_saved_payment_method" to hasSavedPaymentMethod.toString())
        eventReporter.onExperimentExposure(
            LoggableExperiment.OcsMobileHorizontalModeAndroidAA(
                experimentsData = experimentsData,
                group = variant,
                dimensions = dimensions,
            )
        )
    }
}
