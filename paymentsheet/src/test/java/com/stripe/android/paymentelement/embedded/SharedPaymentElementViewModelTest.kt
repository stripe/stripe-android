package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
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
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
@RunWith(RobolectricTestRunner::class)
internal class SharedPaymentElementViewModelTest {
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
    fun `configure sets initial confirmationState`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))

        assertThat(viewModel.confirmationStateHolder.state).isNull()

        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()

        assertThat(viewModel.confirmationStateHolder.state?.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `configure clears confirmationStateHolder_state while configure is in flight`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))

        assertThat(viewModel.confirmationStateHolder.state).isNull()

        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
        assertThat(viewModel.confirmationStateHolder.state?.paymentMethodMetadata).isNotNull()

        val secondConfigureResult = testScope.async {
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        }

        testScope.testScheduler.advanceUntilIdle()
        assertThat(viewModel.confirmationStateHolder.state).isNull()
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))
        assertThat(secondConfigureResult.await()).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
        assertThat(viewModel.confirmationStateHolder.state?.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `Updating selection updates confirmationState`() = testScenario {
        configurationHandler.emit(
            Result.success(
                createPaymentElementLoaderState(
                    isGooglePayReady = true,
                    paymentSelection = PaymentSelection.GooglePay,
                )
            )
        )

        assertThat(viewModel.confirmationStateHolder.state?.selection?.paymentMethodType).isNull()

        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()

        assertThat(viewModel.confirmationStateHolder.state?.selection?.paymentMethodType).isEqualTo("google_pay")
        selectionHolder.set(null)
        assertThat(viewModel.confirmationStateHolder.state?.selection?.paymentMethodType).isNull()
    }

    @Test
    fun `configure maps success result`() = testScenario {
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))
        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = defaultConfiguration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

        viewModel.paymentOption.test {
            assertThat(awaitItem()).isNull()
        }
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `configure emits payment option and sets initial selection`() = testScenario {
        configurationHandler.emit(
            Result.success(
                createPaymentElementLoaderState(
                    isGooglePayReady = true,
                    paymentSelection = PaymentSelection.GooglePay,
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
                    configuration = defaultConfiguration,
                )
            ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
        }
        assertThat(selectionHolder.selection.value?.paymentMethodType).isEqualTo("google_pay")
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `configure correctly sets row style`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .appearance(
                PaymentSheet.Appearance(
                    embeddedAppearance = Embedded(
                        Embedded.RowStyle.FlatWithRadio.defaultLight
                    )
                )
            ).build()
        configurationHandler.emit(Result.success(createPaymentElementLoaderState()))
        assertThat(
            viewModel.configure(
                PaymentSheet.IntentConfiguration(
                    PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                ),
                configuration = configuration,
            )
        ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

        embeddedContentHelper.dataLoadedTurbine.awaitItem().let {
            assertThat(it).isNotNull()
            assertThat(it.rowStyle).isInstanceOf<Embedded.RowStyle.FlatWithRadio>()
        }
    }

    @Test
    fun `configure correctly sets customerStateHolder`() = testScenario {
        val configuration = EmbeddedPaymentElement.Configuration
            .Builder("Example, Inc.")
            .build()
        configurationHandler.emit(
            Result.success(
                createPaymentElementLoaderState(
                    isGooglePayReady = true,
                    paymentSelection = PaymentSelection.GooglePay,
                    customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                )
            )
        )
        customerStateHolder.customer.test {
            assertThat(awaitItem()).isNull()

            assertThat(
                viewModel.configure(
                    PaymentSheet.IntentConfiguration(
                        PaymentSheet.IntentConfiguration.Mode.Payment(5000, "USD"),
                    ),
                    configuration = configuration,
                )
            ).isInstanceOf<EmbeddedPaymentElement.ConfigureResult.Succeeded>()

            assertThat(awaitItem()).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        }

        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
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
                createPaymentElementLoaderState(
                    isGooglePayReady = true,
                    paymentSelection = PaymentSelection.GooglePay,
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
        assertThat(embeddedContentHelper.dataLoadedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `initEmbeddedActivityLauncher and clearEmbeddedActivityLauncher successfully init and clear sheetLauncher`() =
        testScenario {
            assertThat(embeddedContentHelper.testSheetLauncher).isNull()
            viewModel.initEmbeddedSheetLauncher(DummyActivityResultCaller.noOp(), TestLifecycleOwner())
            assertThat(embeddedContentHelper.testSheetLauncher).isNotNull()
            viewModel.clearEmbeddedSheetLauncher()
            assertThat(embeddedContentHelper.testSheetLauncher).isNull()
        }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val configurationHandler = FakeEmbeddedConfigurationHandler()
        val paymentOptionDisplayDataFactory = PaymentOptionDisplayDataFactory(
            iconLoader = mock(),
            context = ApplicationProvider.getApplicationContext(),
        )
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val confirmationStateHolder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        val embeddedContentHelper = FakeEmbeddedContentHelper()
        val customerStateHolder = CustomerStateHolder(savedStateHandle, selectionHolder.selection)

        val viewModel = SharedPaymentElementViewModel(
            confirmationStateHolderFactory = EmbeddedConfirmationStateHolderFactory {
                confirmationStateHolder
            },
            confirmationHandlerFactory = { confirmationHandler },
            ioContext = testScheduler,
            configurationHandler = configurationHandler,
            paymentOptionDisplayDataFactory = paymentOptionDisplayDataFactory,
            selectionHolder = selectionHolder,
            selectionChooser = { _, _, _, newSelection, _ ->
                newSelection
            },
            customerStateHolder = customerStateHolder,
            embeddedSheetLauncherFactory = { activityResultCaller, lifecycleOwner ->
                DefaultEmbeddedSheetLauncher(
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = lifecycleOwner,
                    selectionHolder = selectionHolder,
                    customerStateHolder = customerStateHolder,
                )
            },
            embeddedContentHelperFactory = EmbeddedContentHelperFactory {
                embeddedContentHelper
            },
        )

        Scenario(
            configurationHandler = configurationHandler,
            viewModel = viewModel,
            selectionHolder = selectionHolder,
            embeddedContentHelper = embeddedContentHelper,
            customerStateHolder = customerStateHolder,
            testScope = this,
        ).block()

        configurationHandler.turbine.ensureAllEventsConsumed()
        confirmationHandler.validate()
        embeddedContentHelper.validate()
    }

    private class Scenario(
        val configurationHandler: FakeEmbeddedConfigurationHandler,
        val viewModel: SharedPaymentElementViewModel,
        val selectionHolder: EmbeddedSelectionHolder,
        val embeddedContentHelper: FakeEmbeddedContentHelper,
        val customerStateHolder: CustomerStateHolder,
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
