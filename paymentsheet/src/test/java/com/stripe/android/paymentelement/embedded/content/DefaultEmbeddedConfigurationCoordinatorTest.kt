package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfigurationCoordinatorTest {
    private val defaultConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()

    private fun createPaymentElementLoaderState(
        isGooglePayReady: Boolean = false,
        paymentSelection: PaymentSelection? = null,
        customer: CustomerState? = null,
    ): PaymentElementLoader.State {
        return PaymentElementLoader.State(
            config = defaultConfiguration.asCommonConfiguration(),
            customer = customer,
            paymentSelection = paymentSelection,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
                billingDetailsCollectionConfiguration = defaultConfiguration
                    .billingDetailsCollectionConfiguration,
                allowsDelayedPaymentMethods = defaultConfiguration.allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = defaultConfiguration
                    .allowsPaymentMethodsRequiringShippingAddress,
                isGooglePayReady = isGooglePayReady,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            ),
        )
    }

    @Test
    fun `configure sets initial state`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))

        assertThat(confirmationStateHolder.state).isNull()

        assertThat(
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        stateHelper.stateTurbine.awaitItem().let { state ->
            assertThat(state).isNotNull()
            assertThat(state?.confirmationState?.paymentMethodMetadata).isNotNull()
            assertThat(state?.confirmationState?.configuration).isEqualTo(defaultConfiguration)
        }
    }

    @Test
    fun `configure sets state to previousSelection`() = testScenario(
        selectionChooser = {
            PaymentSelection.GooglePay
        },
    ) {
        configurationHandler.emit(
            Result.success(
                createPaymentElementLoaderState(
                    paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                )
            )
        )

        selectionHolder.set(PaymentSelection.GooglePay)
        assertThat(confirmationStateHolder.state).isNull()

        assertThat(
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        stateHelper.stateTurbine.awaitItem().let { state ->
            assertThat(state).isNotNull()
            assertThat(state?.confirmationState?.paymentMethodMetadata).isNotNull()
            assertThat(state?.confirmationState?.selection).isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `configure clears confirmationStateHolder_state while configure is in flight`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))

        assertThat(confirmationStateHolder.state).isNull()

        assertThat(
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        stateHelper.stateTurbine.awaitItem().let { state ->
            assertThat(state).isNotNull()
            assertThat(state?.confirmationState?.paymentMethodMetadata).isNotNull()
        }

        val secondConfigureResult = testScope.async {
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        }

        testScope.testScheduler.advanceUntilIdle()
        assertThat(confirmationStateHolder.state).isNull()
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))
        assertThat(secondConfigureResult.await()).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        stateHelper.stateTurbine.awaitItem().let { state ->
            assertThat(state).isNotNull()
            assertThat(state?.confirmationState?.paymentMethodMetadata).isNotNull()
        }
    }

    @Test
    fun `configure maps success result`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))
        assertThat(
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

        assertThat(stateHelper.stateTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `configure sets initial selection`() = testScenario {
        configurationHandler.emit(
            Result.success(
                createPaymentElementLoaderState(
                    isGooglePayReady = true,
                    paymentSelection = PaymentSelection.GooglePay,
                )
            )
        )

        selectionHolder.selection.test {
            assertThat(awaitItem()?.paymentMethodType).isNull()

            assertThat(
                configurationCoordinator.configure(
                    PaymentSheet.IntentConfiguration(
                        PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                    ),
                    configuration = defaultConfiguration,
                )
            ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        }
        assertThat(stateHelper.stateTurbine.awaitItem()?.confirmationState?.selection?.paymentMethodType)
            .isEqualTo("google_pay")
    }

    @Test
    fun `configure maps failure result`() = testScenario {
        val exception = IllegalStateException("Hi")
        configurationHandler.emit(Result.failure(exception))
        assertThat(
            configurationCoordinator.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
            )
        ).isEqualTo(EmbeddedPaymentElement.ConfigureResult.Failed(exception))
    }

    private fun testScenario(
        selectionChooser: (PaymentSelection?) -> PaymentSelection? = { it },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val configurationHandler = FakeEmbeddedConfigurationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(MainThreadSavedStateHandle(savedStateHandle))
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = MainThreadSavedStateHandle(savedStateHandle),
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        val stateHelper = FakeEmbeddedStateHelper()

        val configurationCoordinator = DefaultEmbeddedConfigurationCoordinator(
            confirmationStateHolder = confirmationStateHolder,
            configurationHandler = configurationHandler,
            selectionHolder = selectionHolder,
            selectionChooser = { _, _, _, newSelection, _ ->
                selectionChooser(newSelection)
            },
            stateHelper = stateHelper,
            viewModelScope = CoroutineScope(UnconfinedTestDispatcher()),
        )

        Scenario(
            configurationHandler = configurationHandler,
            configurationCoordinator = configurationCoordinator,
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
            stateHelper = stateHelper,
            testScope = this,
        ).block()

        configurationHandler.turbine.ensureAllEventsConsumed()
        confirmationHandler.validate()
        stateHelper.validate()
    }

    private class Scenario(
        val configurationHandler: FakeEmbeddedConfigurationHandler,
        val configurationCoordinator: EmbeddedConfigurationCoordinator,
        val selectionHolder: EmbeddedSelectionHolder,
        val confirmationStateHolder: EmbeddedConfirmationStateHolder,
        val stateHelper: FakeEmbeddedStateHelper,
        val testScope: TestScope,
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
