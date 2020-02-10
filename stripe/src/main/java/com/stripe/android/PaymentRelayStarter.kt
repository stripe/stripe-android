package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.android.parcel.Parceler
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
                    val extras = PaymentController.Result(
                        clientSecret = args.stripeIntent?.clientSecret,
                        source = args.source,
                        exception = args.exception
                    ).toBundle()
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
        val exception: StripeException? = null
    ) : Parcelable {
        internal companion object : Parceler<Args> {
            @JvmSynthetic
            internal fun create(stripeIntent: StripeIntent): Args {
                return Args(stripeIntent = stripeIntent)
            }

            @JvmSynthetic
            internal fun create(source: Source): Args {
                return Args(source = source)
            }

            @JvmSynthetic
            internal fun create(exception: StripeException): Args {
                return Args(exception = exception)
            }

            override fun create(parcel: Parcel): Args {
                return Args(
                    stripeIntent = readStripeIntent(parcel),
                    source = parcel.readParcelable(Source::class.java.classLoader),
                    exception = parcel.readSerializable() as? StripeException?
                )
            }

            override fun Args.write(parcel: Parcel, flags: Int) {
                writeStripeIntent(parcel, stripeIntent)
                parcel.writeParcelable(source, 0)
                parcel.writeSerializable(exception)
            }

            private fun readStripeIntent(parcel: Parcel): StripeIntent? {
                return when (StripeIntentType.values()[parcel.readInt()]) {
                    StripeIntentType.PaymentIntent ->
                        parcel.readParcelable(PaymentIntent::class.java.classLoader)
                    StripeIntentType.SetupIntent ->
                        parcel.readParcelable(SetupIntent::class.java.classLoader)
                    else -> null
                }
            }

            private fun writeStripeIntent(parcel: Parcel, stripeIntent: StripeIntent?) {
                val stripeIntentType = when (stripeIntent) {
                    is PaymentIntent -> StripeIntentType.PaymentIntent
                    is SetupIntent -> StripeIntentType.SetupIntent
                    else -> StripeIntentType.None
                }
                parcel.writeInt(stripeIntentType.ordinal)
                stripeIntent?.let {
                    parcel.writeParcelable(it, 0)
                }
            }

            private enum class StripeIntentType {
                None,
                PaymentIntent,
                SetupIntent
            }
        }
    }
}
