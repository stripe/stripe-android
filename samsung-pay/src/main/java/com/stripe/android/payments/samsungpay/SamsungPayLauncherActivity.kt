package com.stripe.android.payments.samsungpay

import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo

internal class SamsungPayLauncherActivity : BaseSamsungPayActivity() {

    private lateinit var args: SamsungPayLauncherContract.Args

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = runCatching {
            requireNotNull(SamsungPayLauncherContract.Args.fromIntent(intent)) {
                "SamsungPayLauncherActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(SamsungPayResult.Failed(it))
            return
        }

        // TODO: Retrieve the PaymentIntent from Stripe API to get amount/currency.
        startSamsungPay(
            config = args.config,
            currencyCode = "USD",
            amount = 0L,
            orderNumber = args.clientSecret,
        )
    }

    override fun onSamsungPaySuccess(
        response: CustomSheetPaymentInfo,
        paymentCredential: String,
        extraPaymentData: Bundle,
    ) {
        // TODO: Confirm the StripeIntent with PaymentController using the paymentCredential.
        finishWithResult(SamsungPayResult.Completed)
    }

    override fun onSamsungPayFailure(errorCode: Int, errorData: Bundle?) {
        finishWithResult(
            SamsungPayResult.Failed(
                Throwable("Samsung Pay failed with error code: $errorCode")
            )
        )
    }

    private fun finishWithResult(result: SamsungPayResult) {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                bundleOf(SamsungPayLauncherContract.EXTRA_RESULT to result)
            )
        )
        finish()
    }
}
