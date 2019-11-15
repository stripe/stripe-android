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
        val toolbarCustomization: StripeToolbarCustomization? = null
    ) : Parcelable

    internal companion object {
        internal const val EXTRA_ARGS = "extra_args"
    }
}
