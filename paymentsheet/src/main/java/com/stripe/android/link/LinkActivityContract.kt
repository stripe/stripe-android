package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.serialization.PopupPayload
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class LinkActivityContract @Inject internal constructor(
    private val stripeRepository: StripeRepository,
) : ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return if (FeatureFlags.nativeLinkEnabled.isEnabled) {
            nativeIntent(context, input)
        } else {
            webIntent(context, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return createLinkActivityResult(resultCode, intent)
    }

    private fun webIntent(context: Context, input: Args): Intent {
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

    private fun nativeIntent(context: Context, input: Args): Intent {
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

    data class Args internal constructor(
        internal val configuration: LinkConfiguration
    )

    data class Result(
        val linkResult: LinkActivityResult
    )

    companion object {
        internal const val EXTRA_RESULT =
            "com.stripe.android.link.LinkActivityContract.extra_result"
    }
}
