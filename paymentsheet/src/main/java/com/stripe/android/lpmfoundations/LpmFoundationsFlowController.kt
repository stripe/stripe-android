package com.stripe.android.lpmfoundations

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentOption

internal class LpmFoundationsFlowController : PaymentSheet.FlowController {
    override var shippingDetails: AddressDetails?
        get() = TODO("Not yet implemented")
        set(_) {
            TODO("Not yet implemented")
        }

    override fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        TODO("Not yet implemented")
    }

    override fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        TODO("Not yet implemented")
    }

    override fun configureWithIntentConfiguration(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback
    ) {
        TODO("Not yet implemented")
    }

    override fun getPaymentOption(): PaymentOption? {
        TODO("Not yet implemented")
    }

    override fun presentPaymentOptions() {
        TODO("Not yet implemented")
    }

    override fun confirm() {
        TODO("Not yet implemented")
    }
}
