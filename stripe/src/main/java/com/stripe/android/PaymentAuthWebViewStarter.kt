package com.stripe.android

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A class that manages starting a [PaymentAuthWebViewActivity] with [PaymentAuthWebViewContract.Args].
 */
internal interface PaymentAuthWebViewStarter : AuthActivityStarter<PaymentAuthWebViewContract.Args> {
    class Legacy(
        private val host: AuthActivityStarter.Host
    ) : PaymentAuthWebViewStarter {
        override fun start(args: PaymentAuthWebViewContract.Args) {
            host.startActivityForResult(
                PaymentAuthWebViewActivity::class.java,
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
