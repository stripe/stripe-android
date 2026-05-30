package com.stripe.android.payments.samsungpay

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import kotlinx.coroutines.launch
import kotlin.getValue

internal class SamsungPayPaymentMethodLauncherActivity : BaseSamsungPayActivity() {

    private lateinit var args: SamsungPayPaymentMethodLauncherContract.Args

    private val viewModel: SamsungPayPaymentMethodLauncherViewModel by viewModels {
        SamsungPayPaymentMethodLauncherViewModel.Factory(args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = runCatching {
            requireNotNull(SamsungPayPaymentMethodLauncherContract.Args.fromIntent(intent)) {
                "SamsungPayPaymentMethodLauncherActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(
                SamsungPayPaymentMethodLauncher.Result.Failed(
                    error = it,
                    errorCode = SamsungPayPaymentMethodLauncher.INTERNAL_ERROR,
                )
            )
            return
        }

        lifecycleScope.launch {
            viewModel.result.collect { result ->
                result?.let(::finishWithResult)
            }
        }

        if (!viewModel.hasLaunched) {
            viewModel.hasLaunched = true
            startSamsungPay(
                config = args.config,
                currencyCode = args.currencyCode,
                amount = args.amount,
                orderNumber = args.orderNumber,
            )
        }
    }

    override fun onSamsungPaySuccess(
        response: CustomSheetPaymentInfo,
        paymentCredential: String,
        extraPaymentData: Bundle,
    ) {
        lifecycleScope.launch {
            val result = viewModel.createPaymentMethod(paymentCredential)
            viewModel.updateResult(result)
        }
    }

    override fun onSamsungPayFailure(errorCode: Int, errorData: Bundle?) {
        viewModel.updateResult(
            SamsungPayPaymentMethodLauncher.Result.Failed(
                error = Throwable("Samsung Pay failed with error code: $errorCode"),
                errorCode = SamsungPayPaymentMethodLauncher.INTERNAL_ERROR,
            )
        )
    }

    private fun finishWithResult(result: SamsungPayPaymentMethodLauncher.Result) {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                bundleOf(SamsungPayPaymentMethodLauncherContract.EXTRA_RESULT to result)
            )
        )
        finish()
    }
}
