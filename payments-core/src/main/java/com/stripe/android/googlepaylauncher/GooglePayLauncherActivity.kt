package com.stripe.android.googlepaylauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.utils.fadeOut
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        lifecycleScope.launch {
            viewModel.googlePayResult.collect { googlePayResult ->
                googlePayResult?.let(::finishWithResult)
            }
        }

        val googlePayLauncher = registerForActivityResult(GetPaymentDataResult()) {
            onGooglePayResult(it)
        }

        if (!viewModel.hasLaunched) {
            lifecycleScope.launch {
                viewModel.createLoadPaymentDataTask().fold(
                    onSuccess = {
                        googlePayLauncher.launch(it)
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
        fadeOut()
    }

    private fun onGooglePayResult(taskResult: ApiTaskResult<PaymentData>) {
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                onGooglePayResult(taskResult.result!!)
            }

            CommonStatusCodes.CANCELED -> {
                viewModel.updateResult(
                    GooglePayLauncher.Result.Canceled
                )
            }

            AutoResolveHelper.RESULT_ERROR -> {
                val statusMessage = taskResult.status.statusMessage.orEmpty()
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
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch {
            viewModel.onConfirmResult(
                requestCode,
                data ?: Intent()
            )
        }
    }

    private fun onGooglePayResult(paymentData: PaymentData) {
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
}
