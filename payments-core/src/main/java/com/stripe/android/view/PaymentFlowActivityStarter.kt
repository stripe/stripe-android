package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import kotlinx.parcelize.Parcelize

class PaymentFlowActivityStarter :
    ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args> {

    constructor(activity: Activity, config: PaymentSessionConfig) : super(
        activity,
        PaymentFlowActivity::class.java,
        REQUEST_CODE
    )

    constructor(fragment: Fragment, config: PaymentSessionConfig) : super(
        fragment,
        PaymentFlowActivity::class.java,
        REQUEST_CODE
    )

    @Parcelize
    data class Args internal constructor(
        internal val paymentSessionConfig: PaymentSessionConfig,
        internal val paymentSessionData: PaymentSessionData,
        internal val isPaymentSessionActive: Boolean = false,
        internal val windowFlags: Int? = null
    ) : ActivityStarter.Args {
        class Builder {
            private var paymentSessionConfig: PaymentSessionConfig? = null
            private var paymentSessionData: PaymentSessionData? = null
            private var isPaymentSessionActive = false
            private var windowFlags: Int? = null

            fun setPaymentSessionConfig(
                paymentSessionConfig: PaymentSessionConfig?
            ): Builder = apply {
                this.paymentSessionConfig = paymentSessionConfig
            }

            fun setPaymentSessionData(paymentSessionData: PaymentSessionData?): Builder = apply {
                this.paymentSessionData = paymentSessionData
            }

            fun setIsPaymentSessionActive(isPaymentSessionActive: Boolean): Builder = apply {
                this.isPaymentSessionActive = isPaymentSessionActive
            }

            /**
             * @param windowFlags optional flags to set on the `Window` object of Stripe Activities
             *
             * See [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
             */
            fun setWindowFlags(windowFlags: Int?): Builder = apply {
                this.windowFlags = windowFlags
            }

            fun build(): Args {
                return Args(
                    paymentSessionConfig = requireNotNull(paymentSessionConfig) {
                        "PaymentFlowActivity launched without PaymentSessionConfig"
                    },
                    paymentSessionData = requireNotNull(paymentSessionData) {
                        "PaymentFlowActivity launched without PaymentSessionData"
                    },
                    isPaymentSessionActive = isPaymentSessionActive,
                    windowFlags = windowFlags
                )
            }
        }

        companion object {
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
