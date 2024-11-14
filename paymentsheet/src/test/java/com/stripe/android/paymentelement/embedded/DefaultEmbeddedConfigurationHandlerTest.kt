package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedConfigurationHandler.ConfigurationCache
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.test.runTest
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
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(currency = "USD"),
        )
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        savedStateHandle[ConfigurationCache.KEY] = ConfigurationCache(
            intentConfiguration = intentConfiguration,
            configuration = configuration.asCommonConfiguration(),
            resultState = loader.createSuccess(configuration.asCommonConfiguration()).getOrThrow(),
        )
        val result = handler.configure(
            intentConfiguration = intentConfiguration,
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
                intentConfiguration = intentConfiguration,
                configuration = configuration.asCommonConfiguration(),
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

    private fun runScenario(block: suspend Scenario.() -> Unit) {
        runTest {
            val loader = FakePaymentElementLoader()
            val savedStateHandle = SavedStateHandle()
            val handler = DefaultEmbeddedConfigurationHandler(loader, savedStateHandle)
            Scenario(
                loader = loader,
                savedStateHandle = savedStateHandle,
                handler = handler,
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
    )

    private class FakePaymentElementLoader : PaymentElementLoader {
        private val turbine: Turbine<Result<PaymentElementLoader.State>> = Turbine()

        fun emit(result: Result<PaymentElementLoader.State>) {
            turbine.add(result)
        }

        fun assertConsumed() {
            turbine.ensureAllEventsConsumed()
        }

        fun createSuccess(
            configuration: CommonConfiguration,
            stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        ): Result<PaymentElementLoader.State> {
            return Result.success(
                PaymentElementLoader.State(
                    config = configuration,
                    customer = null,
                    linkState = null,
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
            return turbine.awaitItem()
        }
    }
}
