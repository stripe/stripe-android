package com.stripe.android

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.StripeBrowserLauncherActivity
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A class that manages starting a [PaymentAuthWebViewActivity] with [PaymentAuthWebViewContract.Args].
 *
 * TODO(mshafrir-stripe): use a more generic class name
 */
internal interface PaymentAuthWebViewStarter :
    AuthActivityStarter<PaymentAuthWebViewContract.Args> {
    class Legacy(
        private val host: AuthActivityStarter.Host,
        private val isCustomTabsSupported: Boolean,
        private val defaultReturnUrl: DefaultReturnUrl
    ) : PaymentAuthWebViewStarter {
        override fun start(args: PaymentAuthWebViewContract.Args) {
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
        private val launcher: ActivityResultLauncher<PaymentAuthWebViewContract.Args>
    ) : PaymentAuthWebViewStarter {
        override fun start(args: PaymentAuthWebViewContract.Args) {
            launcher.launch(args)
        }
    }
}
