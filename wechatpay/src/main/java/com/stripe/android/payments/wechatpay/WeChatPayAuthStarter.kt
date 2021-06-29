package com.stripe.android.payments.wechatpay

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.payments.wechatpay.WeChatPayAuthStarter.Legacy
import com.stripe.android.payments.wechatpay.WeChatPayAuthStarter.Modern
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.AuthActivityStarterHost

/**
 * An [AuthActivityStarter] wrapper to start a [WeChatPayAuthActivity] through different APIs.
 *
 * [Modern] uses the [ActivityResultLauncher].
 * [Legacy] uses [Activity.startActivityForResult] or [Fragment.startActivityForResult].
 */
internal sealed class WeChatPayAuthStarter : AuthActivityStarter<WeChatPayAuthContract.Args> {
    class Modern(
        private val launcher: ActivityResultLauncher<WeChatPayAuthContract.Args>
    ) : WeChatPayAuthStarter() {
        override fun start(args: WeChatPayAuthContract.Args) {
            launcher.launch(args)
        }
    }

    class Legacy(
        private val host: AuthActivityStarterHost,
        private val requestCode: Int
    ) : WeChatPayAuthStarter() {
        override fun start(args: WeChatPayAuthContract.Args) {
            host.startActivityForResult(
                WeChatPayAuthActivity::class.java,
                args.toBundle(),
                requestCode
            )
        }
    }
}
