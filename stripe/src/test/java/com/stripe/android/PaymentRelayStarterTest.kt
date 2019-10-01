package com.stripe.android

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.StripeIntentResultExtras
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentRelayStarterTest {
    @Mock
    private lateinit var activity: Activity

    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>

    private lateinit var starter: PaymentRelayStarter

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        intentArgumentCaptor = argumentCaptor()
        starter = PaymentRelayStarter(
            AuthActivityStarter.Host.create(activity), 500)
    }

    @Test
    fun start_withPaymentIntent_shouldSetCorrectIntentExtras() {
        starter.start(PaymentRelayStarter.Data.create(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertFalse(intent.hasExtra(StripeIntentResultExtras.FLOW_OUTCOME))
        assertNull(intent.getSerializableExtra(StripeIntentResultExtras.AUTH_EXCEPTION))
    }

    @Test
    fun start_withException_shouldSetCorrectIntentExtras() {
        val exception = RuntimeException()
        starter.start(PaymentRelayStarter.Data.create(exception))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertNull(intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertFalse(intent.hasExtra(StripeIntentResultExtras.FLOW_OUTCOME))
        assertEquals(exception,
            intent.getSerializableExtra(StripeIntentResultExtras.AUTH_EXCEPTION))
    }
}
