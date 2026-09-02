package com.stripe.android.elements.ece

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutAnalyticsPerformer
import com.stripe.android.checkout.CheckoutCommonConfigurationFactory
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutOperationCoordinator
import com.stripe.android.checkout.FakeCheckoutSessionRefresher
import com.stripe.android.core.Logger
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.Appearance.ButtonTheme
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.launch
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
    }

    @Test
    fun `confirm starts confirmation with a Google Pay option`() {
        val state = createState()

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
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
                paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata,
                shippingAddressRequired = true,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
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
    fun `confirm uses ECE billing details collection configuration`() {
        val state = createState(
            eceBillingDetailsCollectionConfiguration =
                ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration()
                    .name(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Always
                    )
                    .email(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Never
                    )
                    .address(
                        ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration
                            .AddressCollectionMode.Full
                    )
        )

        runScenario(
            state = state,
            expressButton = createGooglePayExpressButton(
                paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
            val option = args.confirmationOption as GooglePayConfirmationOption
            val billingDetails = option.config.billingDetailsCollectionConfiguration
            assertThat(billingDetails.name).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
            assertThat(billingDetails.phone).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
            )
            assertThat(billingDetails.email).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
            )
            assertThat(billingDetails.address).isEqualTo(
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
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
                paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
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
                paymentMethodMetadata = state.expressCheckoutElementPaymentMethodMetadata,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                buttonTheme = ButtonTheme.Automatic,
            ),
        ) {
            performer.confirm(expressButton)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.confirmationOption).isInstanceOf<LinkConfirmationOption>()
            assertThat(args.paymentMethodMetadata)
                .isEqualTo(stateHolder.state?.expressCheckoutElementPaymentMethodMetadata)
        }
    }

    private fun createState(
        allowedShippingCountries: List<String>? = null,
        requiresBillingAddress: Boolean = false,
        eceBillingDetailsCollectionConfiguration:
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration =
            ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration(),
    ): CheckoutControllerState {
        return CheckoutControllerStateFactory.create(
            configuration = CheckoutController.Configuration()
                .expressCheckoutElement(
                    ExpressCheckoutElement.Configuration().billingDetailsCollectionConfiguration(
                        eceBillingDetailsCollectionConfiguration
                    )
                )
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
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = SheetStateHolder(savedStateHandle),
            sessionRefresher = sessionRefresher,
            logger = Logger.noop(),
            resultCallback = {},
        )
        val paymentSheetEventReporter = FakeEventReporter()
        val analyticsPerformer = CheckoutAnalyticsPerformer(
            confirmationHandler = confirmationHandler,
            eventReporter = paymentSheetEventReporter,
            savedStateHandle = savedStateHandle,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            analyticsPerformer.reportConfirmationResults()
        }
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
            paymentSheetEventReporter = paymentSheetEventReporter,
            stateHolder = stateHolder,
            expressButton = expressButton,
        ).block()

        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
        paymentSheetEventReporter.validate()
    }

    private class Scenario(
        val performer: DefaultExpressCheckoutElementConfirmationPerformer,
        val confirmationHandler: FakeConfirmationHandler,
        val errorReporter: FakeErrorReporter,
        val paymentSheetEventReporter: FakeEventReporter,
        val stateHolder: CheckoutControllerStateHolder,
        val expressButton: ExpressButton,
    )
}
