package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.utils.FakePaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutStateLoaderTest {

    @Test
    fun `load commits confirmation state with payment method metadata`() = runScenario {
        loader.load(checkoutControllerState())

        assertThat(confirmationStateHolder.state?.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `load uses the provided merchant display name`() = runScenario(
        merchantDisplayName = "Acme Corp",
    ) {
        loader.load(checkoutControllerState())

        assertThat(confirmationStateHolder.state?.configuration?.merchantDisplayName).isEqualTo("Acme Corp")
    }

    @Test
    fun `load sources the billing email from the checkout session customer email`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(customerEmail = "checkout@example.com")

        loader.load(checkoutControllerState(checkoutSessionResponse = response))

        assertThat(confirmationStateHolder.state?.configuration?.defaultBillingDetails?.email)
            .isEqualTo("checkout@example.com")
    }

    @Test
    fun `load propagates embeddedViewDisplaysMandateText from the payment element configuration`() =
        runScenario {
            val configuration = CheckoutController.Configuration()
                .paymentElement(PaymentElement.Configuration().embeddedViewDisplaysMandateText(false))
                .build()

            loader.load(checkoutControllerState(configuration = configuration))

            assertThat(confirmationStateHolder.state?.configuration?.embeddedViewDisplaysMandateText)
                .isFalse()
    }

    @Test
    fun `load resolves selection locally with chooser and commits it`() = runScenario(
        loaderSelection = PaymentSelection.GooglePay,
        selectionChooser = EmbeddedSelectionChooser { _, _, previousSelection, _, _, _ ->
            previousSelection
        },
    ) {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        loader.load(checkoutControllerState())

        assertThat(confirmationStateHolder.state?.selection)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(selectionHolder.selection.value)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(paymentElementLoader.lastReconfigureContext).isNull()
    }

    @Test
    fun `load re-resolves against the current selection on a reload`() {
        val previousSelections = mutableListOf<PaymentSelection?>()
        runScenario(
            loaderSelection = PaymentSelection.GooglePay,
            selectionChooser = EmbeddedSelectionChooser { _, _, previousSelection, newSelection, _, _ ->
                previousSelections += previousSelection
                previousSelection ?: newSelection
            },
        ) {
            loader.load(checkoutControllerState())

            selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

            loader.load(requireNotNull(stateHolder.state))

            assertThat(previousSelections)
                .containsExactly(null, PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
                .inOrder()
            assertThat(confirmationStateHolder.state?.selection)
                .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            assertThat(paymentElementLoader.lastReconfigureContext).isNull()
        }
    }

    @Test
    fun `load commits the loader selection when the chooser returns it`() = runScenario(
        loaderSelection = PaymentSelection.GooglePay,
    ) {
        loader.load(checkoutControllerState())

        assertThat(confirmationStateHolder.state?.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `load commits state that exposes the checkout session`() = runScenario {
        loader.load(checkoutControllerState())

        assertThat(stateHolder.checkoutSession.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(stateHolder.state?.key).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        // No adaptive pricing in the response, so no flag images are resolved.
        assertThat(stateHolder.state?.flagImages).isNull()
    }

    @Test
    fun `load throws and commits nothing when the payment element loader fails`() = runScenario(
        shouldFail = true,
    ) {
        assertFailsWith<IllegalStateException> {
            loader.load(checkoutControllerState())
        }

        assertThat(stateHolder.state).isNull()
        assertThat(confirmationStateHolder.state).isNull()
    }

    @Test
    fun `load reuses cached flag images when the currencies are unchanged`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(adaptivePricingInfo = adaptivePricingInfo())

        loader.load(checkoutControllerState(checkoutSessionResponse = response))

        // Both currency flags are downloaded on the initial load.
        imageLoader.awaitLoadCall()
        imageLoader.awaitLoadCall()

        // A mutation reloads with the previously resolved images carried forward (a copy of the
        // committed state) and the same currencies, so the cache is reused and nothing re-downloads.
        val reloaded = requireNotNull(stateHolder.state).copy(checkoutSessionResponse = response)
        loader.load(reloaded)

        imageLoader.ensureAllEventsConsumed()
    }

    private fun checkoutControllerState(
        configuration: CheckoutController.Configuration.State = CheckoutController.Configuration().build(),
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
    ) = CheckoutControllerState.defaultState(
        configuration = configuration,
        checkoutSessionResponse = checkoutSessionResponse,
    )

    // Adaptive pricing (usd → eur) drives flag image resolution during load.
    private fun adaptivePricingInfo() = CheckoutSessionResponse.AdaptivePricingInfo(
        activePresentmentCurrency = "eur",
        integrationAmount = 5099,
        integrationCurrency = "usd",
        localCurrencyOptions = listOf(
            CheckoutSessionResponse.LocalCurrencyOption(
                amount = 4594,
                conversionMarkupBps = 400,
                currency = "eur",
                presentmentExchangeRate = "0.900961",
            ),
        ),
    )

    private fun runScenario(
        merchantDisplayName: String = "Example, Inc.",
        loaderSelection: PaymentSelection? = null,
        shouldFail: Boolean = false,
        isGooglePayAvailable: Boolean = false,
        selectionChooser: EmbeddedSelectionChooser = EmbeddedSelectionChooser { _, _, _, newSelection, _, _ ->
            newSelection
        },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val imageLoader = FakeStripeImageLoader(
            loadResult = Result.success(Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)),
        )
        val flagImageResolver = FlagImageResolver(
            flagImageRepository = FlagImageRepository(imageLoader = imageLoader, displayDensity = 3f),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = application,
                publishableKey = "pk_test_123",
            ),
        )
        // All holders share one SavedStateHandle, mirroring the singleton graph in production.
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val confirmationStateHolder = CheckoutConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        val stateHolder = CheckoutControllerStateHolder(savedStateHandle)
        val paymentElementLoader = FakePaymentElementLoader(
            paymentSelection = loaderSelection,
            shouldFail = shouldFail,
            isGooglePayAvailable = isGooglePayAvailable,
        )
        val loader = CheckoutStateLoader(
            merchantDisplayName = merchantDisplayName,
            flagImageResolver = flagImageResolver,
            paymentElementLoader = paymentElementLoader,
            selectionChooser = selectionChooser,
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
            stateHolder = stateHolder,
        )

        Scenario(
            loader = loader,
            stateHolder = stateHolder,
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
            paymentElementLoader = paymentElementLoader,
            imageLoader = imageLoader,
        ).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val loader: CheckoutStateLoader,
        val stateHolder: CheckoutControllerStateHolder,
        val confirmationStateHolder: CheckoutConfirmationStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val paymentElementLoader: FakePaymentElementLoader,
        val imageLoader: FakeStripeImageLoader,
    )
}
