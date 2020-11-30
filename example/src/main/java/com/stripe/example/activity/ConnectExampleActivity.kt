package com.stripe.example.activity

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.stripe.example.databinding.ConnectExampleActivityBinding

class ConnectExampleActivity : StripeIntentActivity() {
    private val viewBinding: ConnectExampleActivityBinding by lazy {
        ConnectExampleActivityBinding.inflate(layoutInflater)
    }
    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        viewBinding.payNow.setOnClickListener {
            viewBinding.cardWidget.paymentMethodCreateParams?.let {
                val connectAccount =
                    viewBinding.connectAccount.text.toString().takeIf(String::isNotBlank)
                createAndConfirmPaymentIntent("us", it, stripeAccountId = connectAccount)
            } ?: showSnackbar("Missing card details")
        }
    }

    private fun showSnackbar(message: String) {
        snackbarController.show(message)
    }

    private fun enableUi(enable: Boolean) {
        viewBinding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        viewBinding.payNow.isEnabled = enable
        viewBinding.cardWidget.isEnabled = enable
        viewBinding.connectAccount.isEnabled = enable
    }
}
