package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.stripe.android.ObjectBuilder
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import kotlinx.android.parcel.Parcelize

class PaymentFlowActivityStarter :
    ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args> {

    constructor(activity: Activity) : super(
        activity, PaymentFlowActivity::class.java, Args.DEFAULT, REQUEST_CODE
    )

    constructor(fragment: Fragment) : super(
        fragment, PaymentFlowActivity::class.java, Args.DEFAULT, REQUEST_CODE
    )

    @Parcelize
    data class Args internal constructor(
        internal val paymentSessionConfig: PaymentSessionConfig,
        internal val paymentSessionData: PaymentSessionData,
        internal val isPaymentSessionActive: Boolean
    ) : ActivityStarter.Args {
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
                    paymentSessionData = requireNotNull(paymentSessionData) {
                        "PaymentFlowActivity launched without PaymentSessionData"
                    },
                    isPaymentSessionActive = isPaymentSessionActive
                )
            }
        }

        companion object {
            internal val DEFAULT = Builder()
                .setPaymentSessionData(PaymentSessionData())
                .build()

            @JvmStatic
            fun create(intent: Intent): Args {
                return requireNotNull(intent.getParcelableExtra(ActivityStarter.Args.EXTRA))
            }
        }
    }

    companion object {
        const val REQUEST_CODE: Int = 6002
    }
}
