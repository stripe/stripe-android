package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.AuthActivityStarter
import kotlinx.parcelize.Parcelize
import java.lang.Exception

internal class DefaultPaymentSheetFlowController internal constructor(
    private val paymentController: PaymentController,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val args: Args,
    private val paymentIntent: PaymentIntent,
    // the allowed payment method types
    internal val paymentMethodTypes: List<PaymentMethod.Type>,
    // the customer's existing payment methods
    internal val paymentMethods: List<PaymentMethod>,
    private val googlePayLauncherFactory: (ComponentActivity) -> StripeGooglePayLauncher = {
        StripeGooglePayLauncher(it)
    },
    private val defaultPaymentMethodId: String?
) : PaymentSheet.FlowController {
    private val confirmParamsFactory = ConfirmParamsFactory(args.clientSecret)
    private val paymentOptionFactory = PaymentOptionFactory()

    private var paymentSelection: PaymentSelection? = null

    override fun presentPaymentOptions(
        activity: ComponentActivity
    ) {
        PaymentOptionsActivityStarter(activity)
            .startForResult(
                PaymentOptionsActivityStarter.Args(
                    paymentIntent = paymentIntent,
                    paymentMethods = paymentMethods,
                    config = args.config
                )
            )
    }

    override fun onPaymentOptionResult(intent: Intent?): PaymentOption? {
        val paymentSelection =
            (PaymentOptionResult.fromIntent(intent) as? PaymentOptionResult.Succeeded)?.paymentSelection
        this.paymentSelection = paymentSelection
        return paymentSelection?.let(paymentOptionFactory::create)
    }

    override fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentResult) -> Unit
    ) {
        val paymentSelection = this.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            googlePayLauncherFactory(activity).startForResult(
                StripeGooglePayLauncher.Args(
                    environment = when (args.config?.googlePay?.environment) {
                        PaymentSheet.GooglePayConfiguration.Environment.Production ->
                            StripeGooglePayEnvironment.Production
                        else ->
                            StripeGooglePayEnvironment.Test
                    },
                    paymentIntent = paymentIntent,
                    countryCode = args.config?.googlePay?.countryCode.orEmpty(),
                    merchantName = args.config?.merchantDisplayName
                )
            )
        } else {
            when (paymentSelection) {
                is PaymentSelection.Saved -> {
                    confirmParamsFactory.create(paymentSelection)
                }
                is PaymentSelection.New.Card -> {
                    confirmParamsFactory.create(paymentSelection)
                }
                else -> null
            }?.let { confirmParams ->
                paymentController.startConfirmAndAuth(
                    AuthActivityStarter.Host.create(activity),
                    confirmParams,
                    ApiRequest.Options(
                        apiKey = publishableKey,
                        stripeAccount = stripeAccountId
                    )
                )
            }

            onComplete(
                PaymentResult.Cancelled(null, null)
            )
        }
    }

    override fun isPaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == StripeGooglePayLauncher.REQUEST_CODE ||
            paymentController.shouldHandlePaymentResult(requestCode, data)
    }

    /**
     * Handles results from both the standard confirmation flow via [PaymentController] and the
     * [StripeGooglePayLauncher] flow.
     */
    override fun onPaymentResult(
        requestCode: Int,
        data: Intent?,
        callback: ApiResultCallback<PaymentIntentResult>
    ) {
        if (data != null && paymentController.shouldHandlePaymentResult(requestCode, data)) {
            paymentController.handlePaymentResult(data, callback)
        } else if (data != null && requestCode == StripeGooglePayLauncher.REQUEST_CODE) {
            val googlePayResult = StripeGooglePayLauncher.Result.fromIntent(data) ?: return
            when (googlePayResult) {
                is StripeGooglePayLauncher.Result.PaymentIntent -> {
                    callback.onSuccess(googlePayResult.paymentIntentResult)
                }
                is StripeGooglePayLauncher.Result.Error -> {
                    val exception = googlePayResult.exception
                    callback.onError(
                        when (exception) {
                            is Exception -> exception
                            else -> RuntimeException(exception)
                        }
                    )
                }
                else -> {
                    // TODO(mshafrir-stripe): handle other outcomes
                }
            }
        }
    }

    @Parcelize
    data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : Parcelable
}
