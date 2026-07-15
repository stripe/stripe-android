package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.PaymentElement.Configuration.BillingDetailsCollectionConfiguration
import com.stripe.android.checkout.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
import com.stripe.android.checkout.PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSelectionChooser
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full as PSFull

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
    fun `load propagates billingDetailsCollectionConfiguration from the payment element configuration`() =
        runScenario {
            val configuration = CheckoutController.Configuration()
                .paymentElement(
                    PaymentElement.Configuration().billingDetailsCollectionConfiguration(bdcc(address = Full))
                )
                .build()

            loader.load(checkoutControllerState(configuration = configuration))

            assertThat(
                confirmationStateHolder.state?.configuration?.billingDetailsCollectionConfiguration?.address
            ).isEqualTo(PSFull)
        }

    @Test
    fun `load forces attachDefaultsToPaymentMethod true`() = runScenario {
        val configuration = CheckoutController.Configuration()
            .paymentElement(
                PaymentElement.Configuration().billingDetailsCollectionConfiguration(
                    bdcc(attachDefaultsToPaymentMethod = false)
                )
            )
            .build()

        loader.load(checkoutControllerState(configuration = configuration))

        assertThat(
            confirmationStateHolder.state?.configuration?.billingDetailsCollectionConfiguration
                ?.attachDefaultsToPaymentMethod
        ).isTrue()
    }

    @Test
    fun `load routes the selection through the chooser`() = runScenario(
        loaderSelection = PaymentSelection.GooglePay,
        chosenSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) {
        // The customer's prior selection is what the chooser must be offered as the previous value.
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        loader.load(checkoutControllerState())

        // The committed state adopts whatever the chooser returned, not the loader's recomputed
        // selection, and the selection holder is updated to match.
        assertThat(confirmationStateHolder.state?.selection)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(selectionHolder.selection.value)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val call = chooser.lastCall
        assertThat(call?.previousSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(call?.newSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `load preserves a non-default selection across a mutation`() = runScenario(
        // The loader would recompute a card selection, but the customer's Google Pay pick must win.
        loaderSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        isGooglePayAvailable = true,
        selectionChooser = { savedStateHandle ->
            DefaultEmbeddedSelectionChooser(
                savedStateHandle = savedStateHandle,
                formHelperFactory = EmbeddedFormHelperFactory(
                    linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                    embeddedSelectionHolder = EmbeddedSelectionHolder(savedStateHandle),
                    cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                    savedStateHandle = savedStateHandle,
                ),
                eventReporter = FakeEventReporter(),
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                internalRowSelectionCallback = { null },
            )
        },
    ) {
        // Initial load seeds the chooser's stored previous configuration.
        loader.load(checkoutControllerState())

        // The customer picks Google Pay after the initial load.
        selectionHolder.set(PaymentSelection.GooglePay)

        // A mutation reloads with the same configuration, so the chooser keeps the customer's
        // selection rather than adopting the loader's recomputed one.
        loader.load(requireNotNull(stateHolder.state))

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

    private fun bdcc(
        address: BillingDetailsCollectionConfiguration.AddressCollectionMode = Automatic,
        attachDefaultsToPaymentMethod: Boolean = false,
    ) = BillingDetailsCollectionConfiguration(
        address = address,
        attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
    )

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
        chosenSelection: PaymentSelection? = null,
        shouldFail: Boolean = false,
        isGooglePayAvailable: Boolean = false,
        // When null, a RecordingSelectionChooser is used. Pass a factory to exercise the real
        // DefaultEmbeddedSelectionChooser (it needs the shared SavedStateHandle to track state).
        selectionChooser: ((SavedStateHandle) -> EmbeddedSelectionChooser)? = null,
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
        val recordingChooser = RecordingSelectionChooser(chosenSelection)
        val chooser = selectionChooser?.invoke(savedStateHandle) ?: recordingChooser
        val loader = CheckoutStateLoader(
            merchantDisplayName = merchantDisplayName,
            flagImageResolver = flagImageResolver,
            paymentElementLoader = FakePaymentElementLoader(
                paymentSelection = loaderSelection,
                shouldFail = shouldFail,
                isGooglePayAvailable = isGooglePayAvailable,
            ),
            selectionChooser = chooser,
            selectionHolder = selectionHolder,
            confirmationStateHolder = confirmationStateHolder,
            stateHolder = stateHolder,
        )

        Scenario(
            loader = loader,
            stateHolder = stateHolder,
            confirmationStateHolder = confirmationStateHolder,
            selectionHolder = selectionHolder,
            chooser = recordingChooser,
            imageLoader = imageLoader,
        ).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val loader: CheckoutStateLoader,
        val stateHolder: CheckoutControllerStateHolder,
        val confirmationStateHolder: CheckoutConfirmationStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val chooser: RecordingSelectionChooser,
        val imageLoader: FakeStripeImageLoader,
    )

    // Records the arguments of the most recent choose() call and returns a preconfigured selection,
    // so tests can verify the loader threads the holder's previous selection and the loader's new
    // selection into the chooser.
    private class RecordingSelectionChooser(
        private val result: PaymentSelection?,
    ) : EmbeddedSelectionChooser {
        var lastCall: Call? = null

        override fun choose(
            paymentMethodMetadata: PaymentMethodMetadata,
            paymentMethods: List<PaymentMethod>?,
            previousSelection: PaymentSelection?,
            newSelection: PaymentSelection?,
            newConfiguration: CommonConfiguration,
            formSheetAction: EmbeddedPaymentElement.FormSheetAction,
        ): PaymentSelection? {
            lastCall = Call(previousSelection = previousSelection, newSelection = newSelection)
            return result
        }

        data class Call(
            val previousSelection: PaymentSelection?,
            val newSelection: PaymentSelection?,
        )
    }
}
