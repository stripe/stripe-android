package com.stripe.example.activity

import android.app.Application
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

class USBankAccountPaymentMethodViewModel(
    application: Application
) : StripeIntentViewModel(application) {

    private val stripe by lazy {
        StripeFactory(application, betas = setOf(StripeApiBeta.USBankAccount)).create()
    }

    fun confirmPaymentIntent(
        params: PaymentMethodCreateParams,
        clientSecret: String
    ) {
        status.postValue(
            status.value +
                "\n\nStarting PaymentIntent confirmation"
        )
        viewModelScope.launch {
            val result = runCatching {
                stripe.confirmPaymentIntent(
                    ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = params,
                        clientSecret = clientSecret
                    )
                )
            }
            result.fold(
                onSuccess = { paymentIntent ->
                    if (paymentIntent.requiresAction() &&
                        paymentIntent.nextActionType ==
                        StripeIntent.NextActionType.VerifyWithMicrodeposits
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

    fun confirmSetupIntent(
        params: PaymentMethodCreateParams,
        clientSecret: String
    ) {
        status.postValue(
            status.value +
                "\n\nStarting SetupIntent confirmation"
        )
        viewModelScope.launch {
            val result = runCatching {
                stripe.confirmSetupIntent(
                    ConfirmSetupIntentParams.create(
                        paymentMethodCreateParams = params,
                        clientSecret = clientSecret
                    )
                )
            }
            result.fold(
                onSuccess = { setupIntent ->
                    if (setupIntent.requiresAction() &&
                        setupIntent.nextActionType ==
                        StripeIntent.NextActionType.VerifyWithMicrodeposits
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
}
