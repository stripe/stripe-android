package com.stripe.android

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.SourceFixtures
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.utils.ParcelUtils.verifyParcelRoundtrip
import com.stripe.android.view.AuthActivityStarterHost
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentRelayStarterTest {
    private val activity = mock<ComponentActivity>()
    private val intentArgumentCaptor = argumentCaptor<Intent>()
    private val starter = PaymentRelayStarter.Legacy(
        AuthActivityStarterHost.create(activity)
    )

    @Test
    fun start_withPaymentIntent_shouldSetCorrectIntentExtras() {
        starter.start(
            PaymentRelayStarter.Args.create(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2)
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(50000)
        )

        assertThat(result.clientSecret).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.clientSecret)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
        assertThat(result.exception).isNull()
    }

    @Test
    fun start_withException_shouldSetCorrectIntentExtras() {
        val exception = APIException(RuntimeException())
        starter.start(
            PaymentRelayStarter.Args.ErrorArgs(
                exception,
                50000
            )
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(50000)
        )

        assertThat(result.clientSecret).isNull()
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
        assertThat(result.exception).isEqualTo(exception)
    }

    @Test
    fun start_withStripeException_shouldSetCorrectIntentExtras() {
        val exception = PermissionException(
            stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR
        )
        starter.start(
            PaymentRelayStarter.Args.ErrorArgs(
                exception,
                50000
            )
        )
        verify(activity).startActivityForResult(
            intentArgumentCaptor.capture(),
            eq(50000)
        )

        assertThat(result.clientSecret).isNull()
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)

        assertThat(result.exception).isEqualTo(exception)
    }

    @Test
    fun `PaymentIntentArgs should parcelize successfully`() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args.PaymentIntentArgs(
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            )
        )
    }

    @Test
    fun `SetupIntentArgs should parcelize successfully`() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args.SetupIntentArgs(
                setupIntent = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            )
        )
    }

    @Test
    fun `SourceArgs should parcelize successfully`() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args.SourceArgs(
                source = SourceFixtures.SOURCE_CARD
            )
        )
    }

    @Test
    fun `ErrorArgs should parcelize successfully`() {
        verifyParcelRoundtrip(
            PaymentRelayStarter.Args.ErrorArgs(
                exception = InvalidRequestException(
                    stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR,
                    cause = IllegalArgumentException()
                ),
                requestCode = 50000
            )
        )
    }

    private val result: PaymentFlowResult.Unvalidated
        get() = PaymentFlowResult.Unvalidated.fromIntent(intentArgumentCaptor.firstValue)
}
