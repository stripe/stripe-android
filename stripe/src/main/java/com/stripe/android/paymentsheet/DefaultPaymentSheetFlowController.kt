package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentController
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.AuthActivityStarter
import kotlinx.parcelize.Parcelize

internal class DefaultPaymentSheetFlowController internal constructor(
    private val paymentController: PaymentController,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val args: Args,
    private val paymentMethods: List<PaymentMethod>,
    private val defaultPaymentMethodId: String?
) : PaymentSheetFlowController {
    private val confirmParamsFactory = ConfirmParamsFactory()
    private val paymentOptionFactory = PaymentOptionFactory()

    private var paymentSelection: PaymentSelection? = null

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

    override fun onPaymentOptionResult(intent: Intent?): PaymentOption? {
        val paymentSelection =
            (PaymentOptionResult.fromIntent(intent) as? PaymentOptionResult.Succeeded)?.paymentSelection
        return paymentSelection?.let(paymentOptionFactory::create)
    }

    override fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentResult) -> Unit
    ) {

        val confirmParams = paymentSelection?.let {
            confirmParamsFactory.create(
                args.clientSecret,
                it,
                // TODO(mshafrir-stripe): set correct value
                shouldSavePaymentMethod = false
            )
        }

        if (confirmParams != null) {
            paymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(activity),
                confirmParams,
                ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                )
            )
        } else {
            // TODO(mshafrir-stripe): handle error
        }

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
