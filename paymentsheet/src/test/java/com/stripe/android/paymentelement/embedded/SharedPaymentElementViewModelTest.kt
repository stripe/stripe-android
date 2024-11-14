package com.stripe.android.paymentelement.embedded

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class SharedPaymentElementViewModelTest {

    @Test
    fun `configure maps success result`() = runTest {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        val handler = FakeEmbeddedConfigurationHandler()
        val viewModel = SharedPaymentElementViewModel(handler)
        handler.emit(
            Result.success(
                PaymentElementLoader.State(
                    config = configuration.asCommonConfiguration(),
                    customer = null,
                    linkState = null,
                    paymentSelection = null,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                        allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                        allowsPaymentMethodsRequiringShippingAddress = configuration
                            .allowsPaymentMethodsRequiringShippingAddress,
                        isGooglePayReady = false,
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                    ),
                )
            )
        )
        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),

                ),
                configuration = configuration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
    }

    @Test
    fun `configure maps failure result`() = runTest {
        val exception = IllegalStateException("Hi")
        val handler = FakeEmbeddedConfigurationHandler()
        val viewModel = SharedPaymentElementViewModel(handler)
        handler.emit(Result.failure(exception))
        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),

                ),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
            )
        ).isEqualTo(EmbeddedPaymentElement.ConfigureResult.Failed(exception))
    }

    private class FakeEmbeddedConfigurationHandler : EmbeddedConfigurationHandler {
        private val turbine: Turbine<Result<PaymentElementLoader.State>> = Turbine()

        fun emit(result: Result<PaymentElementLoader.State>) {
            turbine.add(result)
        }

        override suspend fun configure(
            intentConfiguration: PaymentSheet.IntentConfiguration,
            configuration: EmbeddedPaymentElement.Configuration
        ): Result<PaymentElementLoader.State> {
            return turbine.awaitItem()
        }
    }
}
