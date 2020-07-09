package com.stripe.android

import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthWebViewActivity
import kotlinx.android.parcel.Parcelize

/**
 * A class that manages starting a [PaymentAuthWebViewActivity] instance with the correct
 * arguments.
 */
internal class PaymentAuthWebViewStarter internal constructor(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int
) : AuthActivityStarter<PaymentAuthWebViewStarter.Args> {

    override fun start(args: Args) {
        val extras = Bundle().apply {
            putParcelable(EXTRA_ARGS, args)
        }
        host.startActivityForResult(PaymentAuthWebViewActivity::class.java, extras, requestCode)
    }

    @Parcelize
    internal data class Args(
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
    ) : Parcelable

    internal companion object {
        internal const val EXTRA_ARGS = "extra_args"
    }
}
