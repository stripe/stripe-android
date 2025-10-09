package com.stripe.android.link.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.Address
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkPassthroughConfirmationOption
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class DefaultLinkConfirmationHandlerTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful confirmation yields success result`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
            configuration = configuration,
            linkAccount = TestFactory.LINK_ACCOUNT,
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cvc = CVC,
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    postalCode = "12312",
                    country = "US",
                ),
            ),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )
    }

    @Test
    fun `successful confirmation yields success result with setup intent`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = SetupIntentFixtures.SI_SUCCEEDED
        )
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = null,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
            configuration = configuration,
            linkAccount = TestFactory.LINK_ACCOUNT,
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cvc = null,
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    postalCode = "12312",
                    country = "US",
                ),
            ),
            allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )
    }

    @Test
    fun `failed confirmation yields failed result`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val errorMessage = "Something went wrong".resolvableString

        val confirmationHandler = FakeConfirmationHandler()
        val logger = FakeLogger()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            logger = logger
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Failed(
                cause = error,
                message = errorMessage,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment
            )
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Failed(errorMessage))
        assertThat(logger.errorLogs)
            .containsExactly("DefaultLinkConfirmationHandler: Failed to confirm payment" to error)
    }

    @Test
    fun `canceled confirmation yields canceled result`() = runTest(dispatcher) {
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Canceled(ConfirmationHandler.Result.Canceled.Action.None)
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Canceled)
    }

    @Test
    fun `null confirmation yields failed result`() = runTest(dispatcher) {
        val confirmationHandler = FakeConfirmationHandler()
        val logger = FakeLogger()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            logger = logger
        )

        confirmationHandler.awaitResultTurbine.add(null)

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Failed(R.string.stripe_something_went_wrong.resolvableString))
        assertThat(logger.errorLogs)
            .containsExactly("DefaultLinkConfirmationHandler: Payment confirmation returned null" to null)
    }

    @Test
    fun `confirm with New LinkPaymentDetails calls uses correct confirmation args`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.LINK_NEW_PAYMENT_DETAILS,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
            configuration = configuration,
            linkAccount = TestFactory.LINK_ACCOUNT,
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cvc = CVC,
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    postalCode = "12312",
                    country = "US",
                ),
            ),
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )
    }

    @Test
    fun `confirm with saved LinkPaymentDetails creates correct confirmation args`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val savedPaymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS
        val result = handler.confirm(
            paymentDetails = savedPaymentDetails,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertSavedConfirmationArgs(
            configuration = configuration,
            paymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS,
            cvc = CVC
        )
    }

    @Test
    fun `confirm with saved LinkPaymentDetails in passthrough mode omits CVC`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(passthroughModeEnabled = true)
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val result = handler.confirm(
            paymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = CVC,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertSavedConfirmationArgs(
            configuration = configuration,
            paymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS,
            cvc = null
        )
    }

    @Test
    fun `confirm with card payment details in passthrough mode uses correct confirmation args`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION.copy(passthroughModeEnabled = true)
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val result = handler.confirm(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = CVC,
                billingPhone = null
            )

            assertThat(result).isEqualTo(Result.Succeeded)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.intent).isEqualTo(configuration.stripeIntent)

            val option = args.confirmationOption as LinkPassthroughConfirmationOption
            assertThat(option.paymentDetailsId).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id)
            assertThat(option.expectedPaymentMethodType).isEqualTo(ConsumerPaymentDetails.Card.TYPE)
        }

    @Test
    fun `confirm with bank account in passthrough mode uses correct confirmation args`() = runTest(dispatcher) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            passthroughModeEnabled = true,
            linkMode = LinkMode.LinkCardBrand
        )
        val confirmationHandler = FakeConfirmationHandler()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration
        )

        confirmationHandler.awaitResultTurbine.add(
            item = ConfirmationHandler.Result.Succeeded(
                intent = configuration.stripeIntent,
                deferredIntentConfirmationType = null
            )
        )

        val bankAccount = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT

        val result = handler.confirm(
            paymentDetails = bankAccount,
            linkAccount = TestFactory.LINK_ACCOUNT,
            cvc = null,
            billingPhone = null
        )

        assertThat(result).isEqualTo(Result.Succeeded)

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.intent).isEqualTo(configuration.stripeIntent)

        val option = args.confirmationOption as LinkPassthroughConfirmationOption
        assertThat(option.paymentDetailsId).isEqualTo(bankAccount.id)
        assertThat(option.expectedPaymentMethodType).isEqualTo(ConsumerPaymentDetails.Card.TYPE)
    }

    @Test
    fun `confirm with bank account and USBankAccount type in passthrough mode uses correct confirmation args`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION.copy(
                passthroughModeEnabled = true,
                linkMode = LinkMode.LinkCardBrand,
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                    paymentMethodTypes = listOf(USBankAccount.code)
                )
            )
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val bankAccount = TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT

            val result = handler.confirm(
                paymentDetails = bankAccount,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = null,
                billingPhone = null
            )

            assertThat(result).isEqualTo(Result.Succeeded)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.intent).isEqualTo(configuration.stripeIntent)

            val option = args.confirmationOption as LinkPassthroughConfirmationOption
            assertThat(option.paymentDetailsId).isEqualTo(bankAccount.id)
            assertThat(option.expectedPaymentMethodType).isEqualTo(ConsumerPaymentDetails.BankAccount.TYPE)
        }

    @Test
    fun `confirm with passthrough payment details in passthrough mode uses correct confirmation args`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION.copy(passthroughModeEnabled = true)
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val passthroughDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH

            val result = handler.confirm(
                paymentDetails = passthroughDetails,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = null,
                billingPhone = null
            )

            assertThat(result).isEqualTo(Result.Succeeded)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.intent).isEqualTo(configuration.stripeIntent)

            val option = args.confirmationOption as LinkPassthroughConfirmationOption
            assertThat(option.paymentDetailsId).isEqualTo(passthroughDetails.id)
            assertThat(option.expectedPaymentMethodType).isEqualTo(ConsumerPaymentDetails.Card.TYPE)
        }

    @Test
    fun `confirm with passthrough payment details in payment method mode includes billing details`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION.copy(passthroughModeEnabled = false)
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val passthroughDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_PASSTHROUGH.copy(
                billingEmailAddress = "john@doe.com",
                billingAddress = ConsumerPaymentDetails.BillingAddress(
                    name = "John Doe",
                    line1 = "123 Main Street",
                    line2 = null,
                    postalCode = "12345",
                    locality = "Smalltown",
                    administrativeArea = "CA",
                    countryCode = CountryCode.US,
                )
            )

            val result = handler.confirm(
                paymentDetails = passthroughDetails,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = null,
                billingPhone = "+15555555555",
            )

            assertThat(result).isEqualTo(Result.Succeeded)

            val args = confirmationHandler.startTurbine.awaitItem()
            assertThat(args.intent).isEqualTo(configuration.stripeIntent)

            val option = args.confirmationOption as PaymentMethodConfirmationOption.New
            assertThat(option.createParams.billingDetails).isEqualTo(
                PaymentMethod.BillingDetails(
                    address = Address(
                        line1 = "123 Main Street",
                        line2 = null,
                        postalCode = "12345",
                        city = "Smalltown",
                        state = "CA",
                        country = "US",
                    ),
                    email = "john@doe.com",
                    name = "John Doe",
                    phone = "+15555555555",
                )
            )
        }

    @Test
    fun `handler without passive captcha params does not include params in confirmation args - new payment method`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration,
                passiveCaptchaParams = null
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val result = handler.confirm(
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = CVC,
                billingPhone = null
            )

            assertThat(result).isEqualTo(Result.Succeeded)
            confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
                configuration = configuration,
                linkAccount = TestFactory.LINK_ACCOUNT,
                paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                cvc = CVC,
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(
                        postalCode = "12312",
                        country = "US",
                    ),
                ),
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
                passiveCaptchaParams = null
            )
        }

    @Test
    fun `handler without passive captcha params does not include params in confirmation args - saved payment method`() =
        runTest(dispatcher) {
            val configuration = TestFactory.LINK_CONFIGURATION
            val confirmationHandler = FakeConfirmationHandler()
            val handler = createHandler(
                confirmationHandler = confirmationHandler,
                configuration = configuration,
                passiveCaptchaParams = null
            )

            confirmationHandler.awaitResultTurbine.add(
                item = ConfirmationHandler.Result.Succeeded(
                    intent = configuration.stripeIntent,
                    deferredIntentConfirmationType = null
                )
            )

            val savedPaymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS
            val result = handler.confirm(
                paymentDetails = savedPaymentDetails,
                linkAccount = TestFactory.LINK_ACCOUNT,
                cvc = CVC,
                billingPhone = null
            )

            assertThat(result).isEqualTo(Result.Succeeded)
            confirmationHandler.startTurbine.awaitItem().assertSavedConfirmationArgs(
                configuration = configuration,
                paymentDetails = TestFactory.LINK_SAVED_PAYMENT_DETAILS,
                cvc = CVC,
                passiveCaptchaParams = null
            )
        }

    private fun ConfirmationDefinition.Parameters.assertConfirmationArgs(
        configuration: LinkConfiguration,
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        billingDetails: PaymentMethod.BillingDetails?,
        allowRedisplay: PaymentMethod.AllowRedisplay?,
        passiveCaptchaParams: PassiveCaptchaParams? = PASSIVE_CAPTCHA_PARAMS
    ) {
        assertThat(intent).isEqualTo(configuration.stripeIntent)
        val option = confirmationOption as PaymentMethodConfirmationOption.New
        assertThat(option.createParams).isEqualTo(
            PaymentMethodCreateParams.createLink(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = linkAccount.clientSecret,
                extraParams = cvc?.let { mapOf("card" to mapOf("cvc" to cvc)) },
                billingDetails = billingDetails,
                allowRedisplay = allowRedisplay,
            )
        )
        assertThat(option.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
        assertThat(shippingDetails).isEqualTo(configuration.shippingDetails)
        assertThat(initializationMode).isEqualTo(configuration.initializationMode)
    }

    private fun ConfirmationDefinition.Parameters.assertSavedConfirmationArgs(
        configuration: LinkConfiguration,
        paymentDetails: LinkPaymentDetails.Saved,
        cvc: String?,
        passiveCaptchaParams: PassiveCaptchaParams? = PASSIVE_CAPTCHA_PARAMS
    ) {
        assertThat(intent).isEqualTo(configuration.stripeIntent)
        val option = confirmationOption as PaymentMethodConfirmationOption.Saved
        assertThat(option.paymentMethod.id).isEqualTo(paymentDetails.paymentDetails.paymentMethodId)
        assertThat(option.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)

        val optionsCard = option.optionsParams as? PaymentMethodOptionsParams.Card
        assertThat(optionsCard?.cvc).isEqualTo(cvc)
        assertThat(shippingDetails).isEqualTo(configuration.shippingDetails)
        assertThat(initializationMode).isEqualTo(configuration.initializationMode)
    }

    private fun createHandler(
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        logger: Logger = FakeLogger(),
        confirmationHandler: FakeConfirmationHandler = FakeConfirmationHandler(),
        passiveCaptchaParams: PassiveCaptchaParams? = PASSIVE_CAPTCHA_PARAMS
    ): DefaultLinkConfirmationHandler {
        val handler = DefaultLinkConfirmationHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            logger = logger,
            passiveCaptchaParams = passiveCaptchaParams
        )
        confirmationHandler.validate()
        return handler
    }

    companion object {
        private const val CVC = "333"
        private val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParamsFactory.passiveCaptchaParams()
    }
}
