package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.stripe.android.StripeApiBeta
import com.stripe.android.confirmPaymentIntent
import com.stripe.android.confirmSetupIntent
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.example.StripeFactory
import com.stripe.example.module.StripeIntentViewModel
import kotlinx.coroutines.launch

class ManualUSBankAccountPaymentMethodViewModel(
    application: Application
) : StripeIntentViewModel(application) {

    private val stripe by lazy {
        StripeFactory(application, betas = setOf(StripeApiBeta.USBankAccount)).create()
    }

    fun createAndConfirmPaymentIntent(params: PaymentMethodCreateParams) {
        viewModelScope.launch {
            createPaymentIntent(
                country = "us",
                supportedPaymentMethods = "us_bank_account"
            ).asFlow().collect { createResult ->
                createResult.onSuccess { responseData ->
                    val clientSecret = responseData.getString("secret")
                    status.postValue(
                        status.value +
                            "\n\nStarting PaymentIntent confirmation"
                    )
                    runCatching {
                        stripe.confirmPaymentIntent(
                            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                                paymentMethodCreateParams = params,
                                clientSecret = clientSecret
                            )
                        )
                    }.emit { it }
                }
            }
        }
    }

    fun createAndConfirmSetupIntent(params: PaymentMethodCreateParams) {
        viewModelScope.launch {
            createSetupIntent(
                country = "us"
            ).asFlow().collect { createResult ->
                createResult.onSuccess { responseData ->
                    val clientSecret = responseData.getString("secret")
                    status.postValue(
                        status.value +
                            "\n\nStarting SetupIntent confirmation"
                    )
                    runCatching {
                        stripe.confirmSetupIntent(
                            ConfirmSetupIntentParams.create(
                                paymentMethodCreateParams = params,
                                clientSecret = clientSecret
                            )
                        )
                    }.emit { it }
                }
            }
        }
    }

    private fun <T> Result<T>.emit(
        getIntent: (T) -> StripeIntent
    ) {
        fold(
            onSuccess = {
                val intent = getIntent(it)
                if (intent.requiresAction() &&
                    intent.nextActionType == StripeIntent.NextActionType.VerifyWithMicrodeposits ||
                    intent.status == StripeIntent.Status.Processing ||
                    intent.status == StripeIntent.Status.Succeeded
                ) {
                    paymentResultLiveData.postValue(PaymentResult.Completed)
                } else {
                    paymentResultLiveData.postValue(PaymentResult.Canceled)
                }
            },
            onFailure = {
                paymentResultLiveData.postValue(PaymentResult.Failed(it))
            }
        )
    }
}
