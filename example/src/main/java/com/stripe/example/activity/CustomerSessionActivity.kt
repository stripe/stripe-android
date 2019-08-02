package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

import com.stripe.android.CustomerSession
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.service.ExampleEphemeralKeyProvider

import java.lang.ref.WeakReference

/**
 * An example activity that handles working with a [CustomerSession], allowing you to
 * add and select sources for the current customer.
 */
class CustomerSessionActivity : AppCompatActivity() {

    private var mSelectSourceButton: Button? = null
    private var mSelectedSourceTextView: TextView? = null
    private var mProgressBar: ProgressBar? = null
    private var mErrorDialogHandler: ErrorDialogHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_session)
        setTitle(R.string.customer_payment_data_example)
        mProgressBar = findViewById(R.id.customer_progress_bar)
        mSelectedSourceTextView = findViewById(R.id.tv_customer_default_source_acs)
        mSelectSourceButton = findViewById(R.id.btn_launch_payment_methods_acs)
        mSelectSourceButton!!.isEnabled = false
        mErrorDialogHandler = ErrorDialogHandler(this)
        CustomerSession.initCustomerSession(this,
            ExampleEphemeralKeyProvider(ProgressListenerImpl(this)))

        mProgressBar!!.visibility = View.VISIBLE
        CustomerSession.getInstance().retrieveCurrentCustomer(
            CustomerRetrievalListenerImpl(this))

        mSelectSourceButton!!.setOnClickListener { launchWithCustomer() }
    }

    private fun launchWithCustomer() {
        PaymentMethodsActivityStarter(this).startForResult(REQUEST_CODE_SELECT_SOURCE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == Activity.RESULT_OK) {
            val paymentMethod = data!!.getParcelableExtra<PaymentMethod>(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT)

            if (paymentMethod?.card != null) {
                mSelectedSourceTextView!!.text = buildCardString(paymentMethod.card!!)
            }
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return data.brand + getString(R.string.ending_in) + data.last4
    }

    private fun onCustomerRetrieved() {
        mSelectSourceButton!!.isEnabled = true
        mProgressBar!!.visibility = View.INVISIBLE
    }

    private fun onRetrieveError(errorMessage: String) {
        mSelectSourceButton!!.isEnabled = false
        mErrorDialogHandler!!.show(errorMessage)
        mProgressBar!!.visibility = View.INVISIBLE
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

    private class ProgressListenerImpl constructor(
        activity: CustomerSessionActivity
    ) : ExampleEphemeralKeyProvider.ProgressListener {

        private val activityRef: WeakReference<CustomerSessionActivity> = WeakReference(activity)

        override fun onStringResponse(response: String) {
            activityRef.get()?.let {
                if (response.startsWith("Error: ")) {
                    it.mErrorDialogHandler!!.show(response)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_SOURCE = 55
    }
}
