package com.stripe.example.activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.NetbankingPaymentActivityBinding

class NetbankingPaymentActivity : StripeIntentActivity(){
    private val stripe by lazy {
        StripeFactory(application).create()
    }
    private val viewBinding: NetbankingPaymentActivityBinding by lazy {
        NetbankingPaymentActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val banks = arrayOf("hdfc", "icici")
        viewBinding.autoCompleteTextView.threshold = 1

        val arrayAdapter = ArrayAdapter(applicationContext, 0, banks)
        viewBinding.autoCompleteTextView.setAdapter(arrayAdapter)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(
            requestCode,
            data,
            object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(
                    result: PaymentIntentResult
                ) {
                    val paymentIntent = result.intent
                }

                override fun onError(e: Exception) {
                    // TODO: remove this print statement. Check what is the best way to handle this
                    // To reach this path use vpa = payment.failure@stripeupi
                    print("Payment failed")
                }
            }
        )
    }

    internal companion object {
        const val EXTRA_CLIENT_SECRET = "extra_client_secret"
    }
}