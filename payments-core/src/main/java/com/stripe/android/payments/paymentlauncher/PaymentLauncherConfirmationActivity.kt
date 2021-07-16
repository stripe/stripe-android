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
internal class PaymentLauncherConfirmationActivity : AppCompatActivity() {

    private val viewModel: PaymentLauncherViewModel by viewModels {
        PaymentLauncherViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = kotlin.runCatching {
            requireNotNull(PaymentLauncherContract.Args.fromIntent(intent))
        }.getOrElse {
            finishWithResult(PaymentResult.Failed(it))
            return
        }

        when (args) {
            is PaymentLauncherContract.Args.IntentConfirmationArgs -> {
                viewModel.confirmStripeIntent(args.confirmStripeIntentParams)
            }
            is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> {
                viewModel.handleNextActionForPaymentIntent(args.paymentIntentClientSecret)
            }
            is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> {
                viewModel.handleNextActionForSetupIntent(args.setupIntentClientSecret)
            }
        }
    }

    /**
     * After confirmation and next action is handled, finish the activity with
     * corresponding [PaymentResult]
     */
    private fun finishWithResult(result: PaymentResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(result.toBundle())
        )
        finish()
    }
}
