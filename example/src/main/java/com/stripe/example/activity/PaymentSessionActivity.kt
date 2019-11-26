package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.StripeError
import com.stripe.android.model.Address
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.PaymentUtils
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.service.ExampleEphemeralKeyProvider
import kotlinx.android.synthetic.main.activity_payment_session.*
import java.util.Currency
import java.util.Locale

/**
 * An example activity that handles working with a [PaymentSession], allowing you to collect
 * information needed to request payment for the current customer.
 */
class PaymentSessionActivity : AppCompatActivity() {

    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var paymentSession: PaymentSession
    private lateinit var notSelectedText: String

    private var paymentSessionData: PaymentSessionData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_session)

        notSelectedText = getString(R.string.not_selected)

        progress_bar.visibility = View.VISIBLE
        errorDialogHandler = ErrorDialogHandler(this)

        paymentSession = createPaymentSession(savedInstanceState)

        btn_select_payment_method.setOnClickListener {
            paymentSession.presentPaymentMethodSelection(true)
        }
        btn_start_payment_flow.setOnClickListener {
            paymentSession.presentShippingFlow()
        }
    }

    private fun createCustomerSession(): CustomerSession {
        CustomerSession.initCustomerSession(
            this,
            ExampleEphemeralKeyProvider(this),
            false
        )
        return CustomerSession.getInstance()
    }

    private fun createPaymentSession(
        savedInstanceState: Bundle?,
        shouldPrefetchCustomer: Boolean = true
    ): PaymentSession {
        // CustomerSession only needs to be initialized once per app.
        val customerSession = createCustomerSession()

        val paymentSession = PaymentSession(this)
        val paymentSessionInitialized = paymentSession.init(
            listener = PaymentSessionListenerImpl(this, customerSession),
            paymentSessionConfig = PaymentSessionConfig.Builder()
                .setAddPaymentMethodFooter(R.layout.add_payment_method_footer)
                .setPrepopulatedShippingInfo(EXAMPLE_SHIPPING_INFO)
                .setHiddenShippingInfoFields(
                    ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                    ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                )

                // Optionally specify the `PaymentMethod.Type` values to use.
                // Defaults to `PaymentMethod.Type.Card`
                .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card))
                .setAllowedShippingCountryCodes(setOf("US", "CA"))
                .setShippingInformationValidator(ShippingInformationValidator())
                .setShippingMethodsFactory(ShippingMethodsFactory())
                .build(),
            savedInstanceState = savedInstanceState,
            shouldPrefetchCustomer = shouldPrefetchCustomer
        )
        if (paymentSessionInitialized) {
            paymentSession.setCartTotal(2000L)
        }

        return paymentSession
    }

    private fun createPaymentMethodDescription(data: PaymentSessionData): String {
        return data.paymentMethod?.let { paymentMethod ->
            paymentMethod.card?.let { card ->
                "${card.brand} ending in ${card.last4}"
            } ?: paymentMethod.type
        } ?: notSelectedText
    }

    private fun createShippingInfoDescription(shippingInformation: ShippingInformation?): String {
        return if (shippingInformation != null) {
            listOfNotNull(
                shippingInformation.name,
                shippingInformation.address?.line1,
                shippingInformation.address?.line2,
                shippingInformation.address?.city,
                shippingInformation.address?.state,
                shippingInformation.address?.country,
                shippingInformation.address?.postalCode,
                shippingInformation.phone
            ).joinToString("\n")
        } else {
            notSelectedText
        }
    }

    private fun createShippingMethodDescription(shippingMethod: ShippingMethod?): String {
        return if (shippingMethod != null) {
            listOfNotNull(
                shippingMethod.label,
                shippingMethod.detail,
                PaymentUtils.formatPriceStringUsingFree(
                    shippingMethod.amount,
                    Currency.getInstance("USD"),
                    "Free"
                )
            ).joinToString("\n")
        } else {
            notSelectedText
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        paymentSession.handlePaymentData(requestCode, resultCode, data ?: Intent())
    }

    override fun onDestroy() {
        paymentSession.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        paymentSession.savePaymentSessionInstanceState(outState)
    }

    private fun onPaymentSessionDataChanged(
        customerSession: CustomerSession,
        data: PaymentSessionData
    ) {
        paymentSessionData = data
        progress_bar.visibility = View.VISIBLE
        customerSession.retrieveCurrentCustomer(
            PaymentSessionChangeCustomerRetrievalListener(this)
        )
    }

    private fun enableUi() {
        progress_bar.visibility = View.INVISIBLE
        btn_select_payment_method.isEnabled = true
        btn_start_payment_flow.isEnabled = true
    }

    private fun onCustomerRetrieved() {
        enableUi()

        paymentSessionData?.let { paymentSessionData ->
            tv_ready_to_charge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (paymentSessionData.isPaymentReadyToCharge) {
                    ContextCompat.getDrawable(this, R.drawable.ic_check)
                } else {
                    ContextCompat.getDrawable(this, R.drawable.ic_cancel)
                },
                null, null, null
            )

            tv_payment_method.text = createPaymentMethodDescription(paymentSessionData)

            tv_shipping_info.text =
                createShippingInfoDescription(paymentSessionData.shippingInformation)

            tv_shipping_method.text =
                createShippingMethodDescription(paymentSessionData.shippingMethod)
        }
    }

    private class ShippingInformationValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return shippingInformation.address?.country == Locale.US.country
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            return "The country must be US."
        }
    }

    private class ShippingMethodsFactory : PaymentSessionConfig.ShippingMethodsFactory {
        override fun create(shippingInformation: ShippingInformation): List<ShippingMethod> {
            return SHIPPING_METHODS
        }
    }

    private class PaymentSessionListenerImpl internal constructor(
        activity: PaymentSessionActivity,
        private val customerSession: CustomerSession
    ) : PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity>(activity) {

        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            listenerActivity?.progress_bar?.visibility = if (isCommunicating) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            listenerActivity?.errorDialogHandler?.show(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            listenerActivity?.onPaymentSessionDataChanged(customerSession, data)
        }
    }

    private class PaymentSessionChangeCustomerRetrievalListener internal constructor(
        activity: PaymentSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            activity?.onCustomerRetrieved()
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            activity?.progress_bar?.visibility = View.INVISIBLE
        }
    }

    private companion object {
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

        private val SHIPPING_METHODS = arrayListOf(
            ShippingMethod("UPS Ground", "ups-ground",
                0, "USD", "Arrives in 3-5 days"),
            ShippingMethod("FedEx", "fedex",
                599, "USD", "Arrives tomorrow")
        )
    }
}
