package com.stripe.android.paymentsheet.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.databinding.ActivityEspressoCustomLauncherBinding


class EspressoCustomLauncher : AppCompatActivity() {

    val viewBinding by lazy {
        ActivityEspressoCustomLauncherBinding.inflate(layoutInflater)
    }

    lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        paymentSheet = PaymentSheet(
            this,
            ::onPaymentSheetResult
        )
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            PaymentSheetResult.Canceled -> {}
            PaymentSheetResult.Completed -> {}
            is PaymentSheetResult.Failed -> {}
        }
    }
}
