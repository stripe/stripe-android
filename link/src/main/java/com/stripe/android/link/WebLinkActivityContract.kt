package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.serialization.PopupPayload
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class WebLinkActivityContract @Inject internal constructor(
    private val stripeRepository: StripeRepository,
) : ActivityResultContract<WebLinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        val payload = PopupPayload.create(
            configuration = input.configuration,
            context = context,
            publishableKey = paymentConfiguration.publishableKey,
            stripeAccount = paymentConfiguration.stripeAccountId,
            paymentUserAgent = stripeRepository.buildPaymentUserAgent(),
        )
        return LinkForegroundActivity.createIntent(context, payload.toUrl())
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
            "com.stripe.android.link.WebLinkActivityContract.extra_result"
    }
}
