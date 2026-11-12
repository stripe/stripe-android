package com.stripe.android.paymentsheet.featuredinstitutions

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession

internal class FakeFeaturedInstitutionsHelper(
    override val featuredInstitutions: FeaturedInstitutions? = null,
) : FeaturedInstitutionsHelper {
    private val _fetchCalls = Turbine<PaymentMethodMetadata>()
    val fetchCalls: ReceiveTurbine<PaymentMethodMetadata> = _fetchCalls

    override fun fetchAsync(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
        apiConfiguration: ApiConfiguration.State,
    ) {
        _fetchCalls.add(paymentMethodMetadata)
    }
}
