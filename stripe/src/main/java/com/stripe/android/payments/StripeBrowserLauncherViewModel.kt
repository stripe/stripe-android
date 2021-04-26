package com.stripe.android.payments

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.stripe.android.auth.PaymentAuthWebViewContract

internal class StripeBrowserLauncherViewModel : ViewModel() {

    fun getResultIntent(args: PaymentAuthWebViewContract.Args): Intent {
        val url = Uri.parse(args.url)
        return Intent().putExtras(
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                sourceId = url.lastPathSegment.orEmpty(),
                stripeAccountId = args.stripeAccountId
            ).toBundle()
        )
    }
}
