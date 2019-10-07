package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.service.ExampleEphemeralKeyProvider

/**
 * An example activity that handles working with a [CustomerSession], allowing you to
 * add and select sources for the current customer.
 */
class CustomerSessionActivity : AppCompatActivity() {

    private lateinit var selectSourceButton: Button
    private lateinit var selectedSourceTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorDialogHandler: ErrorDialogHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_session)
        setTitle(R.string.customer_payment_data_example)
        progressBar = findViewById(R.id.customer_progress_bar)
        selectedSourceTextView = findViewById(R.id.tv_customer_default_source_acs)
        selectSourceButton = findViewById(R.id.btn_launch_payment_methods_acs)
        selectSourceButton.isEnabled = false
        errorDialogHandler = ErrorDialogHandler(this)
        CustomerSession.initCustomerSession(
            this,
            ExampleEphemeralKeyProvider(),
            false
        )

        progressBar.visibility = View.VISIBLE
        CustomerSession.getInstance().retrieveCurrentCustomer(
            CustomerRetrievalListenerImpl(this))

        selectSourceButton.setOnClickListener { launchWithCustomer() }
    }

    private fun launchWithCustomer() {
        PaymentMethodsActivityStarter(this).startForResult()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null) {
            val paymentMethod =
                PaymentMethodsActivityStarter.Result.fromIntent(data)?.paymentMethod
            paymentMethod?.card?.let { card ->
                selectedSourceTextView.text = buildCardString(card)
            }
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return getString(R.string.ending_in, data.brand, data.last4)
    }

    private fun onCustomerRetrieved() {
        selectSourceButton.isEnabled = true
        progressBar.visibility = View.INVISIBLE
    }

    private fun onRetrieveError(errorMessage: String) {
        selectSourceButton.isEnabled = false
        errorDialogHandler.show(errorMessage)
        progressBar.visibility = View.INVISIBLE
    }

    private class CustomerRetrievalListenerImpl constructor(
        activity: CustomerSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<CustomerSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            activity?.onCustomerRetrieved()
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            activity?.onRetrieveError(errorMessage)
        }
    }
}
