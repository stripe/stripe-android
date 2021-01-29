package com.stripe.android

import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthUpiAppViewActivity
import com.stripe.android.view.PaymentAuthWebViewActivity
import kotlinx.parcelize.Parcelize

/**
 * A class that manages starting a [PaymentAuthWebViewActivity] instance with the correct
 * arguments.
 */
internal class PaymentAuthUpiAppViewStarter internal constructor(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int
) : AuthActivityStarter<PaymentAuthUpiAppViewStarter.Args> {

    override fun start(args: Args) {
        host.startActivityForResult(
            PaymentAuthUpiAppViewActivity::class.java,
            bundleOf(EXTRA_ARGS to args),
            requestCode
        )
    }

    @Parcelize
    internal data class Args(
        val nativeData: String,
        val enableLogging: Boolean = false,
        val clientSecret: String
    ) : Parcelable

    internal companion object {
        internal const val EXTRA_ARGS = "extra_args"
    }
}
