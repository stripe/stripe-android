package com.stripe.android.googlepaylauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.stripe.android.StripePaymentController.Companion.PAYMENT_REQUEST_CODE
import com.stripe.android.StripePaymentController.Companion.SETUP_REQUEST_CODE
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.uicore.utils.fadeOut
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

    private val errorReporter: ErrorReporter by lazy {
        ErrorReporter.createFallbackInstance(context = this)
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.googlePayLaunchTask.collect { task ->
                    if (task != null) {
                        googlePayLauncher.launch(task)
                        viewModel.markTaskAsLaunched()
                    }
                }
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
                val result = taskResult.result
                if (result != null) {
                    val paymentDataJson = JSONObject(result.toJson())
                    val params = PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)
                    val host = AuthActivityStarterHost.create(this)
                    viewModel.confirmStripeIntent(host, params)
                } else {
                    errorReporter.report(ErrorReporter.UnexpectedErrorEvent.GOOGLE_PAY_MISSING_INTENT_DATA)
                    viewModel.updateResult(
                        GooglePayLauncher.Result.Failed(
                            RuntimeException(
                                "Google Pay missing result data."
                            )
                        )
                    )
                }
            }

            CommonStatusCodes.CANCELED -> {
                viewModel.updateResult(
                    GooglePayLauncher.Result.Canceled
                )
            }

            else -> {
                val status = taskResult.status
                val statusMessage = status.statusMessage.orEmpty()
                val statusCode = status.statusCode.toString()

                errorReporter.report(
                    ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_FAILED,
                    additionalNonPiiParams = mapOf(
                        "status_message" to statusMessage,
                        "status_code" to statusCode,
                    )
                )

                viewModel.updateResult(
                    GooglePayLauncher.Result.Failed(
                        RuntimeException(
                            "Google Pay failed with error ${status.statusCode}: ${status.statusMessage.orEmpty()}"
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
        if (requestCode == PAYMENT_REQUEST_CODE || requestCode == SETUP_REQUEST_CODE) {
            viewModel.onConfirmResult(
                requestCode,
                data ?: Intent()
            )
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
