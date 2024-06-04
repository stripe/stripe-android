package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ExternalPaymentMethodResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodResultHandler
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

class FawryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE) != "external_fawry") {
            ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult(
                this,
                externalPaymentMethodResult = ExternalPaymentMethodResult.failed(
                    displayMessage = "Received invalid external payment method type. Expected external_fawry."
                ),
            )
        }

        val billingDetails: PaymentMethod.BillingDetails? =
            intent.getParcelableExtra(EXTRA_BILLING_DETAILS)

        setContent {
            PaymentSheetExampleTheme {
                Column {
                    if (billingDetails != null) {
                        BillingDetails(billingDetails = billingDetails)
                    }
                    ResultButton(result = ExternalPaymentMethodResult.completed(), testTag = COMPLETED_BUTTON_TEST_TAG)
                    ResultButton(result = ExternalPaymentMethodResult.canceled(), testTag = CANCELED_BUTTON_TEST_TAG)
                    ResultButton(
                        result = ExternalPaymentMethodResult.failed(displayMessage = FAILED_DISPLAY_MESSAGE),
                        testTag = FAILED_BUTTON_TEST_TAG
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult(this, ExternalPaymentMethodResult.canceled())
    }

    private fun onExternalPaymentMethodResult(
        context: Context,
        externalPaymentMethodResult: ExternalPaymentMethodResult
    ) {
        ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult(context, externalPaymentMethodResult)
        finish()
    }

    @Composable
    fun ResultButton(result: ExternalPaymentMethodResult, testTag: String) {
        Button(onClick = { onExternalPaymentMethodResult(this, result) }, modifier = Modifier.testTag(testTag)) {
            Text(text = result.toString())
        }
    }

    @Composable
    fun BillingDetails(billingDetails: PaymentMethod.BillingDetails) {
        Text("Billing details: $billingDetails", color = Color.White)
    }

    companion object {
        const val EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE = "external_payment_method_type"
        const val EXTRA_BILLING_DETAILS = "external_payment_method_billing_details"

        const val COMPLETED_BUTTON_TEST_TAG = "external_fawry_complete"
        const val CANCELED_BUTTON_TEST_TAG = "external_fawry_canceled"
        const val FAILED_BUTTON_TEST_TAG = "external_fawry_failed"
        const val FAILED_DISPLAY_MESSAGE = "Payment failed!"
    }
}
