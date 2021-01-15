package com.stripe.android.paymentsheet

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.R
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetFlowControllerTest {
    private val googlePayLauncher = mock<StripeGooglePayLauncher>()
    private val paymentController = mock<PaymentController>()
    private val eventReporter = mock<EventReporter>()
    private val flowController: PaymentSheet.FlowController by lazy {
        createFlowController()
    }

    @Test
    fun `init should fire analytics event`() {
        createFlowController()
        verify(eventReporter).onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is null should be null`() {
        assertThat(flowController.getPaymentOption())
            .isNull()
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is not null should return expected value`() {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val flowController = createFlowController(
            paymentMethods = paymentMethods,
            defaultPaymentMethodId = paymentMethods.first().id
        )
        assertThat(flowController.getPaymentOption())
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = CardBrand.Visa.icon,
                    label = "Visa"
                )
            )
    }

    @Test
    fun `onPaymentOptionResult() with saved payment method selection result should return payment option`() {
        val paymentOption = flowController.onPaymentOptionResult(
            createPaymentOptionIntent(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )

        val expectedPaymentOption = PaymentOption(
            drawableResourceId = R.drawable.stripe_ic_visa,
            label = CardBrand.Visa.displayName
        )

        assertThat(paymentOption)
            .isEqualTo(expectedPaymentOption)
        assertThat(flowController.getPaymentOption())
            .isEqualTo(expectedPaymentOption)
    }

    @Test
    fun `onPaymentOptionResult() with cancelled result should return null`() {
        val paymentOption = flowController.onPaymentOptionResult(
            Intent().putExtras(
                PaymentOptionResult.Cancelled(null).toBundle()
            )
        )
        assertThat(paymentOption)
            .isNull()
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentController`() {
        verifyNoMoreInteractions(paymentController)
        flowController.confirmPayment(mock())
    }

    @Test
    fun `confirmPayment() with GooglePay should start StripeGooglePayLauncher`() {
        flowController.onPaymentOptionResult(
            createPaymentOptionIntent(PaymentSelection.GooglePay)
        )
        flowController.confirmPayment(mock())
        verify(googlePayLauncher).startForResult(
            StripeGooglePayContract.Args(
                environment = StripeGooglePayEnvironment.Test,
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                countryCode = "US",
                merchantName = "Widget Store"
            )
        )
    }

    @Test
    fun `isPaymentResult with Google Pay request code should return true`() {
        assertThat(
            flowController.isPaymentResult(
                StripeGooglePayLauncher.REQUEST_CODE,
                Intent()
            )
        ).isTrue()
    }

    @Test
    fun `onPaymentResult with Google Pay PaymentIntent result should invoke callback with Succeeded`() {
        val callback = mock<PaymentSheetResultCallback>()
        flowController.onPaymentResult(
            StripeGooglePayLauncher.REQUEST_CODE,
            Intent().putExtras(
                StripeGooglePayContract.Result.PaymentIntent(
                    paymentIntentResult = PAYMENT_INTENT_RESULT
                ).toBundle()
            ),
            callback = callback
        )
        verify(callback).onComplete(
            PaymentResult.Succeeded(PAYMENT_INTENT_RESULT.intent)
        )
        verify(eventReporter).onPaymentSuccess(PaymentSelection.GooglePay)
    }

    @Test
    fun `onPaymentResult with Google Pay Error result should invoke callback with Failed()`() {
        val callback = mock<PaymentSheetResultCallback>()
        flowController.onPaymentResult(
            StripeGooglePayLauncher.REQUEST_CODE,
            Intent().putExtras(
                StripeGooglePayContract.Result.Error(
                    exception = RuntimeException("Google Pay failed")
                ).toBundle()
            ),
            callback = callback
        )
        verify(callback).onComplete(
            argWhere { paymentResult ->
                (paymentResult as? PaymentResult.Failed)?.error?.message == "Google Pay failed"
            }
        )
        verify(eventReporter).onPaymentFailure(PaymentSelection.GooglePay)
    }

    private fun createFlowController(
        paymentMethods: List<PaymentMethod> = emptyList(),
        defaultPaymentMethodId: String? = null
    ): PaymentSheet.FlowController {
        return DefaultPaymentSheetFlowController(
            paymentController,
            eventReporter,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null,
            DefaultPaymentSheetFlowController.Args(
                "client_secret",
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ),
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethodTypes = listOf(PaymentMethod.Type.Card),
            paymentMethods = paymentMethods,
            googlePayLauncherFactory = { googlePayLauncher },
            sessionId = SessionId(),
            defaultPaymentMethodId = defaultPaymentMethodId
        )
    }

    private companion object {
        private fun createPaymentOptionIntent(
            paymentSelection: PaymentSelection
        ): Intent {
            return Intent()
                .putExtras(
                    PaymentOptionResult.Succeeded(paymentSelection).toBundle()
                )
        }

        private val PAYMENT_INTENT_RESULT = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        )
    }
}
