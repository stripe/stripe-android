package com.stripe.android.paymentelement.confirmation.cpms

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler.EXTRA_CUSTOM_PAYMENT_METHOD_RESULT
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CustomPaymentMethodProxyActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `On init, should call confirm callback with expected params`() {
        val countDownLatch = CountDownLatch(1)

        lateinit var receivedCustomPaymentMethod: PaymentSheet.CustomPaymentMethod
        lateinit var receivedBillingDetails: PaymentMethod.BillingDetails

        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .confirmCustomPaymentMethodCallback { customPaymentMethod, billingDetails ->
                receivedCustomPaymentMethod = customPaymentMethod
                receivedBillingDetails = billingDetails

                countDownLatch.countDown()
            }
            .build()

        ActivityScenario.launch<CustomPaymentMethodProxyActivity>(createIntent())

        countDownLatch.await(5, TimeUnit.SECONDS)

        assertThat(receivedCustomPaymentMethod).isEqualTo(CUSTOM_PAYMENT_METHOD)
        assertThat(receivedBillingDetails).isEqualTo(BILLING_DETAILS)
    }

    @Test
    fun `On complete result, should return internal complete result`() = testForResult(
        CustomPaymentMethodResult.completed()
    ) { result ->
        assertThat(result).isEqualTo(InternalCustomPaymentMethodResult.Completed)
    }

    @Test
    fun `On canceled result, should return internal canceled result`() = testForResult(
        CustomPaymentMethodResult.canceled()
    ) { result ->
        assertThat(result).isEqualTo(InternalCustomPaymentMethodResult.Canceled)
    }

    @Test
    fun `On failed result, should return internal failed result`() = testForResult(
        CustomPaymentMethodResult.failed(displayMessage = "Failed to get CPM!")
    ) { result ->
        assertThat(result).isInstanceOf<InternalCustomPaymentMethodResult.Failed>()

        val failedResult = result as InternalCustomPaymentMethodResult.Failed
        val throwable = failedResult.throwable

        assertThat(throwable).isInstanceOf<LocalStripeException>()

        val stripeException = throwable as LocalStripeException

        assertThat(stripeException.displayMessage).isEqualTo("Failed to get CPM!")
        assertThat(stripeException.analyticsValue).isEqualTo("customPaymentMethodFailure")
    }

    private fun testForResult(
        result: CustomPaymentMethodResult,
        test: (InternalCustomPaymentMethodResult) -> Unit,
    ) {
        val activityLaunched = CountDownLatch(1)

        val scenario = ActivityScenario.launchActivityForResult<CustomPaymentMethodProxyActivity>(
            Intent(context, CustomPaymentMethodProxyActivity::class.java).putExtras(
                bundleOf(EXTRA_CUSTOM_PAYMENT_METHOD_RESULT to result)
            )
        ).use { scenario ->
            scenario.onActivity {
                activityLaunched.countDown()
            }
        }

        activityLaunched.await(5, TimeUnit.SECONDS)

        val scenarioResult = scenario.result

        assertThat(scenarioResult.resultCode).isEqualTo(Activity.RESULT_OK)

        test(InternalCustomPaymentMethodResult.fromIntent(scenarioResult.resultData))
    }

    private fun createIntent() = CustomPaymentMethodContract().createIntent(
        context = context,
        input = CustomPaymentMethodInput(
            paymentElementCallbackIdentifier = PAYMENT_ELEMENT_CALLBACK_IDENTIFIER,
            type = CUSTOM_PAYMENT_METHOD,
            billingDetails = BILLING_DETAILS,
        ),
    )

    private companion object {
        const val PAYMENT_ELEMENT_CALLBACK_IDENTIFIER = "CustomPaymentMethodTestIdentifier"

        val CUSTOM_PAYMENT_METHOD = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now".resolvableString,
            disableBillingDetailCollection = false,
        )

        val BILLING_DETAILS = PaymentMethod.BillingDetails(
            name = "John Doe",
        )
    }
}
