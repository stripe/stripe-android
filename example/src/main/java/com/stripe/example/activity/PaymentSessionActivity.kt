package com.stripe.example.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.CustomerSession
import com.stripe.android.PayWithGoogleUtils.getPriceString
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.StripeError
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
import java.util.ArrayList
import java.util.Currency
import java.util.Locale

/**
 * An example activity that handles working with a [PaymentSession], allowing you to collect
 * information needed to request payment for the current customer.
 */
class PaymentSessionActivity : AppCompatActivity() {

    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var paymentSession: PaymentSession
    private lateinit var progressBar: ProgressBar
    private lateinit var resultTextView: TextView
    private lateinit var resultTitleTextView: TextView
    private lateinit var selectPaymentButton: Button
    private lateinit var selectShippingButton: Button

    private var paymentSessionData: PaymentSessionData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_session)
        progressBar = findViewById(R.id.customer_progress_bar)
        progressBar.visibility = View.VISIBLE
        selectPaymentButton = findViewById(R.id.btn_select_payment_method_aps)
        selectShippingButton = findViewById(R.id.btn_start_payment_flow)
        errorDialogHandler = ErrorDialogHandler(this)
        resultTitleTextView = findViewById(R.id.tv_payment_session_data_title)
        resultTextView = findViewById(R.id.tv_payment_session_data)

        // CustomerSession only needs to be initialized once per app.
        val customerSession = createCustomerSession()
        paymentSession = createPaymentSession(customerSession)

        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        broadcastReceiver = object : BroadcastReceiver() {
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

            private fun isValidShippingInfo(shippingInfo: ShippingInformation?): Boolean {
                return shippingInfo?.address?.country == Locale.US.country
            }
        }
        localBroadcastManager.registerReceiver(broadcastReceiver,
            IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED))
        selectPaymentButton.setOnClickListener {
            paymentSession.presentPaymentMethodSelection(true)
        }
        selectShippingButton.setOnClickListener {
            paymentSession.presentShippingFlow()
        }
    }

    private fun createCustomerSession(): CustomerSession {
        CustomerSession.initCustomerSession(
            this,
            ExampleEphemeralKeyProvider(),
            false
        )
        return CustomerSession.getInstance()
    }

    private fun createPaymentSession(customerSession: CustomerSession): PaymentSession {
        val paymentSession = PaymentSession(this)
        val paymentSessionInitialized = paymentSession.init(
            PaymentSessionListenerImpl(this, customerSession),
            PaymentSessionConfig.Builder()
                .setAddPaymentMethodFooter(R.layout.add_payment_method_footer)
                .setPrepopulatedShippingInfo(EXAMPLE_SHIPPING_INFO)
                .setHiddenShippingInfoFields(
                    ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                    ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                )
                .build())
        if (paymentSessionInitialized) {
            paymentSession.setCartTotal(2000L)
        }

        return paymentSession
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
        paymentSession.handlePaymentData(requestCode, resultCode, data ?: Intent())
    }

    override fun onDestroy() {
        super.onDestroy()
        paymentSession.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun onPaymentSessionDataChanged(
        customerSession: CustomerSession,
        data: PaymentSessionData
    ) {
        paymentSessionData = data
        progressBar.visibility = View.VISIBLE
        customerSession.retrieveCurrentCustomer(
            PaymentSessionChangeCustomerRetrievalListener(this))
    }

    private class PaymentSessionListenerImpl internal constructor(
        activity: PaymentSessionActivity,
        private val customerSession: CustomerSession
    ) : PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity>(activity) {

        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            val activity = listenerActivity ?: return
            activity.progressBar.visibility = if (isCommunicating) View.VISIBLE else View.INVISIBLE
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            val activity = listenerActivity ?: return
            activity.errorDialogHandler.show(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            val activity = listenerActivity ?: return

            activity.onPaymentSessionDataChanged(customerSession, data)
        }
    }

    private class PaymentSessionChangeCustomerRetrievalListener internal constructor(
        activity: PaymentSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            val activity = activity ?: return

            activity.progressBar.visibility = View.INVISIBLE
            activity.selectPaymentButton.isEnabled = true
            activity.selectShippingButton.isEnabled = true

            activity.paymentSessionData?.let { paymentSessionData ->
                activity.resultTitleTextView.visibility = View.VISIBLE
                activity.resultTextView.text = activity.formatStringResults(paymentSessionData)
            }
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            val activity = activity ?: return
            activity.progressBar.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val EXAMPLE_SHIPPING_INFO = ShippingInformation(
            Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build(),
            "Fake Name",
            "(555) 555-5555"
        )
    }
}
