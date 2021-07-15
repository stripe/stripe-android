package com.stripe.android.payments.paymentlauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

/**
 * WIP - host activity to perform actions for PaymentLauncher.
 * This activity starts activities to handle next actions, capture their result
 * and convert them to [PaymentResult] and return back to client.
 */
internal class PaymentConfirmationActivity : AppCompatActivity() {

    val viewModel: PaymentLauncherViewModel by viewModels {
        PaymentLauncherViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args =
            requireNotNull(PaymentLauncherHostContract.Args.fromIntent(intent))

        require(args.isValid())

        args.confirmStripeIntentParams?.let {
            viewModel.confirmStripeIntent(it)
        }

        args.paymentIntentClientSecret?.let {
            viewModel.handleNextActionForPaymentIntent(it)
        }

        args.setupIntentClientSecret?.let {
            viewModel.handleNextActionForSetupIntent(it)
        }
    }

    /**
     * After confirmation and next action is handled, finish the activity with
     * corresponding [PaymentResult]
     */
    private fun finishWithResult() {
        // TODO: get the correct result
        val result = PaymentResult.Canceled
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(result.toBundle())
        )
        finish()
    }
}
