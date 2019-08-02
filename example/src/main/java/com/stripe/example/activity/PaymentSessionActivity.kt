package com.stripe.example.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.stripe.android.*
import com.stripe.android.PayWithGoogleUtils.getPriceString
import com.stripe.android.model.Address
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED
import com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED
import com.stripe.android.view.PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD
import com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID
import com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA
import com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.service.ExampleEphemeralKeyProvider
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Currency
import java.util.Locale

/**
 * An example activity that handles working with a [PaymentSession], allowing you to collect
 * information needed to request payment for the current customer.
 */
class PaymentSessionActivity : AppCompatActivity() {

    private var mBroadcastReceiver: BroadcastReceiver? = null
    private var mErrorDialogHandler: ErrorDialogHandler? = null
    private var mPaymentSession: PaymentSession? = null
    private var mProgressBar: ProgressBar? = null
    private var mResultTextView: TextView? = null
    private var mResultTitleTextView: TextView? = null
    private var mSelectPaymentButton: Button? = null
    private var mSelectShippingButton: Button? = null
    private var mPaymentSessionData: PaymentSessionData? = null

    private val exampleShippingInfo: ShippingInformation
        get() {
            val address = Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build()
            return ShippingInformation(address, "Fake Name", "(555) 555-5555")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_session)
        mProgressBar = findViewById(R.id.customer_progress_bar)
        mProgressBar!!.visibility = View.VISIBLE
        mSelectPaymentButton = findViewById(R.id.btn_select_payment_method_aps)
        mSelectShippingButton = findViewById(R.id.btn_start_payment_flow)
        mErrorDialogHandler = ErrorDialogHandler(this)
        mResultTitleTextView = findViewById(R.id.tv_payment_session_data_title)
        mResultTextView = findViewById(R.id.tv_payment_session_data)

