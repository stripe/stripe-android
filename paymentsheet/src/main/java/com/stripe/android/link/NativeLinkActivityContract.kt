package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject

internal class NativeLinkActivityContract @Inject constructor() :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {
    override fun createIntent(context: Context, input: LinkActivityContract.Args): Intent {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        return LinkActivity.createIntent(
            context = context,
            args = NativeLinkArgs(
                configuration = input.configuration,
                stripeAccountId = paymentConfiguration.stripeAccountId,
                publishableKey = paymentConfiguration.publishableKey
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return createLinkActivityResult(resultCode, intent)
    }
}
