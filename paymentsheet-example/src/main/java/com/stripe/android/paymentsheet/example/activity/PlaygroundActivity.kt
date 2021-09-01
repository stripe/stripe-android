package com.stripe.android.paymentsheet.example.activity

import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity

internal class Playground : PaymentSheetPlaygroundActivity() {
    internal override fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        println("Hello world.")
    }
}
