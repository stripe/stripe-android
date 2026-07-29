package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.ApiConfiguration
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import javax.inject.Inject
import javax.inject.Provider

/**
 * Contract used to explicitly launch Link natively.
 */
internal class NativeLinkActivityContract @Inject constructor(
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
    private val requestSurface: RequestSurface,
    private val apiConfigurationProvider: Provider<ApiConfiguration.State>,
) :
    ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {
    override fun createIntent(context: Context, input: LinkActivityContract.Args): Intent {
        val apiConfiguration = apiConfigurationProvider.get()
        return LinkActivity.createIntent(
            context = context,
            args = NativeLinkArgs(
                configuration = input.configuration,
                paymentMethodMetadata = input.paymentMethodMetadata,
                requestSurface = requestSurface,
                stripeAccountId = apiConfiguration.stripeAccountId,
                publishableKey = apiConfiguration.publishableKey,
                linkExpressMode = input.linkExpressMode,
                launchMode = input.launchMode,
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                linkAccountInfo = input.linkAccountInfo,
                statusBarColor = input.statusBarColor,
            )
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return when (resultCode) {
            Activity.RESULT_CANCELED -> {
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            }

            LinkActivity.RESULT_COMPLETE -> {
                val result = intent?.extras?.let {
                    BundleCompat.getParcelable(it, LinkActivityContract.EXTRA_RESULT, LinkActivityResult::class.java)
                }
                return result ?: LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            }

            else -> {
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            }
        }
    }
}
