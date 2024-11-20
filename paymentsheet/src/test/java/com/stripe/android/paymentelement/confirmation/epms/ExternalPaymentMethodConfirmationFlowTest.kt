package com.stripe.android.paymentelement.confirmation.epms

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asSucceeded
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExternalPaymentMethodConfirmationFlowTest {
    @Before
    fun setup() {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler =
            ExternalPaymentMethodConfirmHandler { _, _ ->
                // Do nothing
            }
    }

    @After
    fun teardown() {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null
    }

    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val mediator = createExternalPaymentMethodConfirmationMediator(savedStateHandle)

        var called = false
        mediator.register(
            activityResultCaller = DummyActivityResultCaller(
                onLaunch = { called = true }
            ),
            onResult = {}
        )

        val action = mediator.action(
            option = EPM_CONFIRMATION_OPTION,
            intent = PAYMENT_INTENT,
        )

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        val parameters = savedStateHandle
            .get<Parameters<ExternalPaymentMethodConfirmationOption>>("ExternalPaymentMethodParameters")

        assertThat(parameters?.confirmationOption).isEqualTo(EPM_CONFIRMATION_OPTION)
        assertThat(parameters?.intent).isEqualTo(PAYMENT_INTENT)
        assertThat(parameters?.deferredIntentConfirmationType).isNull()

        assertThat(called).isTrue()
    }

    @Test
    fun `on result, should return confirmation result as expected`() = runTest {
        val countDownLatch = CountDownLatch(1)

        val savedStateHandle = SavedStateHandle().apply {
            set(
                "ExternalPaymentMethodParameters",
                Parameters(
                    confirmationOption = EPM_CONFIRMATION_OPTION,
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )
        }

        val mediator = createExternalPaymentMethodConfirmationMediator(savedStateHandle)
        val caller = DummyActivityResultCaller()

        var result: ConfirmationHandler.Result? = null

        mediator.register(
            activityResultCaller = caller,
            onResult = {
                result = it
            }
        )

        val call = caller.calls.awaitItem()

        call.callback.asExternalPaymentMethodCallback().onActivityResult(PaymentResult.Completed)

        countDownLatch.await(5, TimeUnit.SECONDS)

        assertThat(result).isInstanceOf<ConfirmationHandler.Result.Succeeded>()

        val successResult = result.asSucceeded()

        assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
    }

    private fun createExternalPaymentMethodConfirmationMediator(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = ConfirmationMediator(
        definition = ExternalPaymentMethodConfirmationDefinition(FakeErrorReporter()),
        savedStateHandle = savedStateHandle
    )

    private companion object {
        private val EPM_CONFIRMATION_OPTION = ExternalPaymentMethodConfirmationOption(
            type = "paypal",
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
                address = Address(
                    line1 = "123 Apple Street",
                    city = "South San Francisco",
                    state = "CA",
                    country = "US",
                ),
            )
        )

        private val PAYMENT_INTENT = PaymentIntentFactory.create()
    }
}
