package com.stripe.android

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.exception.APIException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.PermissionException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.SourceFixtures
import com.stripe.android.utils.ParcelUtils.verifyParcelRoundtrip
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            result.clientSecret
        )
        assertEquals(
            StripeIntentResult.Outcome.UNKNOWN,
            result.flowOutcome
        )
        assertNull(result.exception)
    }

    @Test
    fun start_withException_shouldSetCorrectIntentExtras() {
        val exception = APIException(RuntimeException())
        starter.start(PaymentRelayStarter.Args.create(exception))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertNull(result.clientSecret)
        assertEquals(
            StripeIntentResult.Outcome.UNKNOWN,
            result.flowOutcome
        )
        assertEquals(exception, result.exception)
    }

    @Test
    fun start_withStripeException_shouldSetCorrectIntentExtras() {
        val exception = PermissionException(
            stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR
        )
        starter.start(PaymentRelayStarter.Args.create(exception))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertNull(result.clientSecret)
        assertEquals(
            StripeIntentResult.Outcome.UNKNOWN,
            result.flowOutcome
        )

        assertEquals(exception, result.exception)
    }

    @Test
    fun testParcel_withPaymentIntent() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                source = SourceFixtures.CARD,
                exception = InvalidRequestException(
                    stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                    cause = IllegalArgumentException()
                )
            )
        )
    }

    @Test
    fun testParcel_withSetupIntent() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args(
                stripeIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                source = SourceFixtures.CARD,
                exception = InvalidRequestException(
                    stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                    cause = IllegalArgumentException()
                )
            )
        )
    }

    @Test
    fun testParcel_withoutStripeIntent() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args(
                stripeIntent = null,
                source = SourceFixtures.CARD,
                exception = InvalidRequestException(
                    stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                    cause = IllegalArgumentException()
                )
            )
        )
    }

    @Test
    fun testParcel_withStripeIntentwithoutSource() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args(
                stripeIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                exception = InvalidRequestException(
                    stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                    cause = IllegalArgumentException()
                )
            )
        )
    }

    private val result: PaymentController.Result
        get() {
            return requireNotNull(
                PaymentController.Result.fromIntent(intentArgumentCaptor.firstValue)
            )
        }
}
