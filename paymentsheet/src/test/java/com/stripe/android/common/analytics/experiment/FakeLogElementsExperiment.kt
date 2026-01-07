package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession

internal class FakeLogElementsExperiment: LogElementsExperiment {
    override fun logHorizontalModeAndroidAAExposure(
        experimentsData: ElementsSession.ExperimentsData,
        variant: String,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasSavedPaymentMethod: Boolean
    ) {
        // Do nothing.
    }
}
