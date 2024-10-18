package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NativeLinkActivityContract @Inject internal constructor() : ActivityResultContract<NativeLinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
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
        return createWebLinkActivityResult(resultCode, intent)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args internal constructor(
        internal val configuration: LinkConfiguration,
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Result(
        val linkResult: LinkActivityResult
    )

    companion object {
        internal const val EXTRA_RESULT =
            "com.stripe.android.link.NativeLinkActivityContract.extra_result"
    }
}
