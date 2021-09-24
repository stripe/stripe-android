package com.stripe.android.googlepaylauncher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.launch

/**
 * [GooglePayPaymentMethodLauncherActivity] is used to return the result of a Google Pay operation.
 * The activity's result will be a [GooglePayPaymentMethodLauncher.Result].
 *
 * [GooglePayPaymentMethodLauncherActivity] will:
 * 1. Verify that Google Pay is available
 * 2. Create a [PaymentDataRequest](https://developers.google.com/pay/api/web/reference/request-objects#PaymentDataRequest)
 * 3. Create a [PaymentMethod] using the Google Pay token
 *
 * Use [GooglePayPaymentMethodLauncherContract] to start [GooglePayPaymentMethodLauncherActivity].
 *
 * See [Troubleshooting](https://developers.google.com/pay/api/android/support/troubleshooting)
 * for a guide to troubleshooting Google Pay issues.
 */
internal class GooglePayPaymentMethodLauncherActivity : AppCompatActivity() {
    private val viewModel: GooglePayPaymentMethodLauncherViewModel by viewModels {
        GooglePayPaymentMethodLauncherViewModel.Factory(
            application,
            args,
            this
        )
    }

    private lateinit var args: GooglePayPaymentMethodLauncherContract.Args

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            // In Oreo, Activities where `android:windowIsTranslucent=true` can't request
            // orientation. See https://stackoverflow.com/a/50832408/11103900
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        disableAnimations()

        val nullableArgs = GooglePayPaymentMethodLauncherContract.Args.fromIntent(intent)
        if (nullableArgs == null) {
            finishWithResult(
                GooglePayPaymentMethodLauncher.Result.Failed(
                    RuntimeException(
                        "GooglePayPaymentMethodLauncherActivity was started without arguments."
                    ),
                    GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
                )
            )
            return
        }
        args = nullableArgs

        viewModel.googlePayResult.observe(this) { googlePayResult ->
            googlePayResult?.let(::finishWithResult)
        }

        if (!viewModel.hasLaunched) {
            lifecycleScope.launch {
                runCatching {
                    viewModel.createLoadPaymentDataTask()
                }.fold(
                    onSuccess = {
                        launchGooglePay(it)
                        viewModel.hasLaunched = true
                    },
                    onFailure = {
                        updateResult(
                            GooglePayPaymentMethodLauncher.Result.Failed(
                                it,
                                GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                            )
                        )
                    }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        disableAnimations()
    }

    private fun launchGooglePay(task: Task<PaymentData>) {
        AutoResolveHelper.resolveTask(
            task,
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

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
                    updateResult(
                        GooglePayPaymentMethodLauncher.Result.Canceled
                    )
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    val status = AutoResolveHelper.getStatusFromIntent(data)
                    val statusMessage = status?.statusMessage.orEmpty()
                    updateResult(
                        GooglePayPaymentMethodLauncher.Result.Failed(
                            RuntimeException(
                                "Google Pay failed with error ${status?.statusCode}: $statusMessage"
                            ),
                            status?.statusCode?.let {
                                googlePayStatusCodeToErrorCode(it)
                            } ?: GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                        )
                    )
                }
                else -> {
                    updateResult(
                        GooglePayPaymentMethodLauncher.Result.Failed(
                            RuntimeException("Google Pay returned an expected result code."),
                            GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                        )
                    )
                }
            }
        }
    }

    private fun onGooglePayResult(data: Intent?) {
        data?.let { PaymentData.getFromIntent(it) }?.let { paymentData ->
            lifecycleScope.launch {
                finishWithResult(
                    viewModel.createPaymentMethod(paymentData)
                )
            }
        } ?: updateResult(
            GooglePayPaymentMethodLauncher.Result.Failed(
                IllegalArgumentException("Google Pay data was not available"),
                GooglePayPaymentMethodLauncher.INTERNAL_ERROR
            )
        )
    }

    private fun updateResult(result: GooglePayPaymentMethodLauncher.Result) {
        viewModel.updateResult(result)
    }

    private fun finishWithResult(result: GooglePayPaymentMethodLauncher.Result) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(GooglePayPaymentMethodLauncherContract.EXTRA_RESULT to result)
                )
        )
        finish()
    }

    private fun disableAnimations() {
        // this is a transparent Activity so we want to disable animations
        overridePendingTransition(0, 0)
    }

    private fun googlePayStatusCodeToErrorCode(googlePayStatusCode: Int):
        @GooglePayPaymentMethodLauncher.ErrorCode Int {
        return when (googlePayStatusCode) {
            CommonStatusCodes.NETWORK_ERROR -> GooglePayPaymentMethodLauncher.NETWORK_ERROR
            CommonStatusCodes.DEVELOPER_ERROR -> GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
            else -> GooglePayPaymentMethodLauncher.INTERNAL_ERROR
        }
    }

    private companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 4444
    }
}
