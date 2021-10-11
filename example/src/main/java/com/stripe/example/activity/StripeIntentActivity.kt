package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.getPaymentIntentResult
import com.stripe.android.getSetupIntentResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.StripeFactory
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Base class for Activity's that wish to create and confirm payment methods.
 * Subclasses should observe on the [StripeIntentViewModel]'s LiveData properties
 * in order to display state of the interaction.
 */
abstract class StripeIntentActivity : AppCompatActivity() {
    internal val viewModel: StripeIntentViewModel by viewModels()
    private val stripeAccountId: String? by lazy {
        Settings(this).stripeAccountId
    }
    protected val stripe: Stripe by lazy {
        StripeFactory(this, stripeAccountId).create()
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.paymentIntentResultLiveData
            .observe(
                this,
                {
                    it.fold(
                        onSuccess = ::onConfirmSuccess,
                        onFailure = ::onConfirmError
                    )
                }
            )

        viewModel.setupIntentResultLiveData
            .observe(
                this,
                {
                    it.fold(
                        onSuccess = ::onConfirmSuccess,
                        onFailure = ::onConfirmError
                    )
                }
            )
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        existingPaymentMethodId: String? = null,
        mandateDataParams: MandateDataParams? = null
    ) {
        requireNotNull(paymentMethodCreateParams ?: existingPaymentMethodId)

        keyboardController.hide()

        viewModel.createPaymentIntent(country).observe(
            this,
            { result ->
                result.onSuccess {
                    handleCreatePaymentIntentResponse(
                        it,
                        paymentMethodCreateParams,
                        shippingDetails,
                        stripeAccountId,
                        existingPaymentMethodId,
                        mandateDataParams
                    )
                }
            }
        )
    }

    protected fun createAndConfirmSetupIntent(
        country: String,
        params: PaymentMethodCreateParams,
        stripeAccountId: String? = null
    ) {
        keyboardController.hide()

        viewModel.createSetupIntent(country).observe(
            this,
            { result ->
                result.onSuccess {
                    handleCreateSetupIntentResponse(it, params, stripeAccountId)
                }
            }
        )
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting PaymentIntent confirmation" + (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )
        )
        val confirmPaymentIntentParams = if (existingPaymentMethodId == null) {
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = requireNotNull(params),
                clientSecret = secret,
                shipping = shippingDetails
            )
        } else {
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = existingPaymentMethodId,
                clientSecret = secret,
                mandateData = mandateDataParams
            )
        }
        stripe.confirmPayment(this, confirmPaymentIntentParams, stripeAccountId)
    }

    private fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        stripeAccountId: String?
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting SetupIntent confirmation" + (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )
        )
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret
            ),
            stripeAccountId
        )
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        keyboardController.hide()
//
//        viewModel.status.value += "\n\nPayment authentication completed, getting result"
//        if (stripe.isPaymentResult(requestCode, data)) {
//            lifecycleScope.launch {
//                viewModel.paymentIntentResultLiveData.value = runCatching {
//                    // stripe.isPaymentResult already verifies data is not null
//                    stripe.getPaymentIntentResult(requestCode, data!!)
//                }
//            }
//        } else if (stripe.isSetupResult(requestCode, data)) {
//            lifecycleScope.launch {
//                viewModel.setupIntentResultLiveData.value = runCatching {
//                    // stripe.isSetupResult already verifies data is not null
//                    stripe.getSetupIntentResult(requestCode, data!!)
//                }
//            }
//        }
//    }

    protected open fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewModel.status.value += "\n\n" +
            "PaymentIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmSuccess(result: SetupIntentResult) {
        val setupIntentResult = result.intent
        viewModel.status.value += "\n\n" +
            "SetupIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.setup_intent_status, setupIntentResult.status)
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmError(throwable: Throwable) {
        viewModel.status.value += "\n\nException: " + throwable.message
        viewModel.inProgress.value = false
    }
}
