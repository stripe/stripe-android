package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedConfigurationHandler.ConfigurationCache
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfigurationHandlerTest {
    @Test
    fun validationFailureReturnsFailureResult() = runScenario {
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("").build(),
        )
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string."
        )
    }

    @Test
    fun `configuration fails when sheetIsOpen`() = runScenario {
        sheetStateHolder.sheetIsOpen = true
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = configuration,
        )
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Configuring while a sheet is open is not supported.")
    }

    @Test
    fun `result is used from saved state handle when configurations are the same and sheetIsOpen`() = runScenario {
        sheetStateHolder.sheetIsOpen = true
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        savedStateHandle[ConfigurationCache.KEY] = ConfigurationCache(
            arguments = DefaultEmbeddedConfigurationHandler.Arguments(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
                ),
                configuration = configuration.asCommonConfiguration(),
            ),
            resultState = loader.createSuccess(configuration.asCommonConfiguration()).getOrThrow(),
        )
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = configuration,
        )
        assertThat(result.getOrThrow())
            .isInstanceOf<PaymentElementLoader.State>()
    }

    @Test
    fun paymentElementLoaderIsCalledWithCorrectArguments() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        loader.emit(loader.createSuccess(configuration.asCommonConfiguration()))
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = configuration,
        )
        assertThat(result.getOrThrow())
            .isInstanceOf<PaymentElementLoader.State>()
    }

    @Test
    fun `configure reuses result, only calls the loader once, if configuration is the same`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        loader.emit(loader.createSuccess(configuration.asCommonConfiguration()))

        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )

        val state1 = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration,
        ).getOrThrow()
        val state2 = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration,
        ).getOrThrow()
        assertThat(state1).isEqualTo(state2)
    }

    @Test
    fun `configure calls loader twice when using different configurations`() = runScenario {
        val configuration1 = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        loader.emit(loader.createSuccess(configuration1.asCommonConfiguration()))
        val configuration2 = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .allowsDelayedPaymentMethods(true)
            .build()
        loader.emit(loader.createSuccess(configuration2.asCommonConfiguration()))

        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )

        val state1 = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration1,
        ).getOrThrow()
        val state2 = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration2,
        ).getOrThrow()
        assertThat(state1).isNotEqualTo(state2)
    }

    @Test
    fun `configure calls loader twice when using different intent configurations`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        loader.emit(
            loader.createSuccess(
                configuration = configuration.asCommonConfiguration(),
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
            )
        )
        loader.emit(
            loader.createSuccess(
                configuration = configuration.asCommonConfiguration(),
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(currency = "eur")
            )
        )

        val state1 = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = configuration,
        ).getOrThrow()
        val state2 = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "EUR"),
            ),
            configuration = configuration,
        ).getOrThrow()
        assertThat(state1).isNotEqualTo(state2)
    }

    @Test
    fun `result is used from saved state handle when configurations are the same`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        savedStateHandle[ConfigurationCache.KEY] = ConfigurationCache(
            arguments = DefaultEmbeddedConfigurationHandler.Arguments(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
                ),
                configuration = configuration.asCommonConfiguration(),
            ),
            resultState = loader.createSuccess(configuration.asCommonConfiguration()).getOrThrow(),
        )
        val result = handler.configure(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
            ),
            configuration = configuration,
        )
        assertThat(result.getOrThrow())
            .isInstanceOf<PaymentElementLoader.State>()
    }

    @Test
    fun `results are saved in saved state handle`() = runScenario {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        val loaderResult = loader.createSuccess(configuration.asCommonConfiguration())
        loader.emit(loaderResult)

        val result = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration,
        )
        val configurationCache = savedStateHandle.get<ConfigurationCache>(ConfigurationCache.KEY)
        assertThat(result.getOrThrow()).isEqualTo(configurationCache!!.resultState)
        assertThat(configurationCache).isEqualTo(
            ConfigurationCache(
                DefaultEmbeddedConfigurationHandler.Arguments(
                    intentConfiguration = intentConfiguration,
                    configuration = configuration.asCommonConfiguration(),
                ),
                resultState = loaderResult.getOrThrow(),
            )
        )
    }

    @Test
    fun `results are not saved in saved state handle on failure`() = runScenario {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        loader.emit(Result.failure(IllegalStateException("Bad data")))

        val result = handler.configure(
            intentConfiguration = intentConfiguration,
            configuration = configuration,
        )
        assertThat(result.isFailure).isTrue()
        val configurationCache = savedStateHandle.get<ConfigurationCache>(ConfigurationCache.KEY)
        assertThat(configurationCache).isNull()
    }

    @Test
    fun `parallel calls to configure with the same arguments results in a single call to the loader`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )
        val countDownLatch = CountDownLatch(2)
        val testDispatcher = UnconfinedTestDispatcher()

        val first = testScope.async(testDispatcher) {
            countDownLatch.countDown()
            handler.configure(
                intentConfiguration = intentConfiguration,
                configuration = configuration,
            ).getOrThrow()
        }

        val second = testScope.async(testDispatcher) {
            countDownLatch.countDown()
            handler.configure(
                intentConfiguration = intentConfiguration,
                configuration = configuration,
            ).getOrThrow()
        }

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()

        loader.emit(loader.createSuccess(configuration.asCommonConfiguration()))

        listOf(first, second).awaitAll()

        assertThat(first.await()).isEqualTo(second.await())
    }

    @Test
    fun `parallel calls to configure with different arguments results in different results`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()

        val first = testScope.backgroundScope.async(Dispatchers.IO) {
            handler.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD"),
                ),
                configuration = configuration,
            ).getOrThrow()
        }

        loader.loadCalledTurbine.awaitItem()

        val second = testScope.backgroundScope.async(Dispatchers.IO) {
            handler.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
                ),
                configuration = configuration,
            ).getOrThrow()
        }

        loader.loadCalledTurbine.awaitItem()
        val expectedResult = loader.createSuccess(
            configuration.asCommonConfiguration(),
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
        loader.emit(expectedResult)

        assertThat(first.isCompleted).isFalse()
        assertThat(second.await()).isEqualTo(expectedResult.getOrThrow())
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) {
        runTest {
            val loader = FakePaymentElementLoader()
            val savedStateHandle = SavedStateHandle()
            val sheetStateHolder = SheetStateHolder(savedStateHandle)
            val eventReporter = FakeEventReporter()
            val handler = DefaultEmbeddedConfigurationHandler(loader, savedStateHandle, sheetStateHolder, eventReporter)
            Scenario(
                loader = loader,
                savedStateHandle = savedStateHandle,
                handler = handler,
                testScope = this,
                sheetStateHolder = sheetStateHolder,
            ).apply {
                block()
            }
            loader.assertConsumed()
        }
    }

    private class Scenario(
        val loader: FakePaymentElementLoader,
        val savedStateHandle: SavedStateHandle,
        val handler: DefaultEmbeddedConfigurationHandler,
        val testScope: TestScope,
        val sheetStateHolder: SheetStateHolder,
    )

    private class FakePaymentElementLoader : PaymentElementLoader {
        private val resultTurbine: Turbine<Result<PaymentElementLoader.State>> = Turbine()
        val loadCalledTurbine: Turbine<PaymentElementLoader.InitializationMode> = Turbine()

        fun emit(result: Result<PaymentElementLoader.State>) {
            resultTurbine.add(result)
        }

        fun assertConsumed() {
            resultTurbine.ensureAllEventsConsumed()
        }

        fun createSuccess(
            configuration: CommonConfiguration,
            stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        ): Result<PaymentElementLoader.State> {
            return Result.success(
                PaymentElementLoader.State(
                    config = configuration,
                    customer = null,
                    paymentSelection = null,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = stripeIntent,
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
        }

        override suspend fun load(
            initializationMode: PaymentElementLoader.InitializationMode,
            configuration: CommonConfiguration,
            isReloadingAfterProcessDeath: Boolean,
            initializedViaCompose: Boolean,
        ): Result<PaymentElementLoader.State> {
            loadCalledTurbine.add(initializationMode)
            return resultTurbine.awaitItem()
        }
    }
}
