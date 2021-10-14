package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.model.PaymentOption

internal class LaunchPaymentSheetCustomActivity : BasePaymentSheetActivity() {
    private lateinit var flowController: PaymentSheet.FlowController

    private val isLoading = MutableLiveData(true)
    private val paymentCompleted = MutableLiveData(false)
    private val selectedPaymentMethod = MutableLiveData<PaymentOption?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        flowController = PaymentSheet.FlowController.create(
            this,
            ::onPaymentOption,
            ::onPaymentSheetResult
        )

        val selectedPaymentMethodLabel = selectedPaymentMethod.map {
            it?.label ?: resources.getString(R.string.select)
        }
        val selectedPaymentMethodIcon = selectedPaymentMethod.map {
            it?.drawableResourceId
        }

        setContent {
            MaterialTheme {
                val isLoadingState by isLoading.observeAsState(true)
                val paymentCompletedState by paymentCompleted.observeAsState(false)
                val status by viewModel.status.observeAsState("")
                val paymentMethodLabel by selectedPaymentMethodLabel.observeAsState(stringResource(R.string.loading))
                val paymentMethodIcon by selectedPaymentMethodIcon.observeAsState()

                if (status.isNotBlank()) {
                    Toast.makeText(LocalContext.current, status, Toast.LENGTH_SHORT).show()
                    viewModel.statusDisplayed()
                }

                Receipt(isLoadingState) {
                    PaymentMethodSelector(
                        isEnabled = !isLoadingState && !paymentCompletedState,
                        paymentMethodLabel = paymentMethodLabel,
                        paymentMethodIcon = paymentMethodIcon,
                        onClick = {
                            flowController.presentPaymentOptions()
                        }
                    )
                    BuyButton(
                        buyButtonEnabled = !isLoadingState && !paymentCompletedState &&
                            selectedPaymentMethod.value != null,
                        onClick = {
                            isLoading.value = true
                            flowController.confirm()
                        }
                    )
                }
            }
        }

        prepareCheckout { customerConfig, clientSecret ->
            flowController.configureWithPaymentIntent(
                clientSecret,
                makeConfiguration(customerConfig),
                ::onConfigured
            )
        }
    }

    private fun makeConfiguration(
        customerConfig: PaymentSheet.CustomerConfiguration? = null
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = merchantName,
            customer = customerConfig,
            googlePay = googlePayConfig,
            allowsDelayedPaymentMethods = true
        )
    }

    private fun onConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            onPaymentOption(flowController.getPaymentOption())
        } else {
            viewModel.status.postValue(
                "Failed to configure PaymentSheetFlowController: ${error?.message}"
            )
        }
    }

    private fun onPaymentOption(paymentOption: PaymentOption?) {
        isLoading.value = false
        selectedPaymentMethod.value = paymentOption
    }

    override fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        super.onPaymentSheetResult(paymentResult)

        isLoading.value = false
        if (paymentResult !is PaymentSheetResult.Canceled) {
            paymentCompleted.value = true
        }
    }
}
