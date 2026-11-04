package com.stripe.android.common.analytics.experiment

import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon

internal class FakePrimaryButtonExperiment(
    var icon: PrimaryButtonIcon = PrimaryButtonIcon.Lock,
) : PrimaryButtonExperiment {
    val getIconCalls = Turbine<PaymentMethodMetadata>()

    override fun getIcon(metadata: PaymentMethodMetadata): PrimaryButtonIcon {
        getIconCalls.add(metadata)
        return icon
    }

    fun validate() {
        getIconCalls.ensureAllEventsConsumed()
    }
}
