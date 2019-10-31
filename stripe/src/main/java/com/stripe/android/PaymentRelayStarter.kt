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
internal interface PaymentRelayStarter : AuthActivityStarter<PaymentRelayStarter.Data> {
    companion object {
        @JvmSynthetic
        internal fun create(
            host: AuthActivityStarter.Host,
            requestCode: Int
        ): PaymentRelayStarter {
            return object : PaymentRelayStarter {
                override fun start(data: Data) {
                    val extras = Bundle()
                    extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
                        data.stripeIntent?.clientSecret)
                    extras.putSerializable(StripeIntentResultExtras.AUTH_EXCEPTION,
                        data.exception)
                    host.startActivityForResult(
                        PaymentRelayActivity::class.java, extras, requestCode
                    )
                }
            }
        }
    }

    data class Data internal constructor(
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
