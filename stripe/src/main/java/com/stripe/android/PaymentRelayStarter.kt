package com.stripe.android

import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import com.stripe.android.view.StripeIntentResultExtras
import kotlinx.android.parcel.Parcelize

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
                    args.stripeIntent?.let {
                        extras.putString(StripeIntentResultExtras.CLIENT_SECRET, it.clientSecret)
                    }
                    args.source?.let {
                        extras.putParcelable(StripeIntentResultExtras.SOURCE, it)
                    }
                    args.exception?.let {
                        extras.putSerializable(StripeIntentResultExtras.AUTH_EXCEPTION, it)
                    }
                    host.startActivityForResult(
                        PaymentRelayActivity::class.java, extras, requestCode
                    )
                }
            }
        }
    }

    @Parcelize
    data class Args internal constructor(
        val stripeIntent: StripeIntent? = null,
        val source: Source? = null,
        val exception: Exception? = null
    ) : Parcelable {
        internal companion object {
            @JvmSynthetic
            internal fun create(stripeIntent: StripeIntent): Args {
                return Args(stripeIntent = stripeIntent)
            }

            @JvmSynthetic
            internal fun create(source: Source): Args {
                return Args(source = source)
            }

            @JvmSynthetic
            internal fun create(exception: Exception): Args {
                return Args(exception = exception)
            }
        }
    }
}
