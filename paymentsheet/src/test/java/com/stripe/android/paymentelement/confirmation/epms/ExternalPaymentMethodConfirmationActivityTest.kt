package com.stripe.android.paymentelement.confirmation.epms

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.assertCanceled
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertFailed
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.paymentsheet.ExternalPaymentMethodResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class ExternalPaymentMethodConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @Before
    fun setup() {
        PaymentElementCallbackReferences["Confirmation"] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = { _, _ ->
                error("Should not be called!")
            }
        )
    }

    @After
    fun teardown() {
        PaymentElementCallbackReferences.remove("Confirmation")
    }

    @Test
    fun `On EPM confirmation option provided, should launch EPM flow and succeed`() = test {
        intendingEpmToBeLaunched(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent(),
            )
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedEpmToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On EPM confirmation option provided, should launch EPM flow and fail`() = test {
        val failureMessage = "EPM failure"

        intendingEpmToBeLaunched(
            Instrumentation.ActivityResult(
                Activity.RESULT_FIRST_USER,
                Intent().putExtra(
                    ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA,
                    failureMessage,
                )
            )
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedEpmToBeLaunched()

            val failedResult = awaitItem().assertComplete().result.assertFailed()

            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod)
            assertThat(failedResult.message).isEqualTo(failureMessage.resolvableString)

            val cause = failedResult.cause

            assertThat(cause).isInstanceOf<LocalStripeException>()

            val localStripeException = cause.asLocalStripeException()

            assertThat(localStripeException.displayMessage).isEqualTo(failureMessage)
            assertThat(localStripeException.analyticsValue).isEqualTo("externalPaymentMethodFailure")
        }
    }

    @Test
    fun `On EPM confirmation option provided, should launch EPM flow and cancel`() = test {
        intendingEpmToBeLaunched(
            Instrumentation.ActivityResult(
                Activity.RESULT_CANCELED,
                Intent(),
            )
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedEpmToBeLaunched()

            val cancelResult = awaitItem().assertComplete().result.assertCanceled()

            assertThat(cancelResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
        }
    }

    private fun test(
        test: suspend PaymentElementConfirmationTestActivity.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val countDownLatch = CountDownLatch(1)

        ActivityScenario.launch<PaymentElementConfirmationTestActivity>(
            Intent(application, PaymentElementConfirmationTestActivity::class.java)
        ).use { scenario ->
            scenario.onActivity { activity ->
                launch {
                    test(activity)
                    countDownLatch.countDown()
                }
            }

            countDownLatch.await(10, TimeUnit.SECONDS)
        }
    }

    private fun intendingEpmToBeLaunched(result: Instrumentation.ActivityResult) {
        intending(hasComponent(EPM_ACTIVITY_NAME)).respondWith(result)
    }

    private fun intendedEpmToBeLaunched() {
        intended(hasComponent(EPM_ACTIVITY_NAME))
    }

    private fun Throwable.asLocalStripeException(): LocalStripeException {
        return this as LocalStripeException
    }

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFactory.create().copy(
            id = "pm_1",
            amount = 5000,
            currency = "CAD",
            paymentMethodOptionsJsonString = """
                {"card": {"require_cvc_recollection": true}}
            """.trimIndent()
        )

        val CONFIRMATION_OPTION = ExternalPaymentMethodConfirmationOption(
            type = "paypal",
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
            ),
        )

        val CONFIRMATION_ARGUMENTS = ConfirmationHandler.Args(
            confirmationOption = CONFIRMATION_OPTION,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = AddressDetails(),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
        )

        const val EPM_ACTIVITY_NAME =
            "com.stripe.android.paymentsheet.ExternalPaymentMethodProxyActivity"
    }
}
