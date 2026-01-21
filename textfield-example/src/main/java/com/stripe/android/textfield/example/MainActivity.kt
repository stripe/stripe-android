package com.stripe.android.textfield.example

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Builder
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private var customerConfig: PaymentSheet.CustomerConfiguration? = null
    private var paymentIntentClientSecret: String? = null

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        paymentSheet = Builder(::onPaymentSheetResult).build(this)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            fetchFromServer()
        }
    }

    private fun fetchFromServer() {
        lifecycleScope.launch {
            CheckoutRequester.fetchCheckoutData()
                .onSuccess { response ->
                    paymentIntentClientSecret = response.paymentIntentClientSecret

                    // Set up customer config if customer data is available
                    if (response.customerId != null && response.ephemeralKeySecret != null) {
                        customerConfig = PaymentSheet.CustomerConfiguration(
                            response.customerId,
                            response.ephemeralKeySecret
                        )
                    }

                    PaymentConfiguration.init(this@MainActivity, response.publishableKey)
                    presentPaymentSheet()
                }
                .onFailure { error ->
                    showAlert("Failed to load: ${error.message}")
                }
        }
    }



    private fun presentPaymentSheet() {
        val clientSecret = paymentIntentClientSecret ?: return

        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "My Business Merch",
                customer = customerConfig,
                allowsDelayedPaymentMethods = true
            )
        )
    }

    fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                showAlert("Payment cancelled")
            }
            is PaymentSheetResult.Failed -> {
                showAlert("Payment failed ${paymentSheetResult.error.message}")
            }
            is PaymentSheetResult.Completed -> {
                showAlert("Payment completed successfully")
            }
        }
    }

    fun showAlert(message: String) {
        AlertDialog.Builder(this).setTitle("Alert").setMessage(message)
            .setPositiveButton("OK", null).show()
    }
}
