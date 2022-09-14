package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.alipay.sdk.app.PayTask
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.example.R
import com.stripe.example.databinding.PaymentExampleActivityBinding
import org.json.JSONObject

class AlipayPaymentActivity : StripeIntentActivity() {

    private val viewBinding: PaymentExampleActivityBinding by lazy {
        PaymentExampleActivityBinding.inflate(layoutInflater)
    }

    private val stripe: Stripe by lazy {
        Stripe(
            applicationContext,
            PaymentConfiguration.getInstance(applicationContext).publishableKey
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.confirmWithPaymentButton.text =
            resources.getString(R.string.confirm_alipay_button)
        viewBinding.paymentExampleIntro.text =
            resources.getString(R.string.alipay_example_intro)

        viewModel.inProgress.observe(this) { enableUi(!it) }
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.confirmWithPaymentButton.setOnClickListener {
            createAndConfirmPaymentIntent(
                country = "US",
                paymentMethodCreateParams = PaymentMethodCreateParams.createAlipay(),
                supportedPaymentMethods = "alipay"
            )
        }
    }

    override fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?,
        onPaymentIntentCreated: (String) -> Unit
    ) {
        val secret = responseData.getString("secret")
        onPaymentIntentCreated(secret)
        viewModel.status.value +=
            "\n\nStarting PaymentIntent confirmation" +
            (
                stripeAccountId?.let {
                    " for $it"
                } ?: ""
                )

        stripe.confirmAlipayPayment(
            confirmPaymentIntentParams = ConfirmPaymentIntentParams.createAlipay(secret),
            authenticator = { data ->
                PayTask(this).payV2(data, true)
            },
            callback = object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    val paymentIntent = result.intent
                    when (paymentIntent.status) {
                        StripeIntent.Status.Succeeded ->
                            updateStatus("\n\nPayment succeeded")
                        StripeIntent.Status.RequiresAction ->
                            stripe.handleNextActionForPayment(this@AlipayPaymentActivity, secret)
                        else -> updateStatus("\n\nPayment failed or canceled")
                    }
                }

                override fun onError(e: Exception) {
                    updateStatus("\n\nError: ${e.message}")
                }
            }
        )
    }

    private fun updateStatus(appendMessage: String) {
        viewModel.status.value += appendMessage
        viewModel.inProgress.postValue(false)
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.confirmWithPaymentButton.isEnabled = enable
    }
}
