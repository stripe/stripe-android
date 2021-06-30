package com.stripe.android.payments.wechatpay

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.getRequestCode
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import com.stripe.android.view.AuthActivityStarterHost

/**
 * [IntentAuthenticator] authenticating through WeChatPay SDK.
 */
class WeChatPayAuthenticator : IntentAuthenticator {
    /**
     * [weChatPayAuthLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    private var weChatPayAuthLauncher: ActivityResultLauncher<WeChatPayAuthContract.Args>? = null

    @VisibleForTesting
    internal var weChatAuthLauncherFactory =
        { host: AuthActivityStarterHost, requestCode: Int ->
            weChatPayAuthLauncher?.let {
                WeChatPayAuthStarter.Modern(it)
            } ?: WeChatPayAuthStarter.Legacy(host, requestCode)
        }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        weChatPayAuthLauncher = activityResultCaller.registerForActivityResult(
            WeChatPayAuthContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        weChatPayAuthLauncher?.unregister()
        weChatPayAuthLauncher = null
    }

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String?,
        requestOptions: ApiRequest.Options
    ) {
        val weChatPayRedirect =
            requireNotNull(
                stripeIntent.nextActionData as? StripeIntent.NextActionData.WeChatPayRedirect
            ) {
                "stripeIntent.nextActionData should be WeChatPayRedirect, instead it is " +
                    "${stripeIntent.nextActionData}"
            }

        weChatAuthLauncherFactory(
            host,
            stripeIntent.getRequestCode()
        ).start(
            WeChatPayAuthContract.Args(
                weChatPayRedirect.weChat,
                stripeIntent.clientSecret.orEmpty()
            )
        )
    }
}
