package com.stripe.android.link.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultLinkConfirmationHandlerTest {
    private val dispatcher = UnconfinedTestDispatcher()

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
            cvc = "333"
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
            configuration = configuration,
            linkAccount = TestFactory.LINK_ACCOUNT,
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cvc = CVC,
            initMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = configuration.stripeIntent.clientSecret.orEmpty()
            )
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
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        assertThat(result).isEqualTo(Result.Succeeded)
        confirmationHandler.startTurbine.awaitItem().assertConfirmationArgs(
            configuration = configuration,
            linkAccount = TestFactory.LINK_ACCOUNT,
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            cvc = null,
            initMode = PaymentElementLoader.InitializationMode.SetupIntent(
                clientSecret = configuration.stripeIntent.clientSecret.orEmpty()
            )
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
            linkAccount = TestFactory.LINK_ACCOUNT
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
            linkAccount = TestFactory.LINK_ACCOUNT
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
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        assertThat(result).isEqualTo(Result.Failed(R.string.stripe_something_went_wrong.resolvableString))
        assertThat(logger.errorLogs)
            .containsExactly("DefaultLinkConfirmationHandler: Payment confirmation returned null" to null)
    }

    @Test
    fun `invalid client secret yields error result`() = runTest(dispatcher) {
        val confirmationHandler = FakeConfirmationHandler()
        val logger = FakeLogger()
        val handler = createHandler(
            confirmationHandler = confirmationHandler,
            logger = logger,
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
                    clientSecret = null
                )
            )
        )

        confirmationHandler.awaitResultTurbine.add(null)

        val result = handler.confirm(
            paymentDetails = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        assertThat(result).isEqualTo(Result.Failed(R.string.stripe_something_went_wrong.resolvableString))
        assertThat(logger.errorLogs)
            .containsExactly(
                "DefaultLinkConfirmationHandler: Failed to confirm payment"
                    to DefaultLinkConfirmationHandler.NO_CLIENT_SECRET_FOUND
            )
    }

    private fun ConfirmationHandler.Args.assertConfirmationArgs(
        configuration: LinkConfiguration,
        paymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        initMode: PaymentElementLoader.InitializationMode
    ) {
        assertThat(intent).isEqualTo(configuration.stripeIntent)
        val option = confirmationOption as PaymentMethodConfirmationOption.New
        assertThat(option.createParams).isEqualTo(
            PaymentMethodCreateParams.createLink(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = linkAccount.clientSecret,
                extraParams = cvc?.let { mapOf("card" to mapOf("cvc" to cvc)) },
            )
        )
        assertThat(shippingDetails).isEqualTo(configuration.shippingDetails)
        assertThat(initializationMode).isEqualTo(initMode)
    }

    private fun createHandler(
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        logger: Logger = FakeLogger(),
        confirmationHandler: FakeConfirmationHandler = FakeConfirmationHandler()
    ): DefaultLinkConfirmationHandler {
        val handler = DefaultLinkConfirmationHandler(
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            logger = logger
        )
        confirmationHandler.validate()
        return handler
    }

    companion object {
        private const val CVC = "333"
    }
}
