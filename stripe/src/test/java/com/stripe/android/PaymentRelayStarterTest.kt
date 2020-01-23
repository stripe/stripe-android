package com.stripe.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.exception.PermissionException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.utils.ParcelUtils
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.StripeIntentResultExtras
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentRelayStarterTest {
    private val activity: Activity = mock()
    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val starter: PaymentRelayStarter = PaymentRelayStarter.create(
        AuthActivityStarter.Host.create(activity),
        500
    )

    @Test
    fun start_withPaymentIntent_shouldSetCorrectIntentExtras() {
        starter.start(
            PaymentRelayStarter.Args.create(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
        )
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val bundle = ParcelUtils.copy(
            intentArgumentCaptor.firstValue.extras ?: Bundle(),
            Bundle.CREATOR
        )
        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            bundle.getString(StripeIntentResultExtras.CLIENT_SECRET)
        )
        assertFalse(bundle.containsKey(StripeIntentResultExtras.FLOW_OUTCOME))
        assertNull(bundle.getSerializable(StripeIntentResultExtras.AUTH_EXCEPTION))
    }

    @Test
    fun start_withException_shouldSetCorrectIntentExtras() {
        val exception = RuntimeException()
        starter.start(PaymentRelayStarter.Args.create(exception))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val bundle = ParcelUtils.copy(
            intentArgumentCaptor.firstValue.extras ?: Bundle(),
            Bundle.CREATOR
        )
        assertNull(bundle.getString(StripeIntentResultExtras.CLIENT_SECRET))
        assertFalse(bundle.containsKey(StripeIntentResultExtras.FLOW_OUTCOME))
        assertTrue(
            bundle.getSerializable(StripeIntentResultExtras.AUTH_EXCEPTION) is RuntimeException
        )
    }

    @Test
    fun start_withStripeException_shouldSetCorrectIntentExtras() {
        val exception = PermissionException(
            stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR
        )
        starter.start(PaymentRelayStarter.Args.create(exception))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val bundle = ParcelUtils.copy(
            intentArgumentCaptor.firstValue.extras ?: Bundle(),
            Bundle.CREATOR
        )
        assertNull(bundle.getString(StripeIntentResultExtras.CLIENT_SECRET))
        assertFalse(bundle.containsKey(StripeIntentResultExtras.FLOW_OUTCOME))

        val expectedException =
            bundle.getSerializable(StripeIntentResultExtras.AUTH_EXCEPTION) as StripeException
        assertEquals(
            exception.stripeError,
            expectedException.stripeError
        )
    }
}
