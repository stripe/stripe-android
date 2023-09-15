package com.stripe.android.googlepaylauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.utils.AnimationConstants
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * [GooglePayLauncherActivity] is used to return the result of a Google Pay operation.
 * The activity's result will be a [GooglePayLauncher.Result].
 *
 * [GooglePayLauncherActivity] will:
 * 1. Fetch the [StripeIntent]
 * 2. Verify that Google Pay is available
 * 3. Create a [PaymentDataRequest](https://developers.google.com/pay/api/web/reference/request-objects#PaymentDataRequest)
 * 4. Confirm the [StripeIntent] using the Google Pay token
 *
 * Use [GooglePayLauncherContract] to start [GooglePayLauncherActivity].
 *
 * See [Troubleshooting](https://developers.google.com/pay/api/android/support/troubleshooting)
 * for a guide to troubleshooting Google Pay issues.
 */
internal class GooglePayLauncherActivity : AppCompatActivity() {
    private val viewModel: GooglePayLauncherViewModel by viewModels {
        GooglePayLauncherViewModel.Factory(args)
    }

    private lateinit var args: GooglePayLauncherContract.Args

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFadeAnimations()

        args = runCatching {
            requireNotNull(GooglePayLauncherContract.Args.fromIntent(intent)) {
                "GooglePayLauncherActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(
                GooglePayLauncher.Result.Failed(it)
            )
            return
        }

        viewModel.googlePayResult.observe(this) { googlePayResult ->
            googlePayResult?.let(::finishWithResult)
        }

        if (!viewModel.hasLaunched) {
            lifecycleScope.launch {
                viewModel.createLoadPaymentDataTask().fold(
                    onSuccess = {
                        payWithGoogle(it)
                        viewModel.hasLaunched = true
                    },
                    onFailure = {
                        viewModel.updateResult(
                            GooglePayLauncher.Result.Failed(it)
                        )
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        setFadeAnimations()
    }

    private fun payWithGoogle(task: Task<PaymentData>) {
        AutoResolveHelper.resolveTask(
            task,
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    onGooglePayResult(data)
                }
                RESULT_CANCELED -> {
                    viewModel.updateResult(
                        GooglePayLauncher.Result.Canceled
                    )
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    val statusMessage = status?.statusMessage.orEmpty()
                    viewModel.updateResult(
                        GooglePayLauncher.Result.Failed(
                            RuntimeException(
                                "Google Pay failed with error: $statusMessage"
                            )
                        )
                    )
                }
                else -> {
                    viewModel.updateResult(
                        GooglePayLauncher.Result.Failed(
                            RuntimeException(
                                "Google Pay returned an expected result code."
                            )
                        )
                    )
                }
            }
        } else {
            lifecycleScope.launch {
                viewModel.onConfirmResult(
                    requestCode,
                    data ?: Intent()
                )
            }
        }
    }

    private fun onGooglePayResult(data: Intent?) {
        val paymentData = data?.let { PaymentData.getFromIntent(it) }
        if (paymentData == null) {
            viewModel.updateResult(
                GooglePayLauncher.Result.Failed(
                    IllegalArgumentException("Google Pay data was not available")
                )
            )
            return
        }

        val paymentDataJson = JSONObject(paymentData.toJson())

        val params = PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)
        val host = AuthActivityStarterHost.create(this)
        lifecycleScope.launch {
            viewModel.confirmStripeIntent(host, params)
        }
    }

    private fun finishWithResult(result: GooglePayLauncher.Result) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(GooglePayLauncherContract.EXTRA_RESULT to result)
                )
        )
        finish()
    }

    private fun setFadeAnimations() {
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }

    private companion object {
        // the value isn't meaningful / is arbitrary
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 4444
    }
}
