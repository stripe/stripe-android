package com.stripe.android.paymentsheet.verticalmode

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.InternalState
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutSessionCurrencyUpdaterTest {

    private val applicationContext: Application
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `returns failure when metadata is not checkout session`() = runScenario {
        val metadata = PaymentMethodMetadataFactory.create()

        val result = updater.updateCurrency(
            paymentMethodMetadata = metadata,
            currencyCode = "eur",
            integrationConfiguration = DEFAULT_INTEGRATION_CONFIGURATION,
            initializedViaCompose = false,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Not a checkout session")
    }

    @Test
    fun `calls updateCurrency and reloads on success`() = runScenario {
        val instancesKey = setupCheckoutInstances()
        val metadata = createCheckoutMetadata(instancesKey)

        val result = updater.updateCurrency(
            paymentMethodMetadata = metadata,
            currencyCode = "eur",
            integrationConfiguration = DEFAULT_INTEGRATION_CONFIGURATION,
            initializedViaCompose = false,
        )

        assertThat(result.isSuccess).isTrue()

        val loadCall = fakeLoader.loadCalls.awaitItem()
        val initMode = loadCall.initializationMode as PaymentElementLoader.InitializationMode.CheckoutSession
        assertThat(initMode.instancesKey).isEqualTo(instancesKey)
        assertThat(initMode.checkoutSessionResponse).isEqualTo(UPDATED_RESPONSE)
    }

    @Test
    fun `returns failure when updateCurrency fails`() = runScenario(
        updateCurrencyResult = Result.failure(RuntimeException("Network error")),
    ) {
        val instancesKey = setupCheckoutInstances()
        val metadata = createCheckoutMetadata(instancesKey)

        val result = updater.updateCurrency(
            paymentMethodMetadata = metadata,
            currencyCode = "eur",
            integrationConfiguration = DEFAULT_INTEGRATION_CONFIGURATION,
            initializedViaCompose = false,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error")
    }

    @Test
    fun `returns failure when reload fails`() = runScenario(
        loadResult = Result.failure(RuntimeException("Load error")),
    ) {
        val instancesKey = setupCheckoutInstances()
        val metadata = createCheckoutMetadata(instancesKey)

        val result = updater.updateCurrency(
            paymentMethodMetadata = metadata,
            currencyCode = "eur",
            integrationConfiguration = DEFAULT_INTEGRATION_CONFIGURATION,
            initializedViaCompose = false,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Load error")
        fakeLoader.loadCalls.awaitItem()
    }

    @Test
    fun `syncs CheckoutInstances on successful currency update`() = runScenario {
        val instancesKey = setupCheckoutInstances()
        val metadata = createCheckoutMetadata(instancesKey)

        updater.updateCurrency(
            paymentMethodMetadata = metadata,
            currencyCode = "eur",
            integrationConfiguration = DEFAULT_INTEGRATION_CONFIGURATION,
            initializedViaCompose = false,
        )

        fakeLoader.loadCalls.awaitItem()

        val checkout = CheckoutInstances[instancesKey].first()
        assertThat(checkout.internalState.checkoutSessionResponse).isEqualTo(UPDATED_RESPONSE)
    }

    private fun setupCheckoutInstances(): String {
        val instancesKey = "test_key"
        val internalState = InternalState(
            key = instancesKey,
            checkoutSessionResponse = DEFAULT_RESPONSE,
        )
        val checkout = Checkout.createWithState(applicationContext, Checkout.State(internalState))
        CheckoutInstances.add(instancesKey, checkout)
        return instancesKey
    }

    private fun createCheckoutMetadata(instancesKey: String) = PaymentMethodMetadataFactory.create(
        integrationMetadata = IntegrationMetadata.CheckoutSession(
            id = "cs_123",
            instancesKey = instancesKey,
        ),
    )

    private fun runScenario(
        updateCurrencyResult: Result<CheckoutSessionResponse> = Result.success(UPDATED_RESPONSE),
        loadResult: Result<PaymentElementLoader.State>? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val mockRepository: CheckoutSessionRepository = mock()
        whenever(mockRepository.updateCurrency(any(), any())).thenReturn(updateCurrencyResult)

        val fakeLoader = FakePaymentElementLoaderForCurrencyUpdater(
            loadResult = loadResult ?: Result.success(DEFAULT_LOAD_STATE),
        )

        val updater = CheckoutSessionCurrencyUpdater(
            checkoutSessionRepository = mockRepository,
            paymentElementLoader = fakeLoader,
            workContext = testDispatcher,
        )

        block(
            Scenario(
                updater = updater,
                fakeLoader = fakeLoader,
            )
        )

        fakeLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val updater: CheckoutSessionCurrencyUpdater,
        val fakeLoader: FakePaymentElementLoaderForCurrencyUpdater,
    )

    private class FakePaymentElementLoaderForCurrencyUpdater(
        private val loadResult: Result<PaymentElementLoader.State>,
    ) : PaymentElementLoader {
        val loadCalls = Turbine<LoadCall>()

        override suspend fun load(
            initializationMode: PaymentElementLoader.InitializationMode,
            integrationConfiguration: PaymentElementLoader.Configuration,
            metadata: PaymentElementLoader.Metadata,
        ): Result<PaymentElementLoader.State> {
            loadCalls.add(LoadCall(initializationMode, integrationConfiguration, metadata))
            return loadResult
        }

        fun ensureAllEventsConsumed() {
            loadCalls.ensureAllEventsConsumed()
        }

        data class LoadCall(
            val initializationMode: PaymentElementLoader.InitializationMode,
            val integrationConfiguration: PaymentElementLoader.Configuration,
            val metadata: PaymentElementLoader.Metadata,
        )
    }

    companion object {
        private val DEFAULT_INTEGRATION_CONFIGURATION = PaymentElementLoader.Configuration.PaymentSheet(
            configuration = PaymentSheet.Configuration.Builder("Example, Inc.").build(),
        )

        private val DEFAULT_RESPONSE = CheckoutSessionResponse(
            id = "cs_123",
            amount = 1000L,
            currency = "usd",
            mode = CheckoutSessionResponse.Mode.PAYMENT,
            customerEmail = null,
            elementsSession = null,
            paymentIntent = null,
            setupIntent = null,
            customer = null,
            savedPaymentMethodsOfferSave = null,
            totalSummary = null,
            lineItems = emptyList(),
            shippingOptions = emptyList(),
            adaptivePricingInfo = null,
        )

        private val UPDATED_RESPONSE = DEFAULT_RESPONSE.copy(currency = "eur")

        private val DEFAULT_LOAD_STATE = PaymentElementLoader.State(
            config = PaymentSheet.Configuration.Builder("Example, Inc.").build().asCommonConfiguration(),
            customer = null,
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )
    }
}
