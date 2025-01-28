package com.stripe.android.paymentelement.confirmation.bacs

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertCanceled
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class BacsConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @Test
    fun `On Bacs confirmation option provided, should launch Bacs mandate flow then intent flow`() = test {
        intendingBacsToBeLaunched(
            BacsMandateConfirmationResult.Confirmed
        )
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedBacsToBeLaunched()

            val confirmingWithSavedOptionWithCvc = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOptionWithCvc.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.New(
                        createParams = CONFIRMATION_OPTION.createParams,
                        optionsParams = CONFIRMATION_OPTION.optionsParams,
                        shouldSave = false,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On Bacs confirmation option provided, should launch Bacs mandate flow then cancel`() = test {
        intendingBacsToBeLaunched(
            BacsMandateConfirmationResult.Cancelled
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedBacsToBeLaunched()

            val cancelResult = awaitItem().assertComplete().result.assertCanceled()

            assertThat(cancelResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
        }
    }

    @Test
    fun `On Bacs confirmation option provided, should launch Bacs flow then cancel with modify action`() = test {
        intendingBacsToBeLaunched(
            BacsMandateConfirmationResult.ModifyDetails
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedBacsToBeLaunched()

            val cancelResult = awaitItem().assertComplete().result.assertCanceled()

            assertThat(cancelResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails)
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

    private fun intendingBacsToBeLaunched(result: BacsMandateConfirmationResult) {
        intending(hasComponent(BACS_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(ActivityStarter.Result.EXTRA, result)
            )
        )
    }

    private fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    private fun intendedBacsToBeLaunched() {
        intended(hasComponent(BACS_ACTIVITY_NAME))
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
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

        val PAYMENT_METHOD = PaymentMethodFactory.bacs()

        val CONFIRMATION_OPTION = BacsConfirmationOption(
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "1234567",
                    sortCode = "000000"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    email = "email@email.com"
                )
            ),
            optionsParams = null,
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

        const val BACS_ACTIVITY_NAME =
            "com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
