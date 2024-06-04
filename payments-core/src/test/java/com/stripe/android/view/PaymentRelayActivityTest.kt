package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripeErrorFixtures
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.PaymentFlowResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentRelayActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = PaymentRelayContract()

    @Test
    fun `activity started with PaymentIntentArgs should finish with expected result`() {
        createActivity(
            PaymentRelayStarter.Args.PaymentIntentArgs(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            )
        ) { paymentFlowResult ->
            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = "pi_1ExkUeAWhjPjYwPiXph9ouXa_secret_nGTdfGlzL9Uop59wN55LraiC7"
                    )
                )
        }
    }

    @Test
    fun `activity started with ErrorArgs should finish with expected result`() {
        val exception = PermissionException(
            stripeError = StripeErrorFixtures.INVALID_REQUEST_ERROR
        )
        createActivity(
            PaymentRelayStarter.Args.ErrorArgs(
                exception,
                requestCode = 50000
            )
        ) { paymentFlowResult ->
            assertThat(paymentFlowResult)
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        exception = exception
                    )
                )
        }
    }

    private fun createActivity(
        args: PaymentRelayStarter.Args,
        onComplete: (PaymentFlowResult.Unvalidated) -> Unit
    ) {
        launchActivityForResult<PaymentRelayActivity>(
            contract.createIntent(
                context,
                args
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                onComplete(
                    contract.parseResult(0, activityScenario.result.resultData)
                )
            }
        }
    }
}
