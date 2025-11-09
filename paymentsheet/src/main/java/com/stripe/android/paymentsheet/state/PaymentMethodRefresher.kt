package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.PaymentMethod
import javax.inject.Inject

internal interface PaymentMethodRefresher {
    suspend fun refresh(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        metadata: PaymentElementLoader.Metadata
    ): Result<List<PaymentMethod>>
}

internal class DefaultPaymentMethodRefresher @Inject constructor(
    private val loader: PaymentElementLoader
) : PaymentMethodRefresher {
    override suspend fun refresh(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        metadata: PaymentElementLoader.Metadata,
    ) = loader.load(initializationMode, configuration, metadata).map { state ->
        state.customer?.paymentMethods ?: emptyList()
    }
}
