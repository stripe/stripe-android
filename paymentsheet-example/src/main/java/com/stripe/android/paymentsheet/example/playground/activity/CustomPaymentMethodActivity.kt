package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
class CustomPaymentMethodActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customPaymentMethodType: PaymentSheet.CustomPaymentMethod? =
            intent.getParcelableExtra(EXTRA_CUSTOM_PAYMENT_METHOD_TYPE)

        if (customPaymentMethodType == null) {
            onCustomPaymentMethodResult(
                context = this,
                customPaymentMethodResult = CustomPaymentMethodResult.failed(
                    displayMessage = "No custom payment method type provided!",
                ),
            )

            return
        }

        val billingDetails: PaymentMethod.BillingDetails? = intent.getParcelableExtra(EXTRA_BILLING_DETAILS)

        setContent {
            PaymentSheetExampleTheme {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Title(customPaymentMethodType = customPaymentMethodType)
                    if (billingDetails != null) {
                        BillingDetails(billingDetails = billingDetails)
                    }
                    ResultButton(
                        result = CustomPaymentMethodResult.completed(),
                        testTag = COMPLETED_BUTTON_TEST_TAG,
                    )
                    ResultButton(
                        result = CustomPaymentMethodResult.canceled(),
                        testTag = CANCELED_BUTTON_TEST_TAG,
                    )
                    ResultButton(
                        result = CustomPaymentMethodResult.failed(displayMessage = FAILED_DISPLAY_MESSAGE),
                        testTag = FAILED_BUTTON_TEST_TAG,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
            context = this,
            customPaymentMethodResult = CustomPaymentMethodResult.canceled(),
        )
    }

    private fun onCustomPaymentMethodResult(
        context: Context,
        customPaymentMethodResult: CustomPaymentMethodResult,
    ) {
        CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
            context = context,
            customPaymentMethodResult = customPaymentMethodResult,
        )

        finish()
    }

    @Composable
    fun ResultButton(result: CustomPaymentMethodResult, testTag: String) {
        Button(onClick = { onCustomPaymentMethodResult(this, result) }, modifier = Modifier.testTag(testTag)) {
            Text(text = result.toString())
        }
    }

    @Composable
    fun BillingDetails(billingDetails: PaymentMethod.BillingDetails) {
        Text("Billing details: $billingDetails")
    }

    @Composable
    fun Title(
        customPaymentMethodType: PaymentSheet.CustomPaymentMethod,
    ) {
        Text(customPaymentMethodType.id)
    }

    companion object {
        const val EXTRA_CUSTOM_PAYMENT_METHOD_TYPE = "extra_custom_payment_method_type"
        const val EXTRA_BILLING_DETAILS = "extra_custom_payment_method_billing_details"

        const val COMPLETED_BUTTON_TEST_TAG = "cpm_complete"
        const val CANCELED_BUTTON_TEST_TAG = "cpm_canceled"
        const val FAILED_BUTTON_TEST_TAG = "cpm_failed"
        const val FAILED_DISPLAY_MESSAGE = "Custom payment failed!"
    }
}
