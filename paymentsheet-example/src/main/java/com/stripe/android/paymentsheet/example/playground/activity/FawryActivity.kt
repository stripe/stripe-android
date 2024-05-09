package com.stripe.android.paymentsheet.example.playground.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
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
                    errorMessage = "Received invalid external payment method type. Expected external_fawry."
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
                    ResultButton(result = ExternalPaymentMethodResult.completed())
                    ResultButton(result = ExternalPaymentMethodResult.canceled())
                    ResultButton(result = ExternalPaymentMethodResult.failed(errorMessage = "Payment failed!"))
                }
            }
        }
    }

    private fun onExternalPaymentMethodResult(
        context: Context,
        externalPaymentMethodResult: ExternalPaymentMethodResult
    ) {
        ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult(context, externalPaymentMethodResult)
        finish()
    }

    @Composable
    fun ResultButton(result: ExternalPaymentMethodResult) {
        Button(onClick = { onExternalPaymentMethodResult(this, result) }) {
            Text(text = result.toString())
        }
    }

    @Composable
    fun BillingDetails(billingDetails: PaymentMethod.BillingDetails) {
        Text("Billing details: $billingDetails", color = Color.White)
    }

    companion object {
        private const val EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE = "external_payment_method_type"
        private const val EXTRA_BILLING_DETAILS = "external_payment_method_billing_details"
    }

    class FawryConfirmHandler(private val context: Context) : ExternalPaymentMethodConfirmHandler {

        override fun confirmExternalPaymentMethod(
            externalPaymentMethodType: String,
            billingDetails: PaymentMethod.BillingDetails
        ) {
            context.startActivity(
                Intent().setClass(
                    context,
                    FawryActivity::class.java
                )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, externalPaymentMethodType)
                    .putExtra(EXTRA_BILLING_DETAILS, billingDetails)
            )
        }
    }
}
