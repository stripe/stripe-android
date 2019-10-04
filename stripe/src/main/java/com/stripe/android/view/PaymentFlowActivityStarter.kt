package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData

class PaymentFlowActivityStarter :
    ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args> {

    constructor(activity: Activity) : super(
        activity, PaymentFlowActivity::class.java, Args.DEFAULT, REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment, PaymentFlowActivity::class.java, Args.DEFAULT, REQUEST_CODE
    )

    class Args private constructor(
        internal val paymentSessionConfig: PaymentSessionConfig,
        internal val paymentSessionData: PaymentSessionData?,
        internal val isPaymentSessionActive: Boolean
    ) : ActivityStarter.Args {
        private constructor(parcel: Parcel) : this(
            paymentSessionConfig = requireNotNull(
                parcel.readParcelable(PaymentSessionConfig::class.java.classLoader)
            ),
            paymentSessionData = parcel.readParcelable(PaymentSessionData::class.java.classLoader),
            isPaymentSessionActive = parcel.readInt() == 1
        )

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(paymentSessionConfig, 0)
            dest.writeParcelable(paymentSessionData, 0)
            dest.writeInt(if (isPaymentSessionActive) 1 else 0)
        }

        class Builder : ObjectBuilder<Args> {
            private var paymentSessionConfig: PaymentSessionConfig? = null
            private var paymentSessionData: PaymentSessionData? = null
            private var isPaymentSessionActive = false

            fun setPaymentSessionConfig(paymentSessionConfig: PaymentSessionConfig?): Builder {
                this.paymentSessionConfig = paymentSessionConfig
                return this
            }

            fun setPaymentSessionData(paymentSessionData: PaymentSessionData?): Builder {
                this.paymentSessionData = paymentSessionData
                return this
            }

            fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder {
                this.isPaymentSessionActive = isPaymentSessionActive
                return this
            }

            override fun build(): Args {
                return Args(
                    paymentSessionConfig = paymentSessionConfig
                        ?: PaymentSessionConfig.Builder().build(),
                    paymentSessionData = paymentSessionData,
                    isPaymentSessionActive = isPaymentSessionActive
                )
            }
        }

        companion object {
            internal val DEFAULT = Builder().build()

            @JvmStatic
            fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }

            @JvmField
            val CREATOR: Parcelable.Creator<Args> = object : Parcelable.Creator<Args> {
                override fun createFromParcel(parcel: Parcel): Args {
                    return Args(parcel)
                }

                override fun newArray(size: Int): Array<Args?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6002
    }
}
