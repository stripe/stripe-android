package com.stripe.android

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A class that manages starting a [StripeBrowserLauncherActivity] or [PaymentAuthWebViewActivity]
 * with a [PaymentBrowserAuthContract.Args].
 */
internal interface PaymentBrowserAuthStarter :
    AuthActivityStarter<PaymentBrowserAuthContract.Args> {
    class Legacy(
        private val host: AuthActivityStarter.Host,
        private val isCustomTabsSupported: Boolean,
        private val defaultReturnUrl: DefaultReturnUrl
    ) : PaymentBrowserAuthStarter {
        override fun start(args: PaymentBrowserAuthContract.Args) {
            val shouldUseCustomTabs = args.shouldUseCustomTabs(
                isCustomTabsSupported,
                defaultReturnUrl
            )
            host.startActivityForResult(
                when (shouldUseCustomTabs) {
                    true -> StripeBrowserLauncherActivity::class.java
                    false -> PaymentAuthWebViewActivity::class.java
                },
                args.toBundle(),
                args.requestCode
            )
        }
    }

    class Modern(
        private val launcher: ActivityResultLauncher<PaymentBrowserAuthContract.Args>
    ) : PaymentBrowserAuthStarter {
        override fun start(args: PaymentBrowserAuthContract.Args) {
            launcher.launch(args)
        }
    }
}
