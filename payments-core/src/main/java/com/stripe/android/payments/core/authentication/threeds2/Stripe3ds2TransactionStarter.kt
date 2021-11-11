package com.stripe.android.payments.core.authentication.threeds2

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.StripePaymentController
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.AuthActivityStarterHost

/**
 * A [AuthActivityStarter] to start [Stripe3ds2TransactionActivity]
 * with legacy [Activity.startActivityForResult] or modern [ActivityResultLauncher.launch].
 */
internal interface Stripe3ds2TransactionStarter :
    AuthActivityStarter<Stripe3ds2TransactionContract.Args> {
    class Legacy(
        private val host: AuthActivityStarterHost
    ) : Stripe3ds2TransactionStarter {
        override fun start(args: Stripe3ds2TransactionContract.Args) {
            host.startActivityForResult(
                Stripe3ds2TransactionActivity::class.java,
                args.toBundle(),
                StripePaymentController.getRequestCode(args.stripeIntent)
            )
        }
    }

    class Modern(
        private val launcher: ActivityResultLauncher<Stripe3ds2TransactionContract.Args>
    ) : Stripe3ds2TransactionStarter {
        override fun start(args: Stripe3ds2TransactionContract.Args) {
            launcher.launch(args)
        }
    }
}
