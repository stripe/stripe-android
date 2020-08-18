package com.stripe.example.activity

import android.os.Bundle
import androidx.lifecycle.Observer
import com.stripe.android.view.BecsDebitWidget
import com.stripe.example.databinding.BecsDebitActivityBinding

class BecsDebitPaymentMethodActivity : StripeIntentActivity() {
    private val viewBinding: BecsDebitActivityBinding by lazy {
        BecsDebitActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.element.validParamsCallback = object : BecsDebitWidget.ValidParamsCallback {
            override fun onInputChanged(isValid: Boolean) {
                viewBinding.payButton.isEnabled = isValid
                viewBinding.saveButton.isEnabled = isValid
            }
        }

        viewBinding.payButton.setOnClickListener {
            viewBinding.element.params?.let { createAndConfirmPaymentIntent("au", it) }
        }

        viewBinding.saveButton.setOnClickListener {
            viewBinding.element.params?.let { createAndConfirmSetupIntent("au", it) }
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.payButton.isEnabled = enabled
        viewBinding.saveButton.isEnabled = enabled
    }
}
