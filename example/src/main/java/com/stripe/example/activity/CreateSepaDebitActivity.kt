package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R

class CreateSepaDebitActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_sepa_debit_pm_layout)
        setTitle(R.string.launch_create_pm_sepa_debit)

        val stripe = Stripe(this,
            PaymentConfiguration.getInstance(this).publishableKey)

        val progressBar: ProgressBar = findViewById(R.id.progress_bar)
        val ibanInput: EditText = findViewById(R.id.iban_input)
        val button: Button = findViewById(R.id.create_sepa_debit_button)
        button.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            stripe.createPaymentMethod(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.SepaDebit(
                        iban = ibanInput.text.toString()
                    ),
                    PaymentMethod.BillingDetails(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL
                    )
                ),
                callback = object : ApiResultCallback<PaymentMethod> {
                    override fun onSuccess(result: PaymentMethod) {
                        progressBar.visibility = View.INVISIBLE
                        Toast.makeText(
                            this@CreateSepaDebitActivity,
                            "Created payment method: " + result.id,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onError(e: Exception) {
                        progressBar.visibility = View.INVISIBLE
                        e.printStackTrace()
                    }
                }
            )
        }
    }

    private companion object {
        private const val CUSTOMER_NAME = "Jenny Rosen"
        private const val CUSTOMER_EMAIL = "jrosen@example.com"
    }
}
