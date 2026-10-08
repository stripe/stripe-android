package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.R

/** Launches a native UPI app chooser; never opens a browser or handles a return URL. */
internal class UpiAppChooserActivity : AppCompatActivity() {
    private val args by lazy { UpiAppChooserContract.Args.fromIntent(intent) }
    private val viewModel: UpiAppChooserViewModel by viewModels {
        UpiAppChooserViewModel.Factory(requireNotNull(args))
    }

    // Always register on creation, including when an external app is still open after recreation.
    private val chooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (args != null) finishWithResult(viewModel.getResult())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finishWithResult(
                PaymentFlowResult.Unvalidated(
                    flowOutcome = StripeIntentResult.Outcome.FAILED,
                    exception = LocalStripeException(
                        displayMessage = getString(R.string.stripe_upi_launch_failed),
                        analyticsValue = "missingUpiAppChooserArgs",
                    ),
                )
            )
            return
        }
        onBackPressedDispatcher.addCallback(this) { finishWithResult(viewModel.getResult()) }
        if (!viewModel.hasLaunched) launchChooser()
    }

    override fun onStop() {
        if (args != null && !isFinishing) viewModel.onStop(isChangingConfigurations)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Some apps do not deliver an activity result. Only verify once checkout is foregrounded
        // after leaving for the external activity; recreation alone must not complete the flow.
        if (args != null && viewModel.hasLeftForExternalActivity) {
            finishWithResult(viewModel.getResult())
        }
    }

    private fun launchChooser() {
        try {
            chooserLauncher.launch(viewModel.createLaunchIntent())
            viewModel.hasLaunched = true
        } catch (e: ActivityNotFoundException) {
            finishWithResult(viewModel.getFailure(e))
        } catch (e: SecurityException) {
            finishWithResult(viewModel.getFailure(e))
        } catch (e: IllegalArgumentException) {
            finishWithResult(viewModel.getFailure(e))
        }
    }

    private fun finishWithResult(result: PaymentFlowResult.Unvalidated) {
        if (isFinishing) return
        setResult(Activity.RESULT_OK, Intent().putExtras(result.toBundle()))
        finish()
    }
}
