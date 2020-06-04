package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
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
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentUtils
import com.stripe.example.R
import com.stripe.example.databinding.PaymentSessionActivityBinding
import java.util.Currency
import java.util.Locale

/**
 * An example activity that handles working with a [PaymentSession], allowing you to collect
 * information needed to request payment for the current customer.
 */
class PaymentSessionActivity : AppCompatActivity() {
    private val viewBinding: PaymentSessionActivityBinding by lazy {
        PaymentSessionActivityBinding.inflate(layoutInflater)
    }

    private lateinit var paymentSession: PaymentSession
    private val notSelectedText: String by lazy {
        getString(R.string.not_selected)
    }
    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    private var paymentSessionData: PaymentSessionData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        paymentSession = createPaymentSession(savedInstanceState == null)

        viewBinding.selectPaymentMethodButton.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }
        viewBinding.startPaymentFlowButton.setOnClickListener {
            paymentSession.presentShippingFlow()
        }
    }

    private fun createPaymentSession(
        shouldPrefetchCustomer: Boolean = false
    ): PaymentSession {
        if (shouldPrefetchCustomer) {
            disableUi()
        } else {
            enableUi()
        }

        // CustomerSession only needs to be initialized once per app.
        val customerSession = CustomerSession.getInstance()

        val paymentSession = PaymentSession(
            activity = this,
            config = PaymentSessionConfig.Builder()
                .setAddPaymentMethodFooter(R.layout.add_payment_method_footer)
                .setPrepopulatedShippingInfo(EXAMPLE_SHIPPING_INFO)
                .setHiddenShippingInfoFields()
                // Optionally specify the `PaymentMethod.Type` values to use.
                // Defaults to `PaymentMethod.Type.Card`
                .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card))
                .setShouldShowGooglePay(true)
                .setAllowedShippingCountryCodes(setOf("US", "CA"))
                .setShippingInformationValidator(ShippingInformationValidator())
                .setShippingMethodsFactory(ShippingMethodsFactory())
                .setWindowFlags(WindowManager.LayoutParams.FLAG_SECURE)
                .setBillingAddressFields(BillingAddressFields.PostalCode)
                .setShouldPrefetchCustomer(shouldPrefetchCustomer)
                .setCanDeletePaymentMethods(true)
                .build()
        )
        paymentSession.init(
            listener = PaymentSessionListenerImpl(this, customerSession)
        )
        paymentSession.setCartTotal(2000L)

        return paymentSession
    }

    private fun createPaymentMethodDescription(data: PaymentSessionData): String {
        val paymentMethod = data.paymentMethod
        return when {
            paymentMethod != null -> {
                paymentMethod.card?.let { card ->
                    "${card.brand} ending in ${card.last4}"
                } ?: paymentMethod.type?.code.orEmpty()
            }
            data.useGooglePay -> {
                "Use Google Pay"
            }
            else -> {
                notSelectedText
            }
        }
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

    private fun onPaymentSessionDataChanged(
        customerSession: CustomerSession,
        data: PaymentSessionData
    ) {
        paymentSessionData = data
        disableUi()
        customerSession.retrieveCurrentCustomer(
            PaymentSessionChangeCustomerRetrievalListener(this)
        )
    }

    private fun enableUi() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        viewBinding.selectPaymentMethodButton.isEnabled = true
        viewBinding.startPaymentFlowButton.isEnabled = true
    }

    private fun disableUi() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.selectPaymentMethodButton.isEnabled = false
        viewBinding.startPaymentFlowButton.isEnabled = false
    }

    private fun onCustomerRetrieved() {
        enableUi()

        paymentSessionData?.let { paymentSessionData ->
            viewBinding.readyToCharge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (paymentSessionData.isPaymentReadyToCharge) {
                    ContextCompat.getDrawable(this, R.drawable.ic_check)
                } else {
                    ContextCompat.getDrawable(this, R.drawable.ic_cancel)
                },
                null, null, null
            )

            viewBinding.paymentMethod.text =
                createPaymentMethodDescription(paymentSessionData)

            viewBinding.shippingInfo.text =
                createShippingInfoDescription(paymentSessionData.shippingInformation)

            viewBinding.shippingMethod.text =
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

    private fun showError(errorMessage: String) {
        snackbarController.show(errorMessage)
    }

    private class PaymentSessionListenerImpl internal constructor(
        activity: PaymentSessionActivity,
        private val customerSession: CustomerSession
    ) : PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity>(activity) {

        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
            if (isCommunicating) {
                BackgroundTaskTracker.onStart()
            }

            if (isCommunicating) {
                listenerActivity?.disableUi()
            } else {
                listenerActivity?.enableUi()
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            BackgroundTaskTracker.onStop()
            listenerActivity?.showError(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            BackgroundTaskTracker.onStop()
            listenerActivity?.onPaymentSessionDataChanged(customerSession, data)
        }
    }

    private fun onError() {
        viewBinding.progressBar.visibility = View.INVISIBLE
    }

    private class PaymentSessionChangeCustomerRetrievalListener internal constructor(
        activity: PaymentSessionActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity>(activity) {

        init {
            BackgroundTaskTracker.onStart()
        }

        override fun onCustomerRetrieved(customer: Customer) {
            BackgroundTaskTracker.onStop()
            activity?.onCustomerRetrieved()
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            BackgroundTaskTracker.onStop()
            activity?.onError()
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
            "Jenny Rosen",
            "(555) 555-5555"
        )

        private val SHIPPING_METHODS = listOf(
            ShippingMethod(
                "UPS Ground", "ups-ground",
                599, "USD", "Arrives in 3-5 days"
            ),
            ShippingMethod(
                "FedEx Overnight", "fedex",
                1499, "USD", "Arrives tomorrow"
            )
        )
    }
}
