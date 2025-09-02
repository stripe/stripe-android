package com.stripe.android.payments.core.authentication.stripejs

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.StripePaymentController
import com.stripe.android.view.AuthActivityStarterHost

internal interface StripeJsNextActionStarter {
    fun start(args: StripeJsNextActionContract.Args)

    class Modern(
        private val launcher: ActivityResultLauncher<StripeJsNextActionContract.Args>
    ) : StripeJsNextActionStarter {
        override fun start(args: StripeJsNextActionContract.Args) {
            launcher.launch(args)
        }
    }

    class Legacy(
        private val host: AuthActivityStarterHost
    ) : StripeJsNextActionStarter {
        override fun start(args: StripeJsNextActionContract.Args) {
            host.startActivityForResult(
                target = StripeJsNextActionActivity::class.java,
                extras = StripeJsNextActionActivity.getBundle(
                    args = StripeJsNextActionArgs(
                        publishableKey = args.publishableKey,
                        intent = args.intent
                    )
                ),
                requestCode = StripePaymentController.getRequestCode(args.intent)
            )
        }
    }
}
