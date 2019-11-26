package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripePaymentAuthTest {

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var paymentController: PaymentController
    @Mock
    private lateinit var paymentCallback: ApiResultCallback<PaymentIntentResult>
    @Mock
    private lateinit var setupCallback: ApiResultCallback<SetupIntentResult>

    private lateinit var hostArgumentCaptor: KArgumentCaptor<AuthActivityStarter.Host>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
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
        verify(paymentController).startConfirmAndAuth(
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
        verify(paymentController).startConfirmAndAuth(
            hostArgumentCaptor.capture(),
            eq(confirmSetupIntentParams),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun authenticatePayment_shouldAuth() {
        val stripe = createStripe()
        val clientSecret =
            requireNotNull(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret)
        stripe.handleNextActionForPayment(activity, clientSecret)
        verify(paymentController).startAuth(
            hostArgumentCaptor.capture(),
            eq(clientSecret),
            eq(REQUEST_OPTIONS)
        )
        assertEquals(activity, hostArgumentCaptor.firstValue.activity)
    }

    @Test
    fun handleNextActionForSetupIntent_shouldStartAuth() {
        val stripe = createStripe()
        val clientSecret =
            requireNotNull(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.clientSecret)
        stripe.handleNextActionForSetupIntent(activity, clientSecret)
        verify(paymentController).startAuth(
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
            StripePaymentController.PAYMENT_REQUEST_CODE, data))
            .thenReturn(true)
        val stripe = createStripe()
        stripe.onPaymentResult(StripePaymentController.PAYMENT_REQUEST_CODE, data, paymentCallback)

        verify(paymentController).handlePaymentResult(data,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            paymentCallback)
    }

    @Test
    fun onSetupResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        val data = Intent()
        `when`(paymentController.shouldHandleSetupResult(
            StripePaymentController.SETUP_REQUEST_CODE, data))
            .thenReturn(true)
        val stripe = createStripe()
        stripe.onSetupResult(StripePaymentController.SETUP_REQUEST_CODE, data, setupCallback)

        verify(paymentController).handleSetupResult(data,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
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

    private companion object {
        private val REQUEST_OPTIONS =
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }
}
