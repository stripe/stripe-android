package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ElementsSession

internal class FakeDefaultPaymentElementLoaderAnalyticsMetadataFactory(
    private val createResultFactory: () -> AnalyticsMetadata,
) : DefaultPaymentElementLoader.AnalyticsMetadataFactory {
    private val _createCall = Turbine<CreateCall>()
    val createCall: ReceiveTurbine<CreateCall> = _createCall

    fun validate() {
        _createCall.ensureAllEventsConsumed()
    }

    override fun create(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationMetadata: IntegrationMetadata,
        elementsSession: ElementsSession,
        isGooglePaySupported: Boolean,
        configuration: PaymentElementLoader.Configuration,
        customerMetadata: CustomerMetadata?,
        linkStateResult: LinkStateResult?
    ): AnalyticsMetadata {
        _createCall.add(
            CreateCall(
                initializationMode = initializationMode,
                integrationMetadata = integrationMetadata,
                elementsSession = elementsSession,
                isGooglePaySupported = isGooglePaySupported,
                configuration = configuration,
                customerMetadata = customerMetadata,
                linkStateResult = linkStateResult,
            )
        )
        return createResultFactory()
    }

    class CreateCall(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val integrationMetadata: IntegrationMetadata,
        val elementsSession: ElementsSession,
        val isGooglePaySupported: Boolean,
        val configuration: PaymentElementLoader.Configuration,
        val customerMetadata: CustomerMetadata?,
        val linkStateResult: LinkStateResult?
    )
}
