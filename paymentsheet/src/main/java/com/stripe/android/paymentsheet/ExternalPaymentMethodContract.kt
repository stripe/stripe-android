package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalArgumentException

internal class ExternalPaymentMethodContract : ActivityResultContract<ExternalPaymentMethodInput, PaymentResult>() {
    override fun createIntent(context: Context, input: ExternalPaymentMethodInput): Intent {
        return input.externalPaymentMethodConfirmHandler.createIntent(
            context = context,
            externalPaymentMethodType = input.type,
            billingDetails = input.billingDetails.toPaymentSheetBillingDetails(),
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return when (resultCode) {
            ExternalPaymentMethodResult.Completed.resultCode -> PaymentResult.Completed
            ExternalPaymentMethodResult.Canceled.resultCode -> PaymentResult.Canceled
            ExternalPaymentMethodResult.Failed.resultCode ->
                PaymentResult.Failed(
                    throwable = Throwable(
                        cause = null,
                        message = intent?.getStringExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA)
                    )
                )

            else ->
                PaymentResult.Failed(
                    throwable = IllegalArgumentException(
                        "Invalid result code returned by external payment method activity"
                    )
                )
        }
    }
}

private fun PaymentMethod.BillingDetails?.toPaymentSheetBillingDetails(): PaymentSheet.BillingDetails {
    if (this == null) {
        return PaymentSheet.BillingDetails()
    }

    val billingDetails = PaymentSheet.BillingDetails.Builder()

    billingDetails.name(this.name)
    billingDetails.phone(this.phone)
    billingDetails.email(this.email)

    if (this.address != null) {
        val address = PaymentSheet.Address.Builder()
        address.line1(this.address?.line1)
        address.line2(this.address?.line2)
        address.city(this.address?.city)
        address.state(this.address?.state)
        address.country(this.address?.country)
        address.postalCode(this.address?.postalCode)
        billingDetails.address(address)
    }

    return billingDetails.build()
}

internal data class ExternalPaymentMethodInput(
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
    val type: String,
    val billingDetails: PaymentMethod.BillingDetails?,
)
