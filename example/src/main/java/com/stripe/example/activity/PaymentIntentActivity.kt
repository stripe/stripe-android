package com.stripe.example.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.StripeFactory
import com.stripe.example.module.PaymentIntentViewModel
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Base class for Activity's that wish to create and confirm payment methods.
 * Subclasses should observe on the [PaymentIntentViewModel]'s LiveData properties
 * in order to display state of the interaction.
 */
abstract class PaymentIntentActivity : AppCompatActivity() {
    internal val viewModel: PaymentIntentViewModel by lazy {
        ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[PaymentIntentViewModel::class.java]
    }
    protected val stripeAccountId: String? by lazy {
        Settings(this).stripeAccountId
    }
    protected val stripe: Stripe by lazy {
        StripeFactory(this, stripeAccountId).create()
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        returnUrl: String = "example://return_url"
    ) {
        keyboardController.hide()

        viewModel.createPaymentIntent(country) {
            handleCreatePaymentIntentResponse(it, paymentMethodCreateParams, shippingDetails, stripeAccountId, returnUrl)
        }
    }

    protected fun createAndConfirmSetupIntent(
        country: String,
        params: PaymentMethodCreateParams,
        stripeAccountId: String? = null,
        returnUrl: String = "example://return_url"
    ) {
        keyboardController.hide()

        viewModel.createSetupIntent(country) {
            handleCreateSetupIntentResponse(it, params, stripeAccountId, returnUrl)
        }
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        returnUrl: String
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting PaymentIntent confirmation" + (stripeAccountId?.let {
                " for $it"
            } ?: ""))
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = secret,
                shipping = shippingDetails,
                returnUrl = returnUrl
            ),
            stripeAccountId
        )
    }

    fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        stripeAccountId: String?,
        returnUrl: String
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting SetupIntent confirmation" + (stripeAccountId?.let {
                " for $it"
            } ?: ""))
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret,
                returnUrl = returnUrl
            ),
            stripeAccountId
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        keyboardController.hide()

        viewModel.status.value += "\n\nPayment authentication completed, getting result"
        val isPaymentResult =
            stripe.onPaymentResult(requestCode, data, PaymentIntentResultCallback(this))
        if (!isPaymentResult) {
            stripe.onSetupResult(requestCode, data, SetupIntentResultCallback(this))
        }
    }

    protected fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewModel.status.value += "\n\n" +
            "PaymentIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        viewModel.inProgress.value = false
    }

    protected fun onConfirmSuccess(result: SetupIntentResult) {
        val setupIntentResult = result.intent
        viewModel.status.value += "\n\n" +
            "SetupIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.setup_intent_status, setupIntentResult.status)
        viewModel.inProgress.value = false
    }

    protected fun onConfirmError(e: Exception) {
        viewModel.status.value += "\n\nException: " + e.message
        viewModel.inProgress.value = false
    }

    internal class PaymentIntentResultCallback(
        activity: PaymentIntentActivity
    ) : ApiResultCallback<PaymentIntentResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: PaymentIntentResult) {
            activityRef.get()?.onConfirmSuccess(result)
        }

        override fun onError(e: Exception) {
            activityRef.get()?.onConfirmError(e)
        }
    }

    internal class SetupIntentResultCallback(
        activity: PaymentIntentActivity
    ) : ApiResultCallback<SetupIntentResult> {

        private val activityRef = WeakReference(activity)

        override fun onSuccess(result: SetupIntentResult) {
            activityRef.get()?.onConfirmSuccess(result)
        }

        override fun onError(e: Exception) {
            activityRef.get()?.onConfirmError(e)
        }
    }
}
