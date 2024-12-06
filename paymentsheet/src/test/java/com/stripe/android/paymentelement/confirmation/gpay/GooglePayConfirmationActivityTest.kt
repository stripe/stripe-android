package com.stripe.android.paymentelement.confirmation.gpay

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
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertCanceled
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertFailed
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.stripe.android.R as PaymentsCoreR

@RunWith(RobolectricTestRunner::class)
internal class GooglePayConfirmationActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @Test
    fun googlePaySucceeds() = test {
        val paymentMethod = PaymentMethodFactory.card()

        intendingGooglePayToBeLaunched(GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod))
        intendingPaymentConfirmationToBeLaunched(
            InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                )
            )

            val confirmingWithGooglePay = awaitItem().assertConfirming()

            assertThat(confirmingWithGooglePay.option).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)

            intendedGooglePayToBeLaunched()

            val confirmingWithSavedPaymentMethod = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedPaymentMethod.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        initializationMode = GOOGLE_PAY_CONFIRMATION_OPTION.initializationMode,
                        shippingDetails = GOOGLE_PAY_CONFIRMATION_OPTION.shippingDetails,
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    @Test
    fun googlePayFailsOnGooglePaySheetFailure() = test {
        confirmationHandler.state.test {
            awaitItem().assertIdle()

            intendingGooglePayToBeLaunched(
                GooglePayPaymentMethodLauncher.Result.Failed(
                    error = IllegalStateException("An error occurred!"),
                    errorCode = GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                )
            )

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                )
            )

            val confirmingWithGooglePay = awaitItem().assertConfirming()

            assertThat(confirmingWithGooglePay.option).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)

            intendedGooglePayToBeLaunched()

            val failedResult = awaitItem().assertComplete().result.assertFailed()

            assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedResult.message).isEqualTo(PaymentsCoreR.string.stripe_internal_error.resolvableString)
            assertThat(failedResult.type).isEqualTo(
                ConfirmationHandler.Result.Failed.ErrorType.GooglePay(GooglePayPaymentMethodLauncher.INTERNAL_ERROR)
            )
        }
    }

    @Test
    fun googlePayFailsOnPaymentFailure() = test {
        confirmationHandler.state.test {
            awaitItem().assertIdle()

            val paymentMethod = PaymentMethodFactory.card()

            intendingGooglePayToBeLaunched(GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod))
            intendingPaymentConfirmationToBeLaunched(
                InternalPaymentResult.Failed(
                    throwable = IllegalStateException("Failed payment!")
                )
            )

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                )
            )

            val confirmingWithGooglePay = awaitItem().assertConfirming()

            assertThat(confirmingWithGooglePay.option).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)

            intendedGooglePayToBeLaunched()

            val confirmingWithSavedPaymentMethod = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedPaymentMethod.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        initializationMode = GOOGLE_PAY_CONFIRMATION_OPTION.initializationMode,
                        shippingDetails = GOOGLE_PAY_CONFIRMATION_OPTION.shippingDetails,
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val failedResult = awaitItem().assertComplete().result.assertFailed()

            assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun googlePayCancelsOnGooglePaySheetCancellation() = test {
        confirmationHandler.state.test {
            awaitItem().assertIdle()

            intendingGooglePayToBeLaunched(GooglePayPaymentMethodLauncher.Result.Canceled)

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                )
            )

            val confirmingWithGooglePay = awaitItem().assertConfirming()

            assertThat(confirmingWithGooglePay.option).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)

            intendedGooglePayToBeLaunched()

            val canceledAction = awaitItem().assertComplete().result.assertCanceled()

            assertThat(canceledAction.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
        }
    }

    @Test
    fun googlePayCancelsOnPaymentScreenCancellation() = test {
        confirmationHandler.state.test {
            awaitItem().assertIdle()

            val paymentMethod = PaymentMethodFactory.card()

            intendingGooglePayToBeLaunched(GooglePayPaymentMethodLauncher.Result.Completed(paymentMethod))
            intendingPaymentConfirmationToBeLaunched(InternalPaymentResult.Canceled)

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                )
            )

            val confirmingWithGooglePay = awaitItem().assertConfirming()

            assertThat(confirmingWithGooglePay.option).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)

            intendedGooglePayToBeLaunched()

            val confirmingWithSavedPaymentMethod = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedPaymentMethod.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        initializationMode = GOOGLE_PAY_CONFIRMATION_OPTION.initializationMode,
                        shippingDetails = GOOGLE_PAY_CONFIRMATION_OPTION.shippingDetails,
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val canceledAction = awaitItem().assertComplete().result.assertCanceled()

            assertThat(canceledAction.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
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

    private fun intendingGooglePayToBeLaunched(result: GooglePayPaymentMethodLauncher.Result) {
        intending(hasComponent(GOOGLE_PAY_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra("extra_result", result)
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

    private fun intendedGooglePayToBeLaunched() {
        intended(hasComponent(GOOGLE_PAY_ACTIVITY_NAME))
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
    }

    private companion object {
        val PAYMENT_INTENT = PaymentIntentFactory.create().copy(
            id = "pm_1",
            amount = 5000,
            currency = "CAD",
        )

        val GOOGLE_PAY_CONFIRMATION_OPTION = GooglePayConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = null,
            config = GooglePayConfirmationOption.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                merchantName = "Test merchant Inc.",
                merchantCurrencyCode = null,
                customAmount = null,
                merchantCountryCode = "CA",
                customLabel = "Test merchant Inc.",
                billingDetailsCollectionConfiguration = PaymentSheet
                    .BillingDetailsCollectionConfiguration(),
                cardBrandFilter = DefaultCardBrandFilter,
            )
        )

        const val GOOGLE_PAY_ACTIVITY_NAME =
            "com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
