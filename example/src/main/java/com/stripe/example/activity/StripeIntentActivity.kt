package com.stripe.example.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
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

    lateinit var paymentLauncher: PaymentLauncher
    private var isPaymentIntent: Boolean = true

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
                viewModel.paymentResultLiveData.postValue(paymentResult)
            }

        viewModel.paymentResultLiveData
            .observe(
                this
            ) {
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
    }

    protected fun createAndConfirmPaymentIntent(
        country: String,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        supportedPaymentMethods: String? = null,
        shippingDetails: ConfirmPaymentIntentParams.Shipping? = null,
        stripeAccountId: String? = null,
        existingPaymentMethodId: String? = null,
        mandateDataParams: MandateDataParams? = null,
        customerId: String? = null,
        setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage? = null,
        currency: String? = null,
        paymentMethodOptions: PaymentMethodOptionsParams? = null,
        onPaymentIntentCreated: (String) -> Unit = {}
    ) {
        requireNotNull(paymentMethodCreateParams ?: existingPaymentMethodId)

        keyboardController.hide()

        viewModel.createPaymentIntent(
            country = country,
            supportedPaymentMethods = supportedPaymentMethods,
            customerId = customerId,
            currency = currency,
        ).observe(
            this
        ) { result ->
            result.onSuccess {
                handleCreatePaymentIntentResponse(
                    it,
                    paymentMethodCreateParams,
                    shippingDetails,
                    stripeAccountId,
                    existingPaymentMethodId,
                    mandateDataParams,
                    setupFutureUsage,
                    paymentMethodOptions,
                    onPaymentIntentCreated
                )
            }
        }
    }

    protected fun createAndConfirmSetupIntent(
        country: String,
        params: PaymentMethodCreateParams,
        supportedPaymentMethods: String? = null,
        mandateData: MandateDataParams? = null,
        customerId: String? = null,
        stripeAccountId: String? = null,
        onSetupIntentCreated: (String) -> Unit = {}
    ) {
        keyboardController.hide()

        viewModel.createSetupIntent(
            country = country,
            supportedPaymentMethods = supportedPaymentMethods,
            customerId = customerId,
        ).observe(
            this
        ) { result ->
            result.onSuccess {
                handleCreateSetupIntentResponse(
                    it,
                    params,
                    stripeAccountId,
                    mandateData,
                    onSetupIntentCreated
                )
            }
        }
    }

    open fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams?,
        shippingDetails: ConfirmPaymentIntentParams.Shipping?,
        stripeAccountId: String?,
        existingPaymentMethodId: String?,
        mandateDataParams: MandateDataParams?,
        setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?,
        paymentMethodOptions: PaymentMethodOptionsParams?,
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
                shipping = shippingDetails,
                setupFutureUsage = setupFutureUsage,
                paymentMethodOptions = paymentMethodOptions,
            ).copy(
                mandateData = mandateDataParams,
            )
        } else {
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = existingPaymentMethodId,
                clientSecret = secret,
                mandateData = mandateDataParams,
                setupFutureUsage = setupFutureUsage,
                paymentMethodOptions = paymentMethodOptions,
            )
        }
        confirmPaymentIntent(confirmPaymentIntentParams)
    }

    protected fun confirmPaymentIntent(params: ConfirmPaymentIntentParams) {
        isPaymentIntent = true
        paymentLauncher.confirm(params)
    }

    fun createPaymentMethod(params: PaymentMethodCreateParams) {
        viewModel.createPaymentMethod(params)
    }

    private fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams,
        stripeAccountId: String?,
        mandateData: MandateDataParams?,
        onSetupIntentCreated: (String) -> Unit = {}
    ) {
        val secret = responseData.getString("secret")
        onSetupIntentCreated(secret)
        viewModel.status.postValue(
            viewModel.status.value +
                "\n\nStarting SetupIntent confirmation" + (
                    stripeAccountId?.let {
                        " for $it"
                    } ?: ""
                    )
        )
        confirmSetupIntent(
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret,
                mandateData = mandateData,
            )
        )
    }

    protected fun confirmSetupIntent(params: ConfirmSetupIntentParams) {
        isPaymentIntent = false
        paymentLauncher.confirm(params)
    }

    protected open fun onConfirmSuccess() {
        viewModel.status.value +=
            "\n\n${if (isPaymentIntent)"Payment" else "Setup"}Intent confirmation succeeded\n\n"
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmCanceled() {
        viewModel.status.value +=
            "\n\n${if (isPaymentIntent)"Payment" else "Setup"}Intent confirmation canceled\n\n"
        viewModel.inProgress.value = false
    }

    protected open fun onConfirmError(failedResult: PaymentResult.Failed) {
        viewModel.status.value +=
            "\n\n${if (isPaymentIntent)"Payment" else "Setup"}Intent confirmation failed with " +
            "throwable ${failedResult.throwable} \n\n"
        viewModel.inProgress.value = false
    }
}
