package com.stripe.example.activity

import android.os.Bundle
import androidx.core.view.isInvisible
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.example.databinding.ActivityPaymentSheetCompleteBinding

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCompleteBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.isInvisible = !it
            viewBinding.launch.isEnabled = !it
        }

        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.launch.setOnClickListener {
            prepareCheckout { customerConfig, clientSecret ->
                if (isSetupIntent) {
                    paymentSheet.presentWithSetupIntent(
                        clientSecret,
                        PaymentSheet.Configuration(
                            merchantDisplayName = merchantName,
                            customer = customerConfig,
                            googlePay = googlePayConfig,
                        )
                    )
                } else {
                    paymentSheet.presentWithPaymentIntent(
                        clientSecret,
                        PaymentSheet.Configuration(
                            merchantDisplayName = merchantName,
                            customer = customerConfig,
                            googlePay = googlePayConfig,
                        )
                    )
                }
            }
        }
    }
}
