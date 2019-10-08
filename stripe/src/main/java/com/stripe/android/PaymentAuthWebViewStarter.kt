package com.stripe.android

import android.os.Bundle
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A class that manages starting a [PaymentAuthWebViewActivity] instance with the correct
 * arguments.
 */
internal class PaymentAuthWebViewStarter @JvmOverloads internal constructor(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int,
    private val toolbarCustomization: StripeToolbarCustomization? = null,
    private val enableLogging: Boolean = false
) : AuthActivityStarter<PaymentAuthWebViewStarter.Data> {

    override fun start(data: Data) {
        Logger.getInstance(enableLogging).debug("PaymentAuthWebViewStarter#start()")
        val extras = Bundle()
        extras.putString(EXTRA_CLIENT_SECRET, data.clientSecret)
        extras.putString(EXTRA_AUTH_URL, data.url)
        extras.putString(EXTRA_RETURN_URL, data.returnUrl)
        extras.putBoolean(EXTRA_ENABLE_LOGGING, enableLogging)
        extras.putParcelable(EXTRA_UI_CUSTOMIZATION, toolbarCustomization)

        host.startActivityForResult(PaymentAuthWebViewActivity::class.java, extras, requestCode)
    }

    internal class Data(
        val clientSecret: String,
        val url: String,
        val returnUrl: String? = null
    )

    companion object {
        internal const val EXTRA_AUTH_URL = "auth_url"
        internal const val EXTRA_CLIENT_SECRET = "client_secret"
        internal const val EXTRA_RETURN_URL = "return_url"
        internal const val EXTRA_UI_CUSTOMIZATION = "ui_customization"
        internal const val EXTRA_ENABLE_LOGGING = "enable_logging"
    }
}
