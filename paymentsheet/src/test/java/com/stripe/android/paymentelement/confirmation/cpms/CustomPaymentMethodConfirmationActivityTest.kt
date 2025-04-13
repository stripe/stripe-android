package com.stripe.android.paymentelement.confirmation.cpms

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
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CustomPaymentMethodConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @get:Rule
    val paymentElementCallbackTestRule = PaymentElementCallbackTestRule()

    @Before
    fun setup() {
        PaymentElementCallbackReferences["ConfirmationTestIdentifier"] = PaymentElementCallbacks.Builder()
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("Should not be called!")
            }
            .build()
    }

    @Test
    fun `On CPM confirmation option provided, should launch CPM flow and succeed`() = test {
        intendingCpmToBeLaunched(InternalCustomPaymentMethodResult.Completed)

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedCpmToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On CPM confirmation option provided, should launch CPM flow and fail`() = test {
        val exception = LocalStripeException(
            displayMessage = "Failed CPM!",
            analyticsValue = "customPaymentMethodFailure"
        )

        intendingCpmToBeLaunched(InternalCustomPaymentMethodResult.Failed(exception))

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedCpmToBeLaunched()

            val failedResult = awaitItem().assertComplete().result.assertFailed()

            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
            assertThat(failedResult.message).isEqualTo("Failed CPM!".resolvableString)
            assertThat(failedResult.cause).isEqualTo(exception)
        }
    }

    @Test
    fun `On CPM confirmation option provided, should launch CPM flow and cancel`() = test {
        intendingCpmToBeLaunched(InternalCustomPaymentMethodResult.Canceled)

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(CONFIRMATION_OPTION)

            intendedCpmToBeLaunched()

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

    private fun intendingCpmToBeLaunched(result: InternalCustomPaymentMethodResult) {
        intending(hasComponent(CPM_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(result.toBundle())
            )
        )
    }

    private fun intendedCpmToBeLaunched() {
        intended(hasComponent(CPM_ACTIVITY_NAME))
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

        val CONFIRMATION_OPTION = CustomPaymentMethodConfirmationOption(
            customPaymentMethodType = PaymentSheet.CustomPaymentMethod(
                id = "cpmt_123",
                subtitle = "Pay now".resolvableString,
                disableBillingDetailCollection = false,
            ),
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

        const val CPM_ACTIVITY_NAME =
            "com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodProxyActivity"
    }
}
