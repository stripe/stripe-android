package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject

/**
 * Contract used to explicitly launch Link natively.
 */
internal class NativeLinkActivityContract @Inject constructor() :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {
    override fun createIntent(context: Context, input: LinkActivityContract.Args): Intent {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        return LinkActivity.createIntent(
            context = context,
            args = NativeLinkArgs(
                configuration = input.configuration,
                stripeAccountId = paymentConfiguration.stripeAccountId,
                publishableKey = paymentConfiguration.publishableKey,
                use2faDialog = input.use2faDialog
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return when (resultCode) {
            Activity.RESULT_CANCELED -> {
                LinkActivityResult.Canceled()
            }

            LinkActivity.RESULT_COMPLETE -> {
                val result = intent?.extras?.let {
                    BundleCompat.getParcelable(it, LinkActivityContract.EXTRA_RESULT, LinkActivityResult::class.java)
                }
                return result ?: LinkActivityResult.Canceled()
            }

            else -> {
                LinkActivityResult.Canceled()
            }
        }
    }
}
