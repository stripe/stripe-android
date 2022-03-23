package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.Settings
import com.stripe.example.module.StripeIntentViewModel
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

    private lateinit var paymentLauncher: PaymentLauncher

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentLauncher =
            PaymentLauncher.create(
                this,
                PaymentConfiguration.getInstance(this).publishableKey,
                stripeAccountId
            ) { paymentResult ->
                if (viewModel.status.value.isNullOrEmpty()) {
                    viewModel.status.value =
                        """
                        Restored from a killed process...
                        
                        Payment authentication completed, getting result
                        """.trimIndent()
                } else {
                    viewModel.status.value += "\n\nPayment authentication completed, getting result"
                }
                viewModel.paymentResultLiveData.postValue(paymentResult)
            }

        viewModel.paymentResultLiveData
            .observe(
                this,
                {
                    when (it) {
                        is PaymentResult.Completed -> {
                            onConfirmSuccess()
                        }
                        is PaymentResult.Canceled -> {
                            onConfirmCanceled()
                        }
                        is PaymentResult.Failed -> {
                            onConfirmError(it)
                        }
                    }
                }
            )
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        supportedPaymentMethods: String? = null,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        existingPaymentMethodId: String? = null,
        mandateDataParams: MandateDataParams? = null,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        requireNotNull(paymentMethodCreateParams ?: existingPaymentMethodId)

        keyboardController.hide()

        viewModel.createPaymentIntent(
            country = country,
            supportedPaymentMethods = supportedPaymentMethods
        ).observe(
            this,
            { result ->
                result.onSuccess {
                    handleCreatePaymentIntentResponse(
                        it,
                        paymentMethodCreateParams,
                        shippingDetails,
                        stripeAccountId,
                        existingPaymentMethodId,
                        mandateDataParams,
                        onPaymentIntentCreated
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
        mandateDataParams: MandateDataParams?,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        val secret = responseData.getString("secret")
        onPaymentIntentCreated(secret)
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
        confirmPaymentIntent(confirmPaymentIntentParams)
        paymentLauncher.confirm(confirmPaymentIntentParams)
    }

    protected fun confirmPaymentIntent(params: ConfirmPaymentIntentParams) {
        paymentLauncher.confirm(params)
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
        paymentLauncher.confirm(
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret
            )
        )
    }

    protected open fun onConfirmSuccess() {
        viewModel.status.value += "\n\nPaymentIntent confirmation succeeded\n\n"
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmCanceled() {
        viewModel.status.value += "\n\nPaymentIntent confirmation cancelled\n\n"
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmError(failedResult: PaymentResult.Failed) {
        viewModel.status.value += "\n\nPaymentIntent confirmation failed with throwable " +
            "${failedResult.throwable} \n\n"
        viewModel.inProgress.value = false
    }
}
