package com.stripe.android.link.confirmation

import com.google.common.truth.Truth
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
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
            confirmationHandler = confirmationHandler
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

        Truth.assertThat(result).isEqualTo(Result.Succeeded)
        Truth.assertThat(confirmationHandler.startTurbine.awaitItem())
            .isEqualTo(
                ConfirmationHandler.Args(
                    intent = configuration.stripeIntent,
                    confirmationOption = PaymentMethodConfirmationOption.New(
                        createParams = PaymentMethodCreateParams.createLink(
                            paymentDetailsId = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
                            consumerSessionClientSecret = TestFactory.LINK_ACCOUNT.clientSecret,
                            extraParams = emptyMap(),
                        ),
                        optionsParams = null,
                        shouldSave = false
                    ),
                    appearance = PaymentSheet.Appearance(),
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = configuration.stripeIntent.clientSecret ?: ""
                    ),
                    shippingDetails = null
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

        Truth.assertThat(result).isEqualTo(Result.Failed(errorMessage))
        Truth.assertThat(logger.errorLogs)
            .containsExactly("Failed to confirm payment" to error)
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

        Truth.assertThat(result).isEqualTo(Result.Canceled)
    }

    @Test
    fun `null confirmation yields canceled result`() = runTest(dispatcher) {
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

        Truth.assertThat(result).isEqualTo(Result.Failed(R.string.stripe_something_went_wrong.resolvableString))
        Truth.assertThat(logger.errorLogs)
            .containsExactly("Payment confirmation returned null" to null)
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
}
