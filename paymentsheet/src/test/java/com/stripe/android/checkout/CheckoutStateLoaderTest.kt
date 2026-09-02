package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSelectionChooser
import com.stripe.android.paymentelement.embedded.content.EmbeddedSelectionChooser
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.uicore.FormInsets
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.PrimaryButtonStyle
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeShapes
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeTypography
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(
    CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
    CardFundingFilteringPrivatePreview::class,
)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutStateLoaderTest {

    @Test
    fun `loadInitial commits only payment element metadata when ECE is not configured`() = runScenario {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(stateHolder.state?.paymentMethodMetadata).isNotNull()
        assertThat(stateHolder.state?.expressCheckoutElementPaymentMethodMetadata).isNull()
    }

    @Test
    fun `loadInitial commits ECE payment method metadata when ECE is configured`() = runScenario {
        val configuration = CheckoutController.Configuration()
            .expressCheckoutElement(ExpressCheckoutElement.Configuration())
            .build()

        loader.loadInitial(configuration = configuration, checkoutSessionResponse = response())

        assertThat(stateHolder.state?.expressCheckoutElementPaymentMethodMetadata).isNotNull()
    }

    @Test
    fun `loadInitial loads payment element and ECE metadata in parallel`() = runScenario(
        loaderDelay = 1.seconds,
    ) {
        val startTime = currentTime()

        loader.loadInitial(
            configuration = CheckoutController.Configuration()
                .expressCheckoutElement(ExpressCheckoutElement.Configuration())
                .build(),
            checkoutSessionResponse = response(),
        )

        assertThat(currentTime() - startTime).isEqualTo(1.seconds.inWholeMilliseconds)
    }

    @Test
    fun `loadInitial reports immediate row selection action to payment element loader`() = runScenario(
        internalRowSelectionCallback = {},
    ) {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        val integrationConfiguration = paymentElementLoader.lastIntegrationConfiguration
            as PaymentElementLoader.Configuration.Embedded
        assertThat(integrationConfiguration.isRowSelectionImmediateAction).isTrue()
    }

    @Test
    fun `loadInitial applies payment element appearance to the global theme`() = runScenario {
        val previousTheme = StripeThemeSnapshot()
        try {
            loader.loadInitial(
                configuration = CheckoutController.Configuration()
                    .paymentElement(
                        PaymentElement.Configuration().appearance(
                            PaymentElement.Configuration.Appearance().colorsLight(
                                PaymentElement.Configuration.Appearance.Colors.light()
                                    .primary(0xFF123456.toInt())
                            )
                        )
                    )
                    .build(),
                checkoutSessionResponse = response(),
            )

            assertThat(StripeTheme.colorsLightMutable.materialColors.primary.toArgb())
                .isEqualTo(0xFF123456.toInt())
        } finally {
            previousTheme.restore()
        }
    }

    @Test
    fun `loadInitial passes payment method order to payment method metadata`() = runScenario {
        loader.loadInitial(
            configuration = CheckoutController.Configuration()
                .paymentElement(
                    PaymentElement.Configuration().paymentMethodOrder(listOf("klarna", "card"))
                )
                .build(),
            checkoutSessionResponse = response(),
        )

        assertThat(stateHolder.state?.paymentMethodMetadata?.paymentMethodOrder)
            .isEqualTo(listOf("klarna", "card"))
    }

    @Test
    fun `loadInitial seeds collected details with the defaults billing address`() = runScenario {
        val address = CheckoutController.Address()
            .city(" San Francisco ")
            .country(" US ")
            .line1(" 510 Townsend St ")
            .postalCode(" 94103 ")
            .state(" CA ")

        loader.loadInitial(
            configuration = CheckoutController.Configuration()
                .defaults(
                    CheckoutController.Configuration.Defaults()
                        .billingDetails(
                            CheckoutController.Configuration.Defaults.ContactDetails().address(address),
                        ),
                )
                .build(),
            checkoutSessionResponse = response(),
        )

        val billingAddress = requireNotNull(stateHolder.state?.collectedDetails?.billingAddress)
        assertThat(billingAddress.city).isEqualTo("San Francisco")
        assertThat(billingAddress.country).isEqualTo("US")
        assertThat(billingAddress.line1).isEqualTo("510 Townsend St")
        assertThat(billingAddress.postalCode).isEqualTo("94103")
        assertThat(billingAddress.state).isEqualTo("CA")
        assertThat(stateHolder.state?.embeddedConfiguration?.defaultBillingDetails?.address?.postalCode)
            .isEqualTo("94103")
    }

    @Test
    fun `loadInitial seeds collected details from configuration defaults`() = runScenario {
        val configuration = CheckoutController.Configuration()
            .defaults(
                CheckoutController.Configuration.Defaults()
                    .billingDetails(
                        CheckoutController.Configuration.Defaults.ContactDetails()
                            .name("Jane Billing")
                            .address(CheckoutController.Address().country("US").city("Denver")),
                    )
                    .shippingDetails(
                        CheckoutController.Configuration.Defaults.ContactDetails().name("John Shipping"),
                    ),
            )
            .build()

        loader.loadInitial(configuration = configuration, checkoutSessionResponse = response())

        val collected = requireNotNull(stateHolder.state).collectedDetails
        assertThat(collected.billingName).isEqualTo("Jane Billing")
        assertThat(collected.billingAddress?.country).isEqualTo("US")
        assertThat(collected.billingAddress?.city).isEqualTo("Denver")
        assertThat(collected.shippingName).isEqualTo("John Shipping")
    }

    @Test
    fun `loadInitial populates the customer state holder from the loaded customer`() = runScenario(
        customer = savedCustomer(),
    ) {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(customerStateHolder.customer.value).isEqualTo(savedCustomer())
        assertThat(customerStateHolder.paymentMethods.value).isEqualTo(savedCustomer().paymentMethods)
    }

    @Test
    fun `loadInitial leaves the customer state holder empty when the session has no customer`() = runScenario {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        assertThat(customerStateHolder.customer.value).isNull()
        assertThat(customerStateHolder.paymentMethods.value).isEmpty()
    }

    @Test
    fun `reload updates the customer state holder when the loaded customer changes`() = runScenario(
        customer = savedCustomer(),
    ) {
        // The initial load seeds the shared holder with the session's saved card.
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())
        assertThat(customerStateHolder.paymentMethods.value).isEqualTo(savedCustomer().paymentMethods)

        // The customer's saved methods change (their only card is removed); a reload must push the
        // new set through to the shared holder rather than leaving the stale one from the initial
        // load in place.
        paymentElementLoader.updatePaymentMethods(emptyList())
        loader.reload(requireNotNull(stateHolder.state))

        assertThat(customerStateHolder.paymentMethods.value).isEmpty()
    }

    @Test
    fun `clear removes controller and customer state`() = runScenario(
        customer = savedCustomer(),
    ) {
        loader.loadInitial(configuration = defaultConfiguration(), checkoutSessionResponse = response())

        loader.clear()

        assertThat(stateHolder.state).isNull()
        assertThat(customerStateHolder.customer.value).isNull()
        assertThat(customerStateHolder.paymentMethods.value).isEmpty()
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

        assertThat(stateHolder.session.value?.id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
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

    private fun savedCustomer() = CustomerState(
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        defaultPaymentMethodId = null,
    )

    // A committed state as [CheckoutStateLoader] would produce it, for exercising reloads. The
    // resolved metadata/configuration are placeholders; reload recomputes and overwrites them.
    private fun committedState(
        paymentSelection: PaymentSelection? = null,
        temporarySelection: String? = null,
        previousNewSelections: Bundle = Bundle(),
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
    ) = CheckoutControllerState(
        configuration = CheckoutController.Configuration().build(),
        checkoutSessionResponse = checkoutSessionResponse,
        flagImages = null,
        collectedDetails = CheckoutCollectedDetails(email = null),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        expressCheckoutElementPaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
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
        loaderSelection: PaymentSelection? = null,
        chosenSelection: PaymentSelection? = null,
        shouldFail: Boolean = false,
        isGooglePayAvailable: Boolean = false,
        customer: CustomerState? = null,
        internalRowSelectionCallback: (() -> Unit)? = null,
        loaderDelay: Duration = Duration.ZERO,
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
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = stateHolder.selection,
            paymentMethodMetadataFlow = stateHolder.stateFlow.mapAsStateFlow {
                it?.paymentMethodMetadata
            },
            customerMetadata = stateHolder.stateFlow.mapAsStateFlow {
                it?.paymentMethodMetadata?.customerMetadata
            },
        )
        val recordingChooser = RecordingSelectionChooser(chosenSelection)
        val chooser = selectionChooser?.invoke(savedStateHandle) ?: recordingChooser
        val paymentElementLoader = FakePaymentElementLoader(
            paymentSelection = loaderSelection,
            shouldFail = shouldFail,
            isGooglePayAvailable = isGooglePayAvailable,
            customer = customer,
            delay = loaderDelay,
        )
        val loader = CheckoutStateLoader(
            embeddedConfigurationFactory = CheckoutEmbeddedConfigurationFactory(appName = "Example, Inc."),
            commonConfigurationFactory = CheckoutCommonConfigurationFactory(appName = "Example, Inc."),
            flagImageResolver = flagImageResolver,
            paymentElementLoader = paymentElementLoader,
            selectionChooser = chooser,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            internalRowSelectionCallback = { internalRowSelectionCallback },
        )

        Scenario(
            loader = loader,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            paymentElementLoader = paymentElementLoader,
            chooser = recordingChooser,
            imageLoader = imageLoader,
            currentTime = { testScheduler.currentTime },
        ).block()

        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val loader: CheckoutStateLoader,
        val stateHolder: CheckoutControllerStateHolder,
        val customerStateHolder: CustomerStateHolder,
        val paymentElementLoader: FakePaymentElementLoader,
        val chooser: RecordingSelectionChooser,
        val imageLoader: FakeStripeImageLoader,
        val currentTime: () -> Long,
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

internal class StripeThemeSnapshot(
    private val colorsLight: StripeColors = StripeTheme.colorsLightMutable,
    private val colorsDark: StripeColors = StripeTheme.colorsDarkMutable,
    private val shapes: StripeShapes = StripeTheme.shapesMutable,
    private val typography: StripeTypography = StripeTheme.typographyMutable,
    private val primaryButtonStyle: PrimaryButtonStyle = StripeTheme.primaryButtonStyle,
    private val formInsets: FormInsets = StripeTheme.formInsets,
    private val sectionSpacing: Float? = StripeTheme.customSectionSpacing,
    private val textFieldInsets: FormInsets = StripeTheme.textFieldInsets,
    private val iconStyle: IconStyle = StripeTheme.iconStyle,
    private val verticalModeRowPadding: Float = StripeTheme.verticalModeRowPadding,
) {
    fun restore() {
        StripeTheme.colorsLightMutable = colorsLight
        StripeTheme.colorsDarkMutable = colorsDark
        StripeTheme.shapesMutable = shapes
        StripeTheme.typographyMutable = typography
        StripeTheme.primaryButtonStyle = primaryButtonStyle
        StripeTheme.formInsets = formInsets
        StripeTheme.customSectionSpacing = sectionSpacing
        StripeTheme.textFieldInsets = textFieldInsets
        StripeTheme.iconStyle = iconStyle
        StripeTheme.verticalModeRowPadding = verticalModeRowPadding
    }
}
