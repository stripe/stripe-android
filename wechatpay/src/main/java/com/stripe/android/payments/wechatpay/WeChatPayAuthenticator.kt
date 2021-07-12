package com.stripe.android.payments.wechatpay

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.getRequestCode
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.view.AuthActivityStarterHost

/**
 * [PaymentAuthenticator] implementation to authenticate through WeChatPay SDK.
 */
class WeChatPayAuthenticator : PaymentAuthenticator<StripeIntent> {
    /**
     * [weChatPayAuthLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    private var weChatPayAuthLauncher: ActivityResultLauncher<WeChatPayAuthContract.Args>? = null
    private val weChatAuthLauncherFactory =
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
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val weChatPayRedirect =
            requireNotNull(
                authenticatable.nextActionData as? StripeIntent.NextActionData.WeChatPayRedirect
            ) {
                "stripeIntent.nextActionData should be WeChatPayRedirect, instead it is " +
                    "${authenticatable.nextActionData}"
            }

        weChatAuthLauncherFactory(
            host,
            authenticatable.getRequestCode()
        ).start(
            WeChatPayAuthContract.Args(weChatPayRedirect.weChat)
        )
    }
}
