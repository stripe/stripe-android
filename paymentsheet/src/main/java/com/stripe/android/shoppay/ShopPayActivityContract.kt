package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentsheet.PaymentSheet
import javax.inject.Inject

internal class ShopPayActivityContract @Inject constructor(
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) :
    ActivityResultContract<ShopPayActivityContract.Args, ShopPayActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val configuration = PaymentConfiguration.getInstance(context)
        return ShopPayActivity.createIntent(
            context,
            ShopPayArgs(
                shopPayConfiguration = input.shopPayConfiguration,
                publishableKey = configuration.publishableKey,
                stripeAccountId = configuration.stripeAccountId,
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                customerSessionClientSecret = input.customerSessionClientSecret,
                businessName = input.businessName
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ShopPayActivityResult {
        val result = intent?.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_RESULT, ShopPayActivityResult::class.java)
        }
        return result ?: ShopPayActivityResult.Failed(Throwable("No result"))
    }

    data class Args(
        val shopPayConfiguration: PaymentSheet.ShopPayConfiguration,
        val customerSessionClientSecret: String,
        val businessName: String
    )

    companion object {
        internal const val EXTRA_RESULT = "com.stripe.android.shoppay.ShopPayActivityContract.extra_result"
    }
}
