package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSelectionChooser
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutStateLoaderTest {

    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `loadInitial commits state with payment method metadata`() = runScenario {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(stateHolder.state?.paymentMethodMetadata).isNotNull()
    }

    @Test
    fun `reload routes the selection through the chooser`() = runScenario(
        loaderSelection = PaymentSelection.GooglePay,
        chosenSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) {
        // The committed state's selection is what the chooser must be offered as the previous
        // value, sourced from the incoming state rather than a separate holder.
        loader.reload(committedState(paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION))

        // The committed state adopts whatever the chooser returned, not the loader's recomputed
        // selection.
        assertThat(stateHolder.state?.paymentSelection)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        val call = chooser.lastCall
        assertThat(call?.previousSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(call?.newSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `reload preserves a non-default selection across a mutation`() = runScenario(
        // The loader would recompute a card selection, but the customer's Google Pay pick must win.
        loaderSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        isGooglePayAvailable = true,
        selectionChooser = { savedStateHandle ->
            DefaultEmbeddedSelectionChooser(
                savedStateHandle = savedStateHandle,
                formHelperFactory = EmbeddedFormHelperFactory(
                    linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                    embeddedSelectionHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle),
                    cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                    savedStateHandle = savedStateHandle,
                    isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
                ),
                eventReporter = FakeEventReporter(),
                coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(UnconfinedTestDispatcher())),
                internalRowSelectionCallback = { null },
            )
        },
    ) {
        // Initial load seeds the chooser's stored previous configuration.
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        // The customer picks Google Pay after the initial load; in the single-state model that pick
        // lives on the committed state rather than a separate selection holder.
        val afterPick = requireNotNull(stateHolder.state).copy(paymentSelection = PaymentSelection.GooglePay)

        // A mutation reloads with the same configuration, so the chooser keeps the customer's
        // selection rather than adopting the loader's recomputed one.
        loader.reload(afterPick)

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `loadInitial commits state that exposes the checkout session`() = runScenario {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(stateHolder.checkoutSession.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        assertThat(stateHolder.state?.key).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        // No adaptive pricing in the response, so no flag images are resolved.
        assertThat(stateHolder.state?.flagImages).isNull()
    }

    @Test
    fun `loadInitial throws and commits nothing when the payment element loader fails`() = runScenario(
        shouldFail = true,
    ) {
        assertFailsWith<IllegalStateException> {
            loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())
        }

        assertThat(stateHolder.state).isNull()
    }

    @Test
    fun `reload reuses cached flag images when the currencies are unchanged`() = runScenario {
        val response = CheckoutSessionResponseFactory.create(adaptivePricingInfo = adaptivePricingInfo())

        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response)

        // Both currency flags are downloaded on the initial load.
        imageLoader.awaitLoadCall()
        imageLoader.awaitLoadCall()

        // A mutation reloads with the previously resolved images carried forward (on the committed
        // state) and the same currencies, so the cache is reused and nothing re-downloads.
        loader.reload(requireNotNull(stateHolder.state))

        imageLoader.ensureAllEventsConsumed()
    }

    @Test
    fun `reload carries the temporary selection and previous new selections forward`() = runScenario {
        val seeded = committedState(
            temporarySelection = "card",
            previousNewSelections = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            },
        )

        loader.reload(seeded)

        assertThat(stateHolder.state?.temporarySelection).isEqualTo("card")
        assertThat(stateHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `loadInitial resets the temporary selection and previous new selections`() = runScenario {
        // A prior state carries a temporary selection and a stashed new payment method; a fresh
        // configuration load must start from a clean slate rather than carrying them forward.
        stateHolder.state = committedState(
            temporarySelection = "card",
            previousNewSelections = Bundle().apply {
                putParcelable("cashapp", PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
            },
        )

        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(stateHolder.state?.temporarySelection).isNull()
        assertThat(stateHolder.getPreviousNewSelection("cashapp")).isNull()
    }

    private fun defaultConfiguration() = CheckoutController.Configuration().build()

    private fun response(
        merchantCountry: String? = "US",
    ) = CheckoutSessionResponseFactory.create(merchantCountry = merchantCountry)

    // A committed state as [CheckoutStateLoader] would produce it, for exercising reloads. The
    // resolved metadata/configuration are placeholders; reload recomputes and overwrites them.
    private fun committedState(
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
    ) = CheckoutControllerState(
        key = checkoutSessionResponse.id,
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = checkoutSessionResponse,
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        embeddedConfiguration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        paymentSelection = paymentSelection,
        temporarySelection = temporarySelection,
        previousNewSelections = previousNewSelections,
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
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        val recordingChooser = RecordingSelectionChooser(chosenSelection)
        val chooser = selectionChooser?.invoke(savedStateHandle) ?: recordingChooser
        val loader = CheckoutStateLoader(
            embeddedConfigurationFactory = CheckoutEmbeddedConfigurationFactory(merchantDisplayName),
            flagImageResolver = flagImageResolver,
            paymentElementLoader = FakePaymentElementLoader(
                paymentSelection = loaderSelection,
                shouldFail = shouldFail,
                isGooglePayAvailable = isGooglePayAvailable,
            ),
            selectionChooser = chooser,
            stateHolder = stateHolder,
        )

        Scenario(
            loader = loader,
            stateHolder = stateHolder,
            chooser = recordingChooser,
            imageLoader = imageLoader,
        ).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val loader: CheckoutStateLoader,
        val stateHolder: CheckoutControllerStateHolder,
        val chooser: RecordingSelectionChooser,
        val imageLoader: FakeStripeImageLoader,
    )

    // Records the arguments of the most recent choose() call and returns a preconfigured selection,
    // so tests can verify the loader threads the state's previous selection and the loader's new
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
