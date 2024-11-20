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
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExternalPaymentMethodConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val mediator = createExternalPaymentMethodConfirmationMediator(savedStateHandle)

        DummyActivityResultCaller.test {
            mediator.register(
                activityResultCaller = activityResultCaller,
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

            assertThat(awaitRegisterCall()).isNotNull()
            assertThat(awaitLaunchCall()).isNotNull()
        }
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

        DummyActivityResultCaller.test {
            val mediator = createExternalPaymentMethodConfirmationMediator(savedStateHandle)
            var result: ConfirmationHandler.Result? = null

            mediator.register(
                activityResultCaller = activityResultCaller,
                onResult = {
                    result = it
                }
            )

            val call = awaitRegisterCall()

            call.callback.asExternalPaymentMethodCallback().onActivityResult(PaymentResult.Completed)

            countDownLatch.await(5, TimeUnit.SECONDS)

            assertThat(result).isInstanceOf<ConfirmationHandler.Result.Succeeded>()

            val successResult = result.asSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
        }
    }

    private fun createExternalPaymentMethodConfirmationMediator(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = ConfirmationMediator(
        definition = ExternalPaymentMethodConfirmationDefinition(
            externalPaymentMethodConfirmHandlerProvider = {
                ExternalPaymentMethodConfirmHandler { _, _ ->
                    error("Not implemented!")
                }
            },
            errorReporter = FakeErrorReporter()
        ),
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
