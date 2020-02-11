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
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.example.R
import kotlinx.android.synthetic.main.activity_customer_session.*

/**
 * An example activity that handles working with a [CustomerSession], allowing you to
 * add and select sources for the current customer.
 */
class CustomerSessionActivity : AppCompatActivity() {

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_session)
        setTitle(R.string.customer_payment_data_example)

        progress_bar.visibility = View.VISIBLE
        CustomerSession.getInstance().retrieveCurrentCustomer(
            CustomerRetrievalListenerImpl(this))

        btn_launch_payment_methods.isEnabled = false
        btn_launch_payment_methods.setOnClickListener { launchWithCustomer() }
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
                tv_selected_payment_method.text = buildCardString(card)
            }
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return getString(R.string.ending_in, data.brand, data.last4)
    }

    private fun onCustomerRetrieved() {
        btn_launch_payment_methods.isEnabled = true
        progress_bar.visibility = View.INVISIBLE
    }

    private fun onRetrieveError(errorMessage: String) {
        btn_launch_payment_methods.isEnabled = false
        progress_bar.visibility = View.INVISIBLE
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
