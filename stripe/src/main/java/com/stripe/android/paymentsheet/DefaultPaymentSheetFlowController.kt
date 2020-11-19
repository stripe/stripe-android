package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.ComponentActivity
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.parcelize.Parcelize

internal class DefaultPaymentSheetFlowController internal constructor(
    private val args: Args,
    private val paymentMethods: List<PaymentMethod>,
    private val defaultPaymentMethodId: String?
) : PaymentSheetFlowController {

    override fun presentPaymentOptions(
        activity: ComponentActivity,
        onComplete: (PaymentOption?) -> Unit
    ) {
        PaymentOptionsActivityStarter(activity)
            .startForResult(
                when (args) {
                    is Args.Default -> {
                        PaymentOptionsActivityStarter.Args.Default(
                            paymentMethods = paymentMethods,
                            ephemeralKey = args.ephemeralKey,
                            customerId = args.customerId
                        )
                    }
                    is Args.Guest -> {
                        PaymentOptionsActivityStarter.Args.Guest
                    }
                }
            )

        onComplete(null)
    }

    override fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentResult) -> Unit
    ) {
        // TODO(mshafrir-stripe): implement

        onComplete(
            PaymentResult.Cancelled(null, null)
        )
    }

    sealed class Args : Parcelable {
        abstract val clientSecret: String

        @Parcelize
        data class Default(
            override val clientSecret: String,
            val ephemeralKey: String,
            val customerId: String
        ) : Args()

        @Parcelize
        data class Guest(
            override val clientSecret: String
        ) : Args()
    }
}
