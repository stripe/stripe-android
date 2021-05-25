package com.stripe.android.payments

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.Stripe3ds2CompletionActivity

internal sealed class Stripe3ds2CompletionStarter : AuthActivityStarter<PaymentFlowResult.Unvalidated> {
    class Modern(
        private val launcher: ActivityResultLauncher<PaymentFlowResult.Unvalidated>
    ) : Stripe3ds2CompletionStarter() {
        override fun start(args: PaymentFlowResult.Unvalidated) {
            launcher.launch(args)
        }
    }

    class Legacy(
        private val host: AuthActivityStarter.Host,
        private val requestCode: Int
    ) : Stripe3ds2CompletionStarter() {
        override fun start(args: PaymentFlowResult.Unvalidated) {
            host.startActivityForResult(
                Stripe3ds2CompletionActivity::class.java,
                args.toBundle(),
                requestCode
            )
        }
    }
}
