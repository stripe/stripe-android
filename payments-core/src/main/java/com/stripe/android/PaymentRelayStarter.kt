package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.AuthActivityStarterHost
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Starts an instance of [PaymentRelayStarter].
 * Should only be called from [StripePaymentController].
 */
internal interface PaymentRelayStarter : AuthActivityStarter<PaymentRelayStarter.Args> {
    class Legacy(
        private val host: AuthActivityStarterHost
    ) : PaymentRelayStarter {
        override fun start(args: Args) {
            host.startActivityForResult(
                PaymentRelayActivity::class.java,
                args.toResult().toBundle(),
                args.requestCode
            )
        }
    }

    class Modern(
        private val launcher: ActivityResultLauncher<Args>
    ) : PaymentRelayStarter {
        override fun start(args: Args) {
            launcher.launch(args)
        }
    }

    sealed class Args : Parcelable {
        abstract val requestCode: Int

        abstract fun toResult(): PaymentFlowResult.Unvalidated

        @Parcelize
        data class PaymentIntentArgs(
            internal val paymentIntent: PaymentIntent,
            internal val stripeAccountId: String? = null
        ) : Args() {
            override val requestCode: Int
                get() = StripePaymentController.PAYMENT_REQUEST_CODE

            override fun toResult(): PaymentFlowResult.Unvalidated {
                return PaymentFlowResult.Unvalidated(
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
            override val requestCode: Int
                get() = StripePaymentController.SETUP_REQUEST_CODE

            override fun toResult(): PaymentFlowResult.Unvalidated {
                return PaymentFlowResult.Unvalidated(
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
            override val requestCode: Int
                get() = StripePaymentController.SOURCE_REQUEST_CODE

            override fun toResult(): PaymentFlowResult.Unvalidated {
                return PaymentFlowResult.Unvalidated(
                    source = source,
                    stripeAccountId = stripeAccountId
                )
            }
        }

        @Parcelize
        data class ErrorArgs(
            internal val exception: StripeException,
            override val requestCode: Int
        ) : Args() {
            override fun toResult(): PaymentFlowResult.Unvalidated {
                return PaymentFlowResult.Unvalidated(
                    exception = exception
                )
            }

            internal companion object : Parceler<ErrorArgs> {
                override fun create(parcel: Parcel): ErrorArgs {
                    return ErrorArgs(
                        exception = parcel.readSerializable() as StripeException,
                        requestCode = parcel.readInt()
                    )
                }

                override fun ErrorArgs.write(parcel: Parcel, flags: Int) {
                    parcel.writeSerializable(exception)
                    parcel.writeInt(requestCode)
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
                }
            }
        }
    }
}
