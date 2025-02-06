package com.stripe.android.link.express

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import javax.inject.Inject

internal class LinkExpressContract @Inject constructor() :
    ActivityResultContract<LinkExpressContract.Args, LinkExpressResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        return LinkExpressActivity.createIntent(
            context = context,
            args = LinkExpressArgs(
                configuration = input.configuration,
                stripeAccountId = paymentConfiguration.stripeAccountId,
                publishableKey = paymentConfiguration.publishableKey,
                linkAccount = input.linkAccount
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkExpressResult {
        return when (resultCode) {
            LinkExpressActivity.RESULT_COMPLETE -> {
                val result = intent?.extras?.let {
                    BundleCompat.getParcelable(it, EXTRA_RESULT, LinkExpressResult::class.java)
                }
                return result ?: LinkExpressResult.Canceled(LinkAccountUpdate.None)
            }
            Activity.RESULT_CANCELED -> {
                LinkExpressResult.Canceled(LinkAccountUpdate.None)
            }
            else -> LinkExpressResult.Canceled(LinkAccountUpdate.None)
        }
    }

    data class Args(
        val configuration: LinkConfiguration,
        val linkAccount: LinkAccount
    )

    companion object {
        internal const val EXTRA_RESULT =
            "com.stripe.android.link.express.LinkExpressContract.extra_result"
    }
}
