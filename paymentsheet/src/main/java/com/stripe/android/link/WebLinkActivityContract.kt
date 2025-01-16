package com.stripe.android.link

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.serialization.PopupPayload
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class WebLinkActivityContract @Inject internal constructor(
    private val stripeRepository: StripeRepository,
) : ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: LinkActivityContract.Args): Intent {
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
        return createLinkActivityResult(resultCode, intent)
    }
}