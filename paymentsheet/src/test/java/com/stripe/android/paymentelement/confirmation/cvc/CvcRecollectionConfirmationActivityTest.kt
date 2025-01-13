package com.stripe.android.paymentelement.confirmation.cvc

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
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ExtendedPaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult
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
internal class CvcRecollectionConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<ExtendedPaymentElementConfirmationTestActivity>()

    @Test
    fun `On CVC recollection enabled, should launch CVC flow then proceed with intent confirmation flow`() = test {
        intendingCvcRecollectionToBeLaunched(
            CvcRecollectionResult.Confirmed(cvc = "444")
        )
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS)

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option).isEqualTo(CONFIRMATION_OPTION)

            intendedCvcRecollectionToBeLaunched()

            val confirmingWithSavedOptionWithCvc = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOptionWithCvc.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        paymentMethod = PAYMENT_METHOD,
                        optionsParams = PaymentMethodOptionsParams.Card(cvc = "444"),
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = PAYMENT_METHOD))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun `On CVC recollection disabled, should skip CVC activity and launch into intent confirmation flow`() = test {
        val paymentIntent = PAYMENT_INTENT_WITHOUT_CVC_RECOLLECTION

        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(paymentIntent.copy(paymentMethod = PAYMENT_METHOD))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(CONFIRMATION_ARGUMENTS.copy(intent = paymentIntent))

            val confirmingWithSavedOption = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedOption.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        paymentMethod = PAYMENT_METHOD,
                        optionsParams = null,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(paymentIntent.copy(paymentMethod = PAYMENT_METHOD))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    private fun test(
        test: suspend ExtendedPaymentElementConfirmationTestActivity.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val countDownLatch = CountDownLatch(1)

        ActivityScenario.launch<ExtendedPaymentElementConfirmationTestActivity>(
            Intent(application, ExtendedPaymentElementConfirmationTestActivity::class.java)
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

    private fun intendingCvcRecollectionToBeLaunched(result: CvcRecollectionResult) {
        intending(hasComponent(CVC_RECOLLECTION_ACTIVITY_NAME)).respondWith(
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

    private fun intendedCvcRecollectionToBeLaunched() {
        intended(hasComponent(CVC_RECOLLECTION_ACTIVITY_NAME))
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

        val PAYMENT_INTENT_WITHOUT_CVC_RECOLLECTION = PaymentIntentFactory.create().copy(
            id = "pm_1",
            amount = 5000,
            currency = "CAD",
        )

        val PAYMENT_METHOD = PaymentMethodFactory.card(random = true)

        val CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PAYMENT_METHOD,
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

        const val CVC_RECOLLECTION_ACTIVITY_NAME =
            "com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
