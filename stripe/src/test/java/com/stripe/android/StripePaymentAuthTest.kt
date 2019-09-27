package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripePaymentAuthTest {

    private lateinit var context: Context

    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var paymentController: PaymentController
    @Mock
    private lateinit var paymentCallback: ApiResultCallback<PaymentIntentResult>
    @Mock
    private lateinit var setupCallback: ApiResultCallback<SetupIntentResult>

    private lateinit var hostArgumentCaptor: KArgumentCaptor<AuthActivityStarter.Host>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        hostArgumentCaptor = argumentCaptor()
    }

    @Test
    fun confirmPayment_shouldConfirmAndAuth() {
        val stripe = createStripe()
        val confirmPaymentIntentParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            "pm_card_threeDSecure2Required",
            "client_secret",
            "yourapp://post-authentication-return-url")
        stripe.confirmPayment(activity, confirmPaymentIntentParams)
        verify<PaymentController>(paymentController).startConfirmAndAuth(
            hostArgumentCaptor.capture(),
            eq(confirmPaymentIntentParams),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun confirmSetupIntent_shouldConfirmAndAuth() {
        val stripe = createStripe()
        val confirmSetupIntentParams = ConfirmSetupIntentParams.create(
            "pm_card_threeDSecure2Required",
            "client_secret",
            "yourapp://post-authentication-return-url")
        stripe.confirmSetupIntent(activity, confirmSetupIntentParams)
        verify<PaymentController>(paymentController).startConfirmAndAuth(
            hostArgumentCaptor.capture(),
            eq(confirmSetupIntentParams),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun authenticatePayment_shouldAuth() {
        val stripe = createStripe()
        val clientSecret = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret!!
        stripe.authenticatePayment(activity, clientSecret)
        verify<PaymentController>(paymentController).startAuth(
            hostArgumentCaptor.capture(),
            eq(clientSecret),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun authenticateSetup_shouldAuth() {
        val stripe = createStripe()
        val clientSecret = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret!!
        stripe.authenticateSetup(activity, clientSecret)
        verify<PaymentController>(paymentController).startAuth(
            hostArgumentCaptor.capture(),
            eq(clientSecret),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun onPaymentResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        val data = Intent()
        `when`(paymentController.shouldHandlePaymentResult(
            PaymentController.PAYMENT_REQUEST_CODE, data))
            .thenReturn(true)
        val stripe = createStripe()
        stripe.onPaymentResult(PaymentController.PAYMENT_REQUEST_CODE, data, paymentCallback)

        verify(paymentController).handlePaymentResult(data,
            ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            paymentCallback)
    }

    @Test
    fun onSetupResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        val data = Intent()
        `when`(paymentController.shouldHandleSetupResult(
            PaymentController.SETUP_REQUEST_CODE, data))
            .thenReturn(true)
        val stripe = createStripe()
        stripe.onSetupResult(PaymentController.SETUP_REQUEST_CODE, data, setupCallback!!)

        verify(paymentController).handleSetupResult(data,
            ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            setupCallback)
    }

    private fun createStripe(): Stripe {
        return Stripe(
            StripeApiRepository(
                context,
                null,
                stripeApiRequestExecutor = StripeApiRequestExecutor(),
                fireAndForgetRequestExecutor = FakeFireAndForgetRequestExecutor()
            ),
            StripeNetworkUtils(context),
            paymentController,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, null
        )
    }

    companion object {
        private val REQUEST_OPTIONS =
            ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }
}
