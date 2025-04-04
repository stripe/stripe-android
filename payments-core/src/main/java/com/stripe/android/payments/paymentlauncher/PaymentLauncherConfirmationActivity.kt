package com.stripe.android.payments.paymentlauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.R
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.uicore.utils.fadeOut
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.launch

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
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentLauncherViewModel.Factory {
        requireNotNull(starterArgs)
    }

    @VisibleForTesting
    internal val viewModel: PaymentLauncherViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = resources.getString(R.string.stripe_confirming_transaction_status)

        val args = runCatching {
            requireNotNull(starterArgs) {
                EMPTY_ARG_ERROR
            }
        }.getOrElse {
            finishWithResult(InternalPaymentResult.Failed(it))
            ErrorReporter.createFallbackInstance(applicationContext)
                .report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.PAYMENT_LAUNCHER_CONFIRMATION_NULL_ARGS,
                    stripeException = StripeException.create(it),
                )
            return
        }

        onBackPressedDispatcher.addCallback {
            // Prevent back presses while confirming payment
        }

        lifecycleScope.launch {
            viewModel.internalPaymentResult.collect {
                it?.let(::finishWithResult)
            }
        }

        viewModel.register(
            activityResultCaller = this,
            lifecycleOwner = this,
        )

        val host = AuthActivityStarterHost.create(
            activity = this,
            statusBarColor = args.statusBarColor,
        )

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

    override fun finish() {
        super.finish()
        fadeOut()
    }

    /**
     * After confirmation and next action is handled, finish the activity with
     * corresponding [PaymentResult]
     */
    private fun finishWithResult(result: InternalPaymentResult) {
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
