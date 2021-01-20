package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.ConfirmParamsFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.AuthActivityStarter
import kotlinx.parcelize.Parcelize

internal class DefaultPaymentSheetFlowController internal constructor(
    private val paymentController: PaymentController,
    private val eventReporter: EventReporter,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val args: Args,
    private val paymentIntent: PaymentIntent,
    // the allowed payment method types
    internal val paymentMethodTypes: List<PaymentMethod.Type>,
    // the customer's existing payment methods
    internal val paymentMethods: List<PaymentMethod>,
    private val sessionId: SessionId,
    private val googlePayLauncherFactory: (ComponentActivity) -> StripeGooglePayLauncher = {
        StripeGooglePayLauncher(it)
    },
    private val defaultPaymentMethodId: String?
) : PaymentSheet.FlowController {
    private val confirmParamsFactory = ConfirmParamsFactory(args.clientSecret)
    private val paymentOptionFactory = PaymentOptionFactory()

    private var paymentSelection: PaymentSelection? = null

    init {
        eventReporter.onInit(args.config)

        val defaultPaymentMethod = paymentMethods.firstOrNull { it.id == defaultPaymentMethodId }
        paymentSelection = defaultPaymentMethod?.let {
            PaymentSelection.Saved(it)
        }
    }

    override fun getPaymentOption(): PaymentOption? {
        return paymentSelection?.let {
            paymentOptionFactory.create(it)
        }
    }

    override fun presentPaymentOptions(
        activity: ComponentActivity
    ) {
        PaymentOptionsActivityStarter(activity)
            .startForResult(
                PaymentOptionContract.Args(
                    paymentIntent = paymentIntent,
                    paymentMethods = paymentMethods,
                    sessionId = sessionId,
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
        activity: ComponentActivity
    ) {
        val paymentSelection = this.paymentSelection
        if (paymentSelection == PaymentSelection.GooglePay) {
            googlePayLauncherFactory(activity).startForResult(
                StripeGooglePayContract.Args(
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
        }
    }

    override fun isPaymentResult(requestCode: Int, data: Intent?): Boolean {
        return requestCode == StripeGooglePayLauncher.REQUEST_CODE ||
            paymentController.shouldHandlePaymentResult(requestCode, data)
    }

    /**
     * Handles results from both the standard confirmation flow via [PaymentController] and the
     * [StripeGooglePayContract] flow.
     */
    override fun onPaymentResult(
        requestCode: Int,
        data: Intent?,
        callback: PaymentSheetResultCallback
    ) {
        if (data != null && paymentController.shouldHandlePaymentResult(requestCode, data)) {
            paymentController.handlePaymentResult(
                data,
                object : ApiResultCallback<PaymentIntentResult> {
                    override fun onSuccess(result: PaymentIntentResult) {
                        if (result.outcome == StripeIntentResult.Outcome.SUCCEEDED) {
                            eventReporter.onPaymentSuccess(paymentSelection)
                            callback.onComplete(
                                PaymentResult.Succeeded(result.intent)
                            )
                        } else {
                            eventReporter.onPaymentFailure(paymentSelection)

                            callback.onComplete(
                                PaymentResult.Failed(
                                    RuntimeException(result.failureMessage),
                                    result.intent
                                )
                            )
                        }
                    }

                    override fun onError(e: Exception) {
                        eventReporter.onPaymentFailure(paymentSelection)
                        callback.onComplete(
                            PaymentResult.Failed(e, null)
                        )
                    }
                }
            )
        } else if (data != null && requestCode == StripeGooglePayLauncher.REQUEST_CODE) {
            when (val googlePayResult = StripeGooglePayContract.Result.fromIntent(data)) {
                is StripeGooglePayContract.Result.PaymentIntent -> {
                    eventReporter.onPaymentSuccess(PaymentSelection.GooglePay)
                    callback.onComplete(
                        PaymentResult.Succeeded(
                            googlePayResult.paymentIntentResult.intent
                        )
                    )
                }
                is StripeGooglePayContract.Result.Error -> {
                    eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                    val exception = googlePayResult.exception
                    callback.onComplete(
                        PaymentResult.Failed(
                            exception,
                            null
                        )
                    )
                }
                else -> {
                    eventReporter.onPaymentFailure(PaymentSelection.GooglePay)
                    callback.onComplete(
                        PaymentResult.Failed(
                            RuntimeException("Google Pay attempt failed"),
                            null
                        )
                    )
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
