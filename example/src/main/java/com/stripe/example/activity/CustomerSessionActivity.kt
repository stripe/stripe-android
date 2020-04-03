package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.example.R
import com.stripe.example.databinding.CustomerSessionActivityBinding

/**
 * An example activity that handles working with a [CustomerSession], allowing you to
 * add and select sources for the current customer.
 */
class CustomerSessionActivity : AppCompatActivity() {
    private val viewBinding: CustomerSessionActivityBinding by lazy {
        CustomerSessionActivityBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    private var selectedPaymentMethodID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setTitle(R.string.customer_payment_data_example)

        viewBinding.progressBar.visibility = View.VISIBLE
        CustomerSession.getInstance().retrieveCurrentCustomer(
            CustomerRetrievalListenerImpl(this))

        viewBinding.selectPaymentMethodButton.isEnabled = false
        viewBinding.selectPaymentMethodButton.setOnClickListener { launchWithCustomer() }
    }

    private fun launchWithCustomer() {
        val args = PaymentMethodsActivityStarter.Args.Builder().setInitialPaymentMethodId(selectedPaymentMethodID).build()
        PaymentMethodsActivityStarter(this, args).startForResult()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null) {
            val paymentMethod =
                PaymentMethodsActivityStarter.Result.fromIntent(data)?.paymentMethod
            paymentMethod?.card?.let { card ->
                viewBinding.paymentMethod.text = buildCardString(card)
                selectedPaymentMethodID = paymentMethod?.id
            }
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return getString(R.string.ending_in, data.brand, data.last4)
    }

    private fun onCustomerRetrieved() {
        viewBinding.selectPaymentMethodButton.isEnabled = true
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private fun onRetrieveError(errorMessage: String) {
        viewBinding.selectPaymentMethodButton.isEnabled = false
        viewBinding.progressBar.visibility = View.INVISIBLE
        snackbarController.show(errorMessage)
    }

    private class CustomerRetrievalListenerImpl constructor(
        activity: CustomerSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<CustomerSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            activity?.onCustomerRetrieved()
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            activity?.onRetrieveError(errorMessage)
        }
    }
}
