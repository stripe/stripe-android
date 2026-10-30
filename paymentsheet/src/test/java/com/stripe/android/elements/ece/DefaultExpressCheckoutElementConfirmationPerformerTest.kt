package com.stripe.android.elements.ece

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutCommonConfigurationFactory
import com.stripe.android.checkout.CheckoutConfirmationFinalizer
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.FakeCheckoutSessionRefresher
import com.stripe.android.checkout.TestCheckoutAnalyticsPerformerFactory
import com.stripe.android.core.Logger
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.Appearance.ButtonTheme
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeAnalyticsRequestExecutor
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultExpressCheckoutElementConfirmationPerformerTest {

    @Test
    fun `confirm reports unexpected error when state is not loaded`() = runScenario(
        state = null,
        expressButton = createGooglePayExpressButton(),
    ) {
        performer.confirm(expressButton)

        val call = errorReporter.awaitCall()

        assertThat(call.errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_STATE_ON_CONFIRM)
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()
        val result = results.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        val error = (result as CheckoutController.Result.Failed).error
        assertThat(error).isInstanceOf<IllegalStateException>()
        assertThat(error).hasMessageThat().isEqualTo("Cannot confirm checkout before it is configured.")
    }

    @Test
    fun `confirm reports unexpected error when confirmation args are null`() = runScenario(
        state = CheckoutControllerStateFactory.create(
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(merchantCountry = null),
        ),
        expressButton = createGooglePayExpressButton(),
    ) {
        performer.confirm(expressButton)

        val call = errorReporter.awaitCall()

        assertThat(call.errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.EXPRESS_CHECKOUT_ELEMENT_NULL_CONFIRMATION_ARGS_ON_CONFIRM
        )
        assertThat(call.stripeException).isNull()
        assertThat(call.additionalNonPiiParams).isEmpty()
        val result = results.awaitItem()
        assertThat(result).isInstanceOf<CheckoutController.Result.Failed>()
        val error = (result as CheckoutController.Result.Failed).error
        assertThat(error).isInstanceOf<IllegalStateException>()
        assertThat(error).hasMessageThat().isEqualTo(
            "Cannot confirm checkout because the selected payment option cannot be confirmed."
        )
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() {
        val state = createState()

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = requireNotNull(state.expressCheckoutElementPaymentMethodMetadata),
            ),
        ) {
            val args = confirmAndAwaitStart()

            assertThat(args.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()
            val option = args.confirmationOption as GooglePayConfirmationOption
            assertThat(option.config.shippingAddressParameters).isNull()
            assertThat(args.paymentMethodMetadata)
                .isEqualTo(stateHolder.state?.expressCheckoutElementPaymentMethodMetadata)
        }
    }

    @Test
    fun `confirm requests a Google Pay shipping address for allowed countries`() {
        val state = createState(
            allowedShippingCountries = listOf("US", "CA"),
        )

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = requireNotNull(state.expressCheckoutElementPaymentMethodMetadata),
                shippingAddressRequired = true,
            ),
        ) {
            val args = confirmAndAwaitStart()

            val option = args.confirmationOption as GooglePayConfirmationOption
            assertThat(option.config.shippingAddressParameters).isEqualTo(
                GooglePayJsonFactory.ShippingAddressParameters(
                    isRequired = true,
                    allowedCountryCodes = setOf("US", "CA"),
                )
            )
        }
    }

    @Test
    fun `confirm collects email when unavailable and uses automatic collection for other billing details`() {
        val state = createState()

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = requireNotNull(state.expressCheckoutElementPaymentMethodMetadata),
            ),
        ) {
            val args = confirmAndAwaitStart()

            val option = args.confirmationOption as GooglePayConfirmationOption
            val billingDetails = option.config.billingDetailsCollectionConfiguration
            assertThat(billingDetails.name).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
            )
            assertThat(billingDetails.phone).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
            )
            assertThat(billingDetails.email).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
            assertThat(billingDetails.address).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
            )
            assertThat(billingDetails.attachDefaultsToPaymentMethod).isTrue()
        }
    }

    @Test
    fun `confirm collects a billing address when required by the Checkout Session`() {
        val state = createState(requiresBillingAddress = true)

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = requireNotNull(state.expressCheckoutElementPaymentMethodMetadata),
            ),
        ) {
            val args = confirmAndAwaitStart()

            val option = args.confirmationOption as GooglePayConfirmationOption
            assertThat(option.config.billingDetailsCollectionConfiguration.address).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            )
        }
    }

    @Test
    fun `confirm starts confirmation with a Link option`() {
        val state = CheckoutControllerStateFactory.create(
            expressCheckoutElementPaymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = LinkTestUtils.createLinkConfiguration(),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null,
                ),
            ),
        )

        runScenario(
            state = state,
            expressButton = ExpressButton.Link.create(
                paymentMethodMetadata = requireNotNull(state.expressCheckoutElementPaymentMethodMetadata),
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
        ) {
            val args = confirmAndAwaitStart()

            assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
            val option = args.confirmationOption as LinkConfirmationOption
            assertThat(option.linkLaunchMode).isEqualTo(
                LinkLaunchMode.Full(showSecondaryButton = false)
            )
            assertThat(args.paymentMethodMetadata)
                .isEqualTo(stateHolder.state?.expressCheckoutElementPaymentMethodMetadata)
        }
    }

    private fun createState(
        allowedShippingCountries: List<String>? = null,
        requiresBillingAddress: Boolean = false,
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            configuration = CheckoutController.Configuration()
                .expressCheckoutElement(ExpressCheckoutElement.Configuration())
                .build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                merchantCountry = "US",
                allowedShippingCountries = allowedShippingCountries,
                requiresBillingAddress = requiresBillingAddress,
            ),
        )
    }

    private fun createGooglePayExpressButton(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        shippingAddressRequired: Boolean = false,
    ): ExpressButton.GooglePay {
        return ExpressButton.GooglePay.create(
            paymentMethodMetadata = paymentMethodMetadata,
            googlePayConfiguration =
                ExpressCheckoutElement.Configuration.GooglePayConfiguration().build(),
            shippingAddressRequired = shippingAddressRequired,
            buttonTheme = ButtonTheme.Automatic,
        )
    }

    @Suppress("LongMethod")
    private fun runScenario(
        state: CheckoutControllerState?,
        expressButton: ExpressButton,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val errorReporter = FakeErrorReporter()
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        stateHolder.state = state
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val results = Turbine<CheckoutController.Result>()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val analyticsPerformer = TestCheckoutAnalyticsPerformerFactory.create(
            savedStateHandle = savedStateHandle,
            stateHolder = stateHolder,
            analyticsRequestExecutor = analyticsRequestExecutor,
            workContext = UnconfinedTestDispatcher(testScheduler),
        )
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            sessionRefresher = sessionRefresher,
            confirmationFinalizer = { CheckoutConfirmationFinalizer.Result.Completed(it) },
            resultReporter = analyticsPerformer,
            logger = Logger.noop(),
            resultCallback = results::add,
            viewModelScope = backgroundScope,
        )
        val performer = DefaultExpressCheckoutElementConfirmationPerformer(
            stateHolder = stateHolder,
            confirmationHandler = confirmationHandler,
            operationCoordinator = operationCoordinator,
            analyticsPerformer = analyticsPerformer,
            commonConfigurationFactory = CheckoutCommonConfigurationFactory(appName = "Test App"),
            errorReporter = errorReporter,
            statusBarColor = null,
            viewModelScope = backgroundScope,
        )

        Scenario(
            performer = performer,
            confirmationHandler = confirmationHandler,
            errorReporter = errorReporter,
            stateHolder = stateHolder,
            expressButton = expressButton,
            results = results,
            analyticsRequestExecutor = analyticsRequestExecutor,
        ).block()

        results.ensureAllEventsConsumed()
        confirmationHandler.validate()
        analyticsRequestExecutor.requestTurbine.expectNoEvents()
        analyticsRequestExecutor.validate()
        sessionRefresher.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private class Scenario(
        val performer: DefaultExpressCheckoutElementConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val errorReporter: FakeErrorReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val expressButton: ExpressButton,
        val results: Turbine<CheckoutController.Result>,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
    ) {
        suspend fun confirmAndAwaitStart(): ConfirmationHandler.Args {
            performer.confirm(expressButton)
            val args = confirmationHandler.startTurbine.awaitItem()
            analyticsRequestExecutor.awaitEvent("checkout_elements.confirm.started")
            return args
        }
    }
}
