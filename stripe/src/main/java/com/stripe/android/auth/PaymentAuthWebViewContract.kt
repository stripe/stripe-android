package com.stripe.android.auth

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.PaymentAuthWebViewActivity
import kotlinx.parcelize.Parcelize

internal class PaymentAuthWebViewContract : ActivityResultContract<PaymentAuthWebViewContract.Args, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(
        context: Context,
        input: Args?
    ): Intent {
        return Intent(context, PaymentAuthWebViewActivity::class.java)
            .also { intent ->
                if (input != null) {
                    intent.putExtras(input.toBundle())
                }
            }
    }

    internal fun parseArgs(
        intent: Intent
    ): Args? {
        return intent.getParcelableExtra(EXTRA_ARGS)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated? {
        return intent?.getParcelableExtra(EXTRA_ARGS)
    }

    @Parcelize
    internal data class Args(
        val requestCode: Int,
        val clientSecret: String,
        val url: String,
        val returnUrl: String? = null,
        val enableLogging: Boolean = false,
        val toolbarCustomization: StripeToolbarCustomization? = null,
        val stripeAccountId: String? = null,
        val shouldCancelSource: Boolean = false,
        /**
         * For most payment methods, if the user navigates away from the webview
         * (e.g. by pressing the back button or tapping "close" in the menu bar),
         * we assume the confirmation flow has been cancelled.
         *
         * However, for some payment methods, such as OXXO, no immediate user action is required.
         * Simply displaying the web view is all we need to do, and we expect the user to
         * navigate away after this.
         */
        val shouldCancelIntentOnUserNavigation: Boolean = true
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)
    }

    private companion object {
        const val EXTRA_ARGS = "extra_args"
    }
}
