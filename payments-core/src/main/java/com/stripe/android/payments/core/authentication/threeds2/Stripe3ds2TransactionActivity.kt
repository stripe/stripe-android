package com.stripe.android.payments.core.authentication.threeds2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.StripeException
import com.stripe.android.payments.PaymentFlowResult

/**
 * Work in progress!
 *
 * A transparent [Activity] that will initiate a 3DS2 transaction by making the authentication
 * request (AReq) and handling the response (ARes). Depending on the response,
 * [Stripe3ds2TransactionActivity] might start the challenge flow UI, complete using the
 * frictionless flow, fall back to a web URL, or finish early if there is a failure.
 */
internal class Stripe3ds2TransactionActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = runCatching {
            requireNotNull(
                Stripe3ds2TransactionContract.Args.fromIntent(intent)
            ) {
                "Error while attempting to initiate 3DS2 transaction."
            }
        }.getOrElse {
            finishWithResult(
                PaymentFlowResult.Unvalidated(
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = StripeException.create(it)
                )
            )
            return
        }

        val viewModel by viewModels<Stripe3ds2TransactionViewModel> {
            Stripe3ds2TransactionViewModelFactory()
        }
    }

    private fun finishWithResult(
        paymentFlowResult: PaymentFlowResult.Unvalidated
    ) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(paymentFlowResult.toBundle())
        )
        finish()
    }
}
