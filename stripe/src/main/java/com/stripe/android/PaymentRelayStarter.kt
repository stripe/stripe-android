package com.stripe.android

import android.os.Bundle
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import com.stripe.android.view.StripeIntentResultExtras

/**
 * Starts an instance of [PaymentRelayStarter].
 * Should only be called from [StripePaymentController].
 */
internal interface PaymentRelayStarter : AuthActivityStarter<PaymentRelayStarter.Args> {
    companion object {
        @JvmSynthetic
        internal fun create(
            host: AuthActivityStarter.Host,
            requestCode: Int
        ): PaymentRelayStarter {
            return object : PaymentRelayStarter {
                override fun start(args: Args) {
                    val extras = Bundle()
                    extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
                        args.stripeIntent?.clientSecret)
                    extras.putSerializable(StripeIntentResultExtras.AUTH_EXCEPTION,
                        args.exception)
                    host.startActivityForResult(
                        PaymentRelayActivity::class.java, extras, requestCode
                    )
                }
            }
        }
    }

    data class Args internal constructor(
        val stripeIntent: StripeIntent? = null,
        val exception: Exception? = null
    ) {
        internal companion object {
            @JvmSynthetic
            internal fun create(stripeIntent: StripeIntent): Args {
                return Args(stripeIntent = stripeIntent)
            }

            @JvmSynthetic
            internal fun create(exception: Exception): Args {
                return Args(exception = exception)
            }
        }
    }
}
