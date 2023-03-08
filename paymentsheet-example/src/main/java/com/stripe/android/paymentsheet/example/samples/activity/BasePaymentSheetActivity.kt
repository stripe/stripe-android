package com.stripe.android.paymentsheet.example.samples.activity

import android.graphics.Color
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.viewmodel.PaymentSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal abstract class BasePaymentSheetActivity : AppCompatActivity() {
    protected val viewModel: PaymentSheetViewModel by viewModels()

    protected val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
        .setBackgroundTint(Color.BLACK)
        .setTextColor(Color.WHITE)
    }

    protected fun prepareCheckout(
        onSuccess: (PaymentSheet.CustomerConfiguration?, String) -> Unit
    ) {
        viewModel.prepareCheckout(backendUrl)

        viewModel.exampleCheckoutResponse.observe(this) { checkoutResponse ->
            // Init PaymentConfiguration with the publishable key returned from the backend,
            // which will be used on all Stripe API calls
            val activity = this
            lifecycleScope.launch(Dispatchers.IO) {
                PaymentConfiguration.init(activity, checkoutResponse.publishableKey)

                launch(Dispatchers.Main) {
                    onSuccess(
                        checkoutResponse.makeCustomerConfig(),
                        checkoutResponse.paymentIntent
                    )
                }
            }

            viewModel.exampleCheckoutResponse.removeObservers(this)
        }
    }

    protected open fun onPaymentSheetResult(
        paymentResult: PaymentSheetResult
    ) {
        viewModel.status.value = when (paymentResult) {
            is PaymentSheetResult.Canceled -> null
            is PaymentSheetResult.Completed -> "Success"
            is PaymentSheetResult.Failed -> paymentResult.error.message
        }
    }

    companion object {
        const val merchantName = "Example, Inc."
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me/checkout"
        val googlePayConfig = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "US"
        )
    }
}