        // CustomerSession only needs to be initialized once per app.
        val customerSession = setupCustomerSession()
        setupPaymentSession(customerSession)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val shippingInformation = intent
                    .getParcelableExtra<ShippingInformation>(EXTRA_SHIPPING_INFO_DATA)
                val shippingInfoProcessedIntent = Intent(EVENT_SHIPPING_INFO_PROCESSED)
                if (!isValidShippingInfo(shippingInformation)) {
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false)
                } else {
                    val shippingMethods = createSampleShippingMethods()
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true)
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(
                        EXTRA_VALID_SHIPPING_METHODS, shippingMethods)
                    shippingInfoProcessedIntent.putExtra(EXTRA_DEFAULT_SHIPPING_METHOD,
                        shippingMethods[1])
                }
                localBroadcastManager.sendBroadcast(shippingInfoProcessedIntent)
            }

            private fun isValidShippingInfo(shippingInfo: ShippingInformation): Boolean {
                return shippingInfo.address != null && Locale.US.country == shippingInfo.address!!.country
            }
        }
        localBroadcastManager.registerReceiver(mBroadcastReceiver!!,
            IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED))
        mSelectPaymentButton!!.setOnClickListener {
            mPaymentSession!!.presentPaymentMethodSelection(true)
        }
        mSelectShippingButton!!.setOnClickListener {
            mPaymentSession!!.presentShippingFlow()
        }
    }

    private fun setupCustomerSession(): CustomerSession {
        CustomerSession.initCustomerSession(this,
            ExampleEphemeralKeyProvider(ProgressListenerImpl(this)))
        val customerSession = CustomerSession.getInstance()
        customerSession.retrieveCurrentCustomer(
            InitialCustomerRetrievalListener(this))
        return customerSession
    }

    private fun setupPaymentSession(customerSession: CustomerSession) {
        mPaymentSession = PaymentSession(this)
        val paymentSessionInitialized = mPaymentSession!!.init(
            PaymentSessionListenerImpl(this, customerSession),
            PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(exampleShippingInfo)
                .setHiddenShippingInfoFields(
                    ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                    ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                )
                .build())
        if (paymentSessionInitialized) {
            mPaymentSession!!.setCartTotal(2000L)
        }
    }

    private fun formatStringResults(data: PaymentSessionData): String {
        val currency = Currency.getInstance("USD")
        val stringBuilder = StringBuilder()

        if (data.paymentMethod != null) {
            val paymentMethod = data.paymentMethod
            val card = paymentMethod!!.card

            if (card != null) {
                stringBuilder.append("Payment Info:\n").append(card.brand)
                    .append(" ending in ")
                    .append(card.last4)
                    .append(if (data.isPaymentReadyToCharge) " IS " else " IS NOT ")
                    .append("ready to charge.\n\n")
            }
        }
        if (data.shippingInformation != null) {
            stringBuilder.append("Shipping Info: \n")
            stringBuilder.append(data.shippingInformation)
            stringBuilder.append("\n\n")
        }
        if (data.shippingMethod != null) {
            stringBuilder.append("Shipping Method: \n")
            stringBuilder.append(data.shippingMethod).append('\n')
            if (data.shippingTotal > 0) {
                stringBuilder.append("Shipping total: ")
                    .append(getPriceString(data.shippingTotal, currency))
            }
        }

        return stringBuilder.toString()
    }

    private fun createSampleShippingMethods(): ArrayList<ShippingMethod> {
        val shippingMethods = ArrayList<ShippingMethod>()
        shippingMethods.add(ShippingMethod("UPS Ground", "ups-ground",
            "Arrives in 3-5 days", 0, "USD"))
        shippingMethods.add(ShippingMethod("FedEx", "fedex",
            "Arrives tomorrow", 599, "USD"))
        return shippingMethods
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPaymentSession!!.handlePaymentData(requestCode, resultCode, data!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPaymentSession!!.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver!!)
    }

    private fun onPaymentSessionDataChanged(
        customerSession: CustomerSession,
        data: PaymentSessionData
    ) {
        mPaymentSessionData = data
        mProgressBar!!.visibility = View.VISIBLE
        customerSession.retrieveCurrentCustomer(
            PaymentSessionChangeCustomerRetrievalListener(this))
    }

    private class PaymentSessionListenerImpl internal constructor(
        activity: PaymentSessionActivity,
        private val mCustomerSession: CustomerSession
    ) : PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity>(activity) {

        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            val activity = listenerActivity ?: return
            activity.mProgressBar!!.visibility = if (isCommunicating) View.VISIBLE else View.INVISIBLE
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            val activity = listenerActivity ?: return

            activity.mErrorDialogHandler!!.show(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            val activity = listenerActivity ?: return

            activity.onPaymentSessionDataChanged(mCustomerSession, data)
        }
    }

    private class InitialCustomerRetrievalListener internal constructor(
        activity: PaymentSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            val activity = activity ?: return

            activity.mProgressBar!!.visibility = View.INVISIBLE
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            val activity = activity ?: return
            activity.mErrorDialogHandler!!.show(errorMessage)
            activity.mProgressBar!!.visibility = View.INVISIBLE
        }
    }

    private class PaymentSessionChangeCustomerRetrievalListener internal constructor(
        activity: PaymentSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            val activity = activity ?: return

            activity.mProgressBar!!.visibility = View.INVISIBLE
            activity.mSelectPaymentButton!!.isEnabled = true
            activity.mSelectShippingButton!!.isEnabled = true

            if (activity.mPaymentSessionData != null) {
                activity.mResultTitleTextView!!.visibility = View.VISIBLE
                activity.mResultTextView!!.text = activity.formatStringResults(activity.mPaymentSessionData!!)
            }
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            val activity = activity ?: return
            activity.mProgressBar!!.visibility = View.INVISIBLE
        }
    }

    private class ProgressListenerImpl internal constructor(
        activity: PaymentSessionActivity
    ) : ExampleEphemeralKeyProvider.ProgressListener {
        private val mActivityRef: WeakReference<PaymentSessionActivity> = WeakReference(activity)

        override fun onStringResponse(response: String) {
            val activity = mActivityRef.get()
            if (activity != null && response.startsWith("Error: ")) {
                activity.mErrorDialogHandler!!.show(response)
            }
        }
    }
}
