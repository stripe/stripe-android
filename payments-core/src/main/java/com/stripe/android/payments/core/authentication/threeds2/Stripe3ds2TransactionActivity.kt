package com.stripe.android.payments.core.authentication.threeds2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.exception.StripeException
import com.stripe.android.databinding.Stripe3ds2TransactionLayoutBinding
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.transaction.ChallengeContract
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.InitChallengeResult
import com.stripe.android.stripe3ds2.views.ChallengeProgressFragmentFactory
import kotlinx.coroutines.launch

/**
 * A transparent [Activity] that will initiate a 3DS2 transaction by making the authentication
 * request (AReq) and handling the response (ARes). Depending on the response,
 * [Stripe3ds2TransactionActivity] might start the challenge flow UI, complete using the
 * frictionless flow, fall back to a web URL, or finish early if there is a failure.
 */
internal class Stripe3ds2TransactionActivity : AppCompatActivity() {

    private val viewBinding: Stripe3ds2TransactionLayoutBinding by lazy {
        Stripe3ds2TransactionLayoutBinding.inflate(layoutInflater)
    }

    lateinit var args: Stripe3ds2TransactionContract.Args

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = Stripe3ds2TransactionViewModelFactory { args }

    public override fun onCreate(savedInstanceState: Bundle?) {
        val argsResult = runCatching {
            val args = requireNotNull(
                Stripe3ds2TransactionContract.Args.fromIntent(intent)
            ) {
                "Error while attempting to initiate 3DS2 transaction."
            }

            val accentColor =
                args.config.uiCustomization.uiCustomization.accentColor?.let { accentColor ->
                    runCatching { accentColor.toColorInt() }.getOrNull()
                }
            supportFragmentManager.fragmentFactory = ChallengeProgressFragmentFactory(
                args.fingerprint.directoryServerName,
                args.sdkTransactionId,
                accentColor
            )

            args
        }

        super.onCreate(savedInstanceState)

        args = argsResult.getOrElse {
            finishWithResult(
                PaymentFlowResult.Unvalidated(
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = StripeException.create(it)
                )
            )
            return
        }

        setContentView(viewBinding.root)

        args.statusBarColor?.let {
            window.statusBarColor = it
        }

        val viewModel by viewModels<Stripe3ds2TransactionViewModel> { viewModelFactory }
        val onChallengeResult = { challengeResult: ChallengeResult ->
            lifecycleScope.launch {
                finishWithResult(
                    viewModel.processChallengeResult(challengeResult)
                )
            }
        }

        val challengeLauncher = registerForActivityResult(
            ChallengeContract()
        ) {
            onChallengeResult(it)
        }

        val browserLauncher = registerForActivityResult(
            PaymentBrowserAuthContract()
        ) {
            finishWithResult(it)
        }

        if (!viewModel.hasCompleted) {
            lifecycleScope.launchWhenResumed {
                if (!isFinishing) {
                    when (val nextStep = viewModel.start3ds2Flow()) {
                        is NextStep.StartChallenge -> {
                            // make the initial challenge request
                            when (
                                val initChallengeResult = viewModel.initChallenge(nextStep.args)
                            ) {
                                is InitChallengeResult.Start -> {
                                    challengeLauncher.launch(initChallengeResult.challengeViewArgs)
                                }
                                is InitChallengeResult.End -> {
                                    onChallengeResult(initChallengeResult.challengeResult)
                                }
                            }
                        }
                        is NextStep.StartFallback -> {
                            browserLauncher.launch(nextStep.args)
                        }
                        is NextStep.Complete -> {
                            finishWithResult(nextStep.result)
                        }
                    }
                }
            }
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
