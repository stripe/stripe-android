package com.stripe.android

import android.os.Bundle
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import com.stripe.android.view.StripeIntentResultExtras

/**
 * Starts an instance of [PaymentRelayStarter].
 * Should only be called from [PaymentController].
 */
internal open class PaymentRelayStarter(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int
) : AuthActivityStarter<PaymentRelayStarter.Data> {

    override fun start(data: Data) {
        val extras = Bundle()
        extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
            if (data.stripeIntent != null) data.stripeIntent.clientSecret else null)
        extras.putSerializable(StripeIntentResultExtras.AUTH_EXCEPTION, data.exception)
        host.startActivityForResult(PaymentRelayActivity::class.java, extras, requestCode)
    }

    internal data class Data private constructor(
        val stripeIntent: StripeIntent? = null,
        val exception: Exception? = null
    ) {
        companion object {
            @JvmSynthetic
            internal fun create(stripeIntent: StripeIntent): Data {
                return Data(stripeIntent = stripeIntent)
            }

            @JvmSynthetic
            internal fun create(exception: Exception): Data {
                return Data(exception = exception)
            }
        }
    }
}
