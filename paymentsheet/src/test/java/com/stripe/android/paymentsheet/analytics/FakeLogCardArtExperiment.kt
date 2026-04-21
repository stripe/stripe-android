package com.stripe.android.paymentsheet.analytics

import com.stripe.android.common.analytics.experiment.LogCardArtExperiment
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession

internal class FakeLogCardArtExperiment : LogCardArtExperiment {

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean {
        // No-op for tests
        return false
    }
}
