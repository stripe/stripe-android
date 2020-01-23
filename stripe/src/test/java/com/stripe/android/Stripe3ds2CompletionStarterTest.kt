package com.stripe.android

import android.app.Activity
import android.content.Intent
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.view.AuthActivityStarter
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2CompletionStarterTest {
    private val activity: Activity = mock()
    private val intentArgumentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
    private val starter: Stripe3ds2CompletionStarter =
        Stripe3ds2CompletionStarter(AuthActivityStarter.Host.create(activity), 500)

    @Test
    fun start_withSuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.Args(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_SUCCESSFUL))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            result.clientSecret
        )
        assertEquals(
            StripeIntentResult.Outcome.SUCCEEDED,
            result.flowOutcome
        )
    }

    @Test
    fun start_withUnsuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.Args(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            result.clientSecret
        )
        assertEquals(
            StripeIntentResult.Outcome.FAILED,
            result.flowOutcome
        )
    }

    @Test
    fun start_withTimeout_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.Args(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.TIMEOUT))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            result.clientSecret
        )
        assertEquals(
            StripeIntentResult.Outcome.TIMEDOUT,
            result.flowOutcome
        )
    }

    @Test
    fun start_withProtocolError_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.Args(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR))
        verify(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))

        assertEquals(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            result.clientSecret
        )
        assertEquals(
            StripeIntentResult.Outcome.FAILED,
            result.flowOutcome
        )
    }

    private val result: PaymentController.Result
        get() {
            return requireNotNull(
                PaymentController.Result.fromIntent(intentArgumentCaptor.firstValue)
            )
        }
}
