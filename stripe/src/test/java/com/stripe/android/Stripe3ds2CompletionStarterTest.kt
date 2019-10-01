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
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2CompletionStarterTest {

    private lateinit var starter: Stripe3ds2CompletionStarter

    @Mock
    private lateinit var activity: Activity

    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        intentArgumentCaptor = argumentCaptor()
        starter = Stripe3ds2CompletionStarter(
            AuthActivityStarter.Host.create(activity), 500)
    }

    @Test
    fun start_withSuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.StartData(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_SUCCESSFUL))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertEquals(StripeIntentResult.Outcome.SUCCEEDED,
            intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                StripeIntentResult.Outcome.UNKNOWN))
    }

    @Test
    fun start_withUnsuccessfulCompletion_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.StartData(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertEquals(StripeIntentResult.Outcome.FAILED,
            intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                StripeIntentResult.Outcome.UNKNOWN))
    }

    @Test
    fun start_withTimeout_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.StartData(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.TIMEOUT))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertEquals(StripeIntentResult.Outcome.TIMEDOUT,
            intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                StripeIntentResult.Outcome.UNKNOWN))
    }

    @Test
    fun start_withProtocolError_shouldAddClientSecretAndOutcomeToIntent() {
        starter.start(Stripe3ds2CompletionStarter.StartData(
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR))
        verify<Activity>(activity).startActivityForResult(intentArgumentCaptor.capture(), eq(500))
        val intent = intentArgumentCaptor.firstValue
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret,
            intent.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET))
        assertEquals(StripeIntentResult.Outcome.FAILED,
            intent.getIntExtra(StripeIntentResultExtras.FLOW_OUTCOME,
                StripeIntentResult.Outcome.UNKNOWN))
    }
}
