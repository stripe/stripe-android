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
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

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
                    host.startActivityForResult(
                        PaymentRelayActivity::class.java,
                        args.toResult().toBundle(),
                        requestCode
                    )
                }
            }
        }
    }

    sealed class Args : Parcelable {
        abstract fun toResult(): PaymentController.Result

        @Parcelize
        data class PaymentIntentArgs(
            internal val paymentIntent: PaymentIntent,
            internal val stripeAccountId: String? = null
        ) : Args() {
            override fun toResult(): PaymentController.Result {
                return PaymentController.Result(
                    clientSecret = paymentIntent.clientSecret,
                    stripeAccountId = stripeAccountId
                )
            }
        }

        @Parcelize
        data class SetupIntentArgs(
            internal val setupIntent: SetupIntent,
            internal val stripeAccountId: String? = null
        ) : Args() {
            override fun toResult(): PaymentController.Result {
                return PaymentController.Result(
                    clientSecret = setupIntent.clientSecret,
                    stripeAccountId = stripeAccountId
                )
            }
        }

        @Parcelize
        data class SourceArgs(
            internal val source: Source,
            internal val stripeAccountId: String? = null
        ) : Args() {
            override fun toResult(): PaymentController.Result {
                return PaymentController.Result(
                    source = source,
                    stripeAccountId = stripeAccountId
                )
            }
        }

        @Parcelize
        data class ErrorArgs(
            internal val exception: StripeException
        ) : Args() {
            override fun toResult(): PaymentController.Result {
                return PaymentController.Result(
                    exception = exception
                )
            }

            internal companion object : Parceler<ErrorArgs> {
                override fun create(parcel: Parcel): ErrorArgs {
                    return ErrorArgs(
                        exception = parcel.readSerializable() as StripeException,
                    )
                }

                override fun ErrorArgs.write(parcel: Parcel, flags: Int) {
                    parcel.writeSerializable(exception)
                }
            }
        }

        companion object {
            fun create(
                stripeIntent: StripeIntent,
                stripeAccountId: String? = null
            ): Args {
                return when (stripeIntent) {
                    is PaymentIntent -> {
                        PaymentIntentArgs(stripeIntent, stripeAccountId)
                    }
                    is SetupIntent -> {
                        SetupIntentArgs(stripeIntent, stripeAccountId)
                    }
                    else -> {
                        error("StripeIntent must either be a PaymentIntent or SetupIntent.")
                    }
                }
            }
        }
    }
}
