package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.BecsDebitWidget
import com.stripe.example.databinding.BecsDebitActivityBinding

class BecsDebitPaymentMethodActivity : AppCompatActivity() {
    private val stripe: Stripe by lazy {
        Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
    }
    private val viewBinding: BecsDebitActivityBinding by lazy {
        BecsDebitActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.element.validParamsCallback = object : BecsDebitWidget.ValidParamsCallback {
            override fun onInputChanged(isValid: Boolean) {
                viewBinding.submit.isEnabled = isValid
            }
        }

        viewBinding.submit.setOnClickListener {
            viewBinding.element.params?.let { params ->
                keyboardController.hide()

                stripe.createPaymentMethod(
                    paymentMethodCreateParams = params,
                    callback = object : ApiResultCallback<PaymentMethod> {
                        override fun onSuccess(result: PaymentMethod) {
                            viewBinding.status.text = result.toString()
                        }

                        override fun onError(e: Exception) {
                            viewBinding.status.text = e.message
                        }
                    }
                )
            }
        }
    }
}
