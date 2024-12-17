package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
@RunWith(RobolectricTestRunner::class)
internal class SharedPaymentElementViewModelTest {

    @Test
    fun `configure sets initial confirmationState`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        configurationHandler.emit(
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

        assertThat(viewModel.confirmationState).isNull()

        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = configuration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

        assertThat(viewModel.confirmationState?.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `Updating selection updates confirmationState`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        configurationHandler.emit(
            Result.success(
                PaymentElementLoader.State(
                    config = configuration.asCommonConfiguration(),
                    customer = null,
                    linkState = null,
                    paymentSelection = PaymentSelection.GooglePay,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                        allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                        allowsPaymentMethodsRequiringShippingAddress = configuration
                            .allowsPaymentMethodsRequiringShippingAddress,
                        isGooglePayReady = true,
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                    ),
                )
            )
        )

        assertThat(viewModel.confirmationState?.selection?.paymentMethodType).isNull()

        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = configuration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

        assertThat(viewModel.confirmationState?.selection?.paymentMethodType).isEqualTo("google_pay")
        selectionHolder.set(null)
        assertThat(viewModel.confirmationState?.selection?.paymentMethodType).isNull()
    }

    @Test
    fun `configure maps success result`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        configurationHandler.emit(
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

        viewModel.paymentOption.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `configure emits payment option and sets initial selection`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        configurationHandler.emit(
            Result.success(
                PaymentElementLoader.State(
                    config = configuration.asCommonConfiguration(),
                    customer = null,
                    linkState = null,
                    paymentSelection = PaymentSelection.GooglePay,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                        allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                        allowsPaymentMethodsRequiringShippingAddress = configuration
                            .allowsPaymentMethodsRequiringShippingAddress,
                        isGooglePayReady = true,
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                    ),
                )
            )
        )

        assertThat(selectionHolder.selection.value?.paymentMethodType).isNull()
        viewModel.paymentOption.test {
            assertThat(awaitItem()).isNull()

            assertThat(
                viewModel.configure(
                    PaymentSheet.IntentConfiguration(
                        PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                    ),
                    configuration = configuration,
                )
            ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
        }
        assertThat(selectionHolder.selection.value?.paymentMethodType).isEqualTo("google_pay")
    }

    @Test
    fun `configure maps failure result`() = testScenario {
        val exception = IllegalStateException("Hi")
        configurationHandler.emit(Result.failure(exception))
        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
            )
        ).isEqualTo(EmbeddedPaymentElement.ConfigureResult.Failed(exception))
    }

    @Test
    fun `clearPaymentOption clears selection`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        configurationHandler.emit(
            Result.success(
                PaymentElementLoader.State(
                    config = configuration.asCommonConfiguration(),
                    customer = null,
                    linkState = null,
                    paymentSelection = PaymentSelection.GooglePay,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                        billingDetailsCollectionConfiguration = configuration
                            .billingDetailsCollectionConfiguration,
                        allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                        allowsPaymentMethodsRequiringShippingAddress = configuration
                            .allowsPaymentMethodsRequiringShippingAddress,
                        isGooglePayReady = true,
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                    ),
                )
            )
        )

        assertThat(selectionHolder.selection.value?.paymentMethodType).isNull()
        viewModel.paymentOption.test {
            assertThat(awaitItem()).isNull()

            assertThat(
                viewModel.configure(
                    PaymentSheet.IntentConfiguration(
                        PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                    ),
                    configuration = configuration,
                )
            ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
            assertThat(selectionHolder.selection.value?.paymentMethodType).isEqualTo("google_pay")

            viewModel.clearPaymentOption()
            assertThat(selectionHolder.selection.value?.paymentMethodType).isNull()
            assertThat(awaitItem()).isNull()
        }
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) {
        val confirmationHandler = FakeConfirmationHandler()
        val configurationHandler = FakeEmbeddedConfigurationHandler()
        val paymentOptionDisplayDataFactory = PaymentOptionDisplayDataFactory(
            iconLoader = mock(),
            context = ApplicationProvider.getApplicationContext(),
        )
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())

        runTest {
            val viewModel = SharedPaymentElementViewModel(
                confirmationHandlerFactory = { confirmationHandler },
                ioContext = testScheduler,
                configurationHandler = configurationHandler,
                paymentOptionDisplayDataFactory = paymentOptionDisplayDataFactory,
                selectionHolder = selectionHolder,
            )

            Scenario(
                configurationHandler = configurationHandler,
                viewModel = viewModel,
                selectionHolder = selectionHolder,
            ).block()
        }

        configurationHandler.turbine.ensureAllEventsConsumed()
        confirmationHandler.validate()
    }

    private class Scenario(
        val configurationHandler: FakeEmbeddedConfigurationHandler,
        val viewModel: SharedPaymentElementViewModel,
        val selectionHolder: EmbeddedSelectionHolder,
    )

    private class FakeEmbeddedConfigurationHandler : EmbeddedConfigurationHandler {
        val turbine: Turbine<Result<PaymentElementLoader.State>> = Turbine()

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
