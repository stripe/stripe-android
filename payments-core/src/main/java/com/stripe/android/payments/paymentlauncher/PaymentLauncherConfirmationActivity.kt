package com.stripe.android.payments.paymentlauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.view.AuthActivityStarterHost

/**
 * Host activity to perform actions for [PaymentLauncher].
 * This activity starts activities to handle next actions, capture their result
 * and convert them to [PaymentResult] and return back to client.
 */
internal class PaymentLauncherConfirmationActivity : AppCompatActivity() {
    private val starterArgs: PaymentLauncherContract.Args? by lazy {
        PaymentLauncherContract.Args.fromIntent(intent)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentLauncherViewModel.Factory(
            { requireNotNull(starterArgs) },
            { application },
            this
        )

    @VisibleForTesting
    internal val viewModel: PaymentLauncherViewModel by viewModels { viewModelFactory }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        disableAnimations()

        val args = runCatching {
            requireNotNull(starterArgs) {
                EMPTY_ARG_ERROR
            }
        }.getOrElse {
            finishWithResult(PaymentResult.Failed(it))
            return
        }

        args.statusBarColor?.let {
            window.statusBarColor = it
        }

        viewModel.paymentLauncherResult.observe(this, ::finishWithResult)
        viewModel.register(this)
        val host = AuthActivityStarterHost.create(this)
        when (args) {
            is PaymentLauncherContract.Args.IntentConfirmationArgs -> {
                viewModel.confirmStripeIntent(args.confirmStripeIntentParams, host)
            }
            is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> {
                viewModel.handleNextActionForStripeIntent(args.paymentIntentClientSecret, host)
            }
            is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> {
                viewModel.handleNextActionForStripeIntent(args.setupIntentClientSecret, host)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanUp()
    }

    override fun finish() {
        super.finish()
        disableAnimations()
    }

    private fun disableAnimations() {
        // this is a transparent Activity so we want to disable animations
        overridePendingTransition(0, 0)
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

    companion object {
        const val EMPTY_ARG_ERROR =
            "PaymentLauncherConfirmationActivity was started without arguments"
    }
}
