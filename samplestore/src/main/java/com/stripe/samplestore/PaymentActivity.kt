package com.stripe.samplestore

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.annotation.Size
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PayWithGoogleUtils
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.Address
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED
import com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED
import com.stripe.android.view.PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD
import com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID
import com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA
import com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS
import com.stripe.samplestore.service.BackendApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var cartItemLayout: LinearLayout
    private lateinit var enterShippingInfo: TextView
    private lateinit var enterPaymentInfo: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var confirmPaymentButton: Button
    private lateinit var setupPaymentCredentialsButton: Button

    private lateinit var stripe: Stripe
    private lateinit var paymentSession: PaymentSession
    private lateinit var service: BackendApi

    private lateinit var storeCart: StoreCart
    private var shippingCosts = 0L

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
        setContentView(R.layout.activity_payment)

        val selectCustomization = PaymentAuthConfig.Stripe3ds2ButtonCustomization.Builder()
            .setBackgroundColor("#EC4847")
            .setTextColor("#000000")
            .build()
        val uiCustomization =
            PaymentAuthConfig.Stripe3ds2UiCustomization.Builder.createWithAppTheme(this)
                .setButtonCustomization(selectCustomization,
                    PaymentAuthConfig.Stripe3ds2UiCustomization.ButtonType.SELECT)
                .build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setUiCustomization(uiCustomization)
                .build())
            .build())

        val publishableKey = PaymentConfiguration.getInstance(this).publishableKey
        stripe = if (Settings.STRIPE_ACCOUNT_ID != null) {
            Stripe(this, publishableKey, Settings.STRIPE_ACCOUNT_ID)
        } else {
            Stripe(this, publishableKey)
        }

        service = RetrofitFactory.instance.create(BackendApi::class.java)

        val extras = intent.extras
        storeCart = extras?.getParcelable(EXTRA_CART)!!

        progressBar = findViewById(R.id.progress_bar)
        cartItemLayout = findViewById(R.id.cart_list_items)
        confirmPaymentButton = findViewById(R.id.btn_confirm_payment)
        setupPaymentCredentialsButton = findViewById(R.id.btn_setup_intent)

        paymentSession = createPaymentSession()

        addCartItems()

        updateConfirmPaymentButton()
        enterShippingInfo = findViewById(R.id.shipping_info)
        enterPaymentInfo = findViewById(R.id.payment_source)
        compositeDisposable.add(RxView.clicks(enterShippingInfo)
            .subscribe { paymentSession.presentShippingFlow() })
        compositeDisposable.add(RxView.clicks(enterPaymentInfo)
            .subscribe { paymentSession.presentPaymentMethodSelection(true) })

        val customerSession = CustomerSession.getInstance()
        compositeDisposable.add(RxView.clicks(confirmPaymentButton)
            .subscribe {
                customerSession.retrieveCurrentCustomer(
                    PaymentIntentCustomerRetrievalListener(this@PaymentActivity))
            })
        compositeDisposable.addAll(RxView.clicks(setupPaymentCredentialsButton)
            .subscribe {
                customerSession.retrieveCurrentCustomer(
                    SetupIntentCustomerRetrievalListener(this@PaymentActivity))
            })
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val shippingInformation = intent
                    .getParcelableExtra<ShippingInformation>(EXTRA_SHIPPING_INFO_DATA)
                val shippingInfoProcessedIntent = Intent(EVENT_SHIPPING_INFO_PROCESSED)
                if (!isShippingInfoValid(shippingInformation)) {
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false)
                } else {
                    val shippingMethods =
                        getValidShippingMethods(shippingInformation)
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true)
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(
                        EXTRA_VALID_SHIPPING_METHODS, ArrayList(shippingMethods))
                    shippingInfoProcessedIntent
                        .putExtra(EXTRA_DEFAULT_SHIPPING_METHOD, shippingMethods[0])
                }
                localBroadcastManager.sendBroadcast(shippingInfoProcessedIntent)
            }
        }
        localBroadcastManager.registerReceiver(broadcastReceiver,
            IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED))
    }

    private fun isShippingInfoValid(shippingInfo: ShippingInformation): Boolean {
        return shippingInfo.address != null && Locale.US.country == shippingInfo.address!!.country
    }

    /*
     * Cleaning up all Rx subscriptions in onDestroy.
     */
    override fun onDestroy() {
        compositeDisposable.dispose()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        paymentSession.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val isPaymentIntentResult = stripe.onPaymentResult(
            requestCode, data,
            object : ApiResultCallback<PaymentIntentResult> {
                override fun onSuccess(result: PaymentIntentResult) {
                    stopLoading()
                    processStripeIntent(result.intent)
                }

                override fun onError(e: Exception) {
                    stopLoading()
                    displayError(e.message)
                }
            })

        if (isPaymentIntentResult) {
            startLoading()
        } else {
            val isSetupIntentResult = stripe.onSetupResult(requestCode, data,
                object : ApiResultCallback<SetupIntentResult> {
                    override fun onSuccess(result: SetupIntentResult) {
                        stopLoading()
                        processStripeIntent(result.intent)
                    }

                    override fun onError(e: Exception) {
                        stopLoading()
                        displayError(e.message)
                    }
                })
            if (!isSetupIntentResult) {
                paymentSession.handlePaymentData(requestCode, resultCode, data!!)
            }
        }
    }

    private fun updateConfirmPaymentButton() {
        val price = paymentSession.paymentSessionData.cartTotal

        confirmPaymentButton.text = getString(R.string.pay_label, StoreUtils.getPriceString(price, null))
    }

    private fun addCartItems() {
        cartItemLayout.removeAllViewsInLayout()
        val currencySymbol = storeCart.currency.getSymbol(Locale.US)

        addLineItems(currencySymbol, storeCart.lineItems)

        addLineItems(currencySymbol,
            listOf(StoreLineItem(getString(R.string.checkout_shipping_cost_label), 1, shippingCosts)))

        val totalView = layoutInflater
            .inflate(R.layout.cart_item, cartItemLayout, false)
        setupTotalPriceView(totalView, currencySymbol)
        cartItemLayout.addView(totalView)
    }

    private fun addLineItems(currencySymbol: String, items: List<StoreLineItem>) {
        for (item in items) {
            val view = layoutInflater.inflate(
                R.layout.cart_item, cartItemLayout, false)
            fillOutCartItemView(item, view, currencySymbol)
            cartItemLayout.addView(view)
        }
    }

    private fun setupTotalPriceView(view: View, currencySymbol: String) {
        val itemViews = getItemViews(view)
        val totalPrice = paymentSession.paymentSessionData.cartTotal
        itemViews[0].text = getString(R.string.checkout_total_cost_label)
        val price = PayWithGoogleUtils.getPriceString(totalPrice,
            storeCart.currency)
        val displayPrice = currencySymbol + price
        itemViews[3].text = displayPrice
    }

    private fun fillOutCartItemView(item: StoreLineItem, view: View, currencySymbol: String) {
        val itemViews = getItemViews(view)

        itemViews[0].text = item.description

        if (getString(R.string.checkout_shipping_cost_label) != item.description) {
            val quantityPriceString = "X " + item.quantity + " @"
            itemViews[1].text = quantityPriceString

            val unitPriceString = currencySymbol + PayWithGoogleUtils.getPriceString(item.unitPrice,
                storeCart.currency)
            itemViews[2].text = unitPriceString
        }

        val totalPriceString = currencySymbol +
            PayWithGoogleUtils.getPriceString(item.totalPrice, storeCart.currency)
        itemViews[3].text = totalPriceString
    }

    @Size(value = 4)
    private fun getItemViews(view: View): Array<TextView> {
        val labelView = view.findViewById<TextView>(R.id.tv_cart_emoji)
        val quantityView = view.findViewById<TextView>(R.id.tv_cart_quantity)
        val unitPriceView = view.findViewById<TextView>(R.id.tv_cart_unit_price)
        val totalPriceView = view.findViewById<TextView>(R.id.tv_cart_total_price)
        return arrayOf(labelView, quantityView, unitPriceView, totalPriceView)
    }

    private fun createCapturePaymentParams(
        data: PaymentSessionData,
        customerId: String,
        stripeAccountId: String?
    ): HashMap<String, Any> {
        val params = HashMap<String, Any>()
        params["amount"] = data.cartTotal.toString()
        params["payment_method"] = data.paymentMethod!!.id!!
        params["payment_method_types"] = Settings.ALLOWED_PAYMENT_METHOD_TYPES.map { it.code }
        params["currency"] = Settings.CURRENCY
        params["customer_id"] = customerId
        if (data.shippingInformation != null) {
            params["shipping"] = data.shippingInformation!!.toParamMap()
        }
        params["return_url"] = "stripe://payment-auth-return"
        if (stripeAccountId != null) {
            params["stripe_account"] = stripeAccountId
        }
        return params
    }

    private fun createSetupIntentParams(
        data: PaymentSessionData,
        customerId: String,
        stripeAccountId: String?
    ): HashMap<String, Any> {
        val params = HashMap<String, Any>()
        params["payment_method"] = data.paymentMethod!!.id!!
        params["payment_method_types"] = Settings.ALLOWED_PAYMENT_METHOD_TYPES.map { it.code }
        params["customer_id"] = customerId
        params["return_url"] = "stripe://payment-auth-return"
        params["currency"] = Settings.CURRENCY
        if (stripeAccountId != null) {
            params["stripe_account"] = stripeAccountId
        }
        return params
    }

    private fun capturePayment(customerId: String) {
        if (paymentSession.paymentSessionData.paymentMethod == null) {
            displayError("No payment method selected")
            return
        }

        val stripeResponse = service.capturePayment(
            createCapturePaymentParams(paymentSession.paymentSessionData, customerId,
                Settings.STRIPE_ACCOUNT_ID))
        compositeDisposable.add(stripeResponse
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { startLoading() }
            .doFinally { stopLoading() }
            .subscribe(
                { onStripeIntentClientSecretResponse(it) },
                { throwable -> displayError(throwable.localizedMessage) }
            ))
    }

    private fun createSetupIntent(customerId: String) {
        if (paymentSession.paymentSessionData.paymentMethod == null) {
            displayError("No payment method selected")
            return
        }

        val stripeResponse = service.createSetupIntent(
            createSetupIntentParams(paymentSession.paymentSessionData, customerId,
                Settings.STRIPE_ACCOUNT_ID))
        compositeDisposable.add(stripeResponse
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { startLoading() }
            .doFinally { stopLoading() }
            .subscribe(
                { onStripeIntentClientSecretResponse(it) },
                { throwable -> displayError(throwable.localizedMessage) }
            ))
    }

    private fun displayError(errorMessage: String?) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Error")
        alertDialog.setMessage(errorMessage)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") {
            dialog, _ -> dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun processStripeIntent(stripeIntent: StripeIntent) {
        if (stripeIntent.requiresAction()) {
            stripe.authenticatePayment(this, stripeIntent.clientSecret!!)
        } else if (stripeIntent.requiresConfirmation()) {
            confirmStripeIntent(stripeIntent.id!!, Settings.STRIPE_ACCOUNT_ID)
        } else if (stripeIntent.status == StripeIntent.Status.Succeeded) {
            if (stripeIntent is PaymentIntent) {
                finishPayment()
            } else if (stripeIntent is SetupIntent) {
                finishSetup()
            }
        } else if (stripeIntent.status == StripeIntent.Status.RequiresPaymentMethod) {
            // reset payment method and shipping if authentication fails
            paymentSession = createPaymentSession()
            enterPaymentInfo.text = getString(R.string.add_payment_method)
            enterShippingInfo.text = getString(R.string.add_shipping_details)
        } else {
            displayError(
                "Unhandled Payment Intent Status: " + stripeIntent.status.toString())
        }
    }

    private fun confirmStripeIntent(stripeIntentId: String, stripeAccountId: String?) {
        val params = HashMap<String, Any>()
        params["payment_intent_id"] = stripeIntentId
        if (stripeAccountId != null) {
            params["stripe_account"] = stripeAccountId
        }

        compositeDisposable.add(service.confirmPayment(params)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { startLoading() }
            .doFinally { stopLoading() }
            .subscribe(
                { onStripeIntentClientSecretResponse(it) },
                { throwable -> displayError(throwable.localizedMessage) }
            ))
    }

    @Throws(IOException::class, JSONException::class)
    private fun onStripeIntentClientSecretResponse(responseBody: ResponseBody) {
        val clientSecret = JSONObject(responseBody.string()).getString("secret")
        compositeDisposable.add(
            Observable
                .fromCallable { retrieveStripeIntent(clientSecret) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { startLoading() }
                .doFinally { stopLoading() }
                .subscribe { processStripeIntent(it) }
        )
    }

    private fun retrieveStripeIntent(clientSecret: String): StripeIntent {
        return when {
            clientSecret.startsWith("pi_") ->
                stripe.retrievePaymentIntentSynchronous(clientSecret)!!
            clientSecret.startsWith("seti_") ->
                stripe.retrieveSetupIntentSynchronous(clientSecret)!!
            else -> throw IllegalArgumentException("Invalid client_secret: $clientSecret")
        }
    }

    private fun finishPayment() {
        paymentSession.onCompleted()
        val data = StoreActivity.createPurchaseCompleteIntent(
            storeCart.totalPrice + shippingCosts)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun finishSetup() {
        paymentSession.onCompleted()
        setResult(Activity.RESULT_OK, Intent().putExtras(Bundle()))
        finish()
    }

    private fun createPaymentSession(): PaymentSession {
        val paymentSession = PaymentSession(this)
        paymentSession.init(PaymentSessionListenerImpl(this),
            PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(exampleShippingInfo).build())
        paymentSession.setCartTotal(storeCart.totalPrice)

        val isPaymentReadyToCharge = paymentSession.paymentSessionData.isPaymentReadyToCharge
        confirmPaymentButton.isEnabled = isPaymentReadyToCharge
        setupPaymentCredentialsButton.isEnabled = isPaymentReadyToCharge

        return paymentSession
    }

    private fun startLoading() {
        progressBar.visibility = View.VISIBLE
        enterPaymentInfo.isEnabled = false
        enterShippingInfo.isEnabled = false

        confirmPaymentButton.tag = confirmPaymentButton.isEnabled
        confirmPaymentButton.isEnabled = false

        setupPaymentCredentialsButton.tag = setupPaymentCredentialsButton.isEnabled
        setupPaymentCredentialsButton.isEnabled = false
    }

    private fun stopLoading() {
        progressBar.visibility = View.INVISIBLE
        enterPaymentInfo.isEnabled = true
        enterShippingInfo.isEnabled = true

        confirmPaymentButton.isEnabled = java.lang.Boolean.TRUE == confirmPaymentButton.tag
        setupPaymentCredentialsButton.isEnabled = java.lang.Boolean.TRUE == setupPaymentCredentialsButton.tag
    }

    private fun getPaymentMethodDescription(paymentMethod: PaymentMethod): String {
        return when (paymentMethod.type) {
            PaymentMethod.Type.Card.code -> {
                paymentMethod.card?.let {
                    "${getDisplayName(it.brand)}-${it.last4}"
                } ?: ""
            }
            PaymentMethod.Type.Fpx.code -> {
                paymentMethod.fpx?.let {
                    "${getDisplayName(it.bank)} (FPX)"
                } ?: ""
            }
            else -> ""
        }
    }

    private fun getDisplayName(name: String?): String {
        return (name ?: "")
            .split("_")
            .joinToString(separator = " ") { it.capitalize() }
    }

    private fun getValidShippingMethods(
        shippingInformation: ShippingInformation
    ): List<ShippingMethod> {
        val isCourierSupported = shippingInformation.address != null &&
            "94110" == shippingInformation.address!!.postalCode
        val courierMethod = if (isCourierSupported) {
            ShippingMethod("1 Hour Courier", "courier",
                "Arrives in the next hour", 1099, Settings.CURRENCY)
        } else {
            null
        }
        return listOfNotNull(
            ShippingMethod("UPS Ground", "ups-ground",
                "Arrives in 3-5 days", 0, Settings.CURRENCY),
            ShippingMethod("FedEx", "fedex",
                "Arrives tomorrow", 599, Settings.CURRENCY),
            courierMethod
        )
    }

    private fun onPaymentSessionDataChanged(data: PaymentSessionData) {
        if (data.shippingMethod != null) {
            enterShippingInfo.text = data.shippingMethod!!.label
            shippingCosts = data.shippingMethod!!.amount
            paymentSession.setCartTotal(storeCart.totalPrice + shippingCosts)
            addCartItems()
            updateConfirmPaymentButton()
        }

        if (data.paymentMethod != null) {
            enterPaymentInfo.text = getPaymentMethodDescription(data.paymentMethod!!)
        }

        if (data.isPaymentReadyToCharge) {
            confirmPaymentButton.isEnabled = true
            setupPaymentCredentialsButton.isEnabled = true
        }
    }

    private class PaymentSessionListenerImpl constructor(
        activity: PaymentActivity
    ) : PaymentSession.ActivityPaymentSessionListener<PaymentActivity>(activity) {

        override fun onCommunicatingStateChanged(isCommunicating: Boolean) {}

        override fun onError(errorCode: Int, errorMessage: String) {
            val activity = listenerActivity ?: return

            activity.displayError(errorMessage)
        }

        override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
            val activity = listenerActivity ?: return
            activity.onPaymentSessionDataChanged(data)
        }
    }

    private class PaymentIntentCustomerRetrievalListener constructor(
        activity: PaymentActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            val activity = activity ?: return

            activity.capturePayment(customer.id)
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            val activity = activity ?: return

            activity.displayError("Error getting payment method:. $errorMessage")
        }
    }

    private class SetupIntentCustomerRetrievalListener constructor(
        activity: PaymentActivity
    ) : CustomerSession.ActivityCustomerRetrievalListener<PaymentActivity>(activity) {

        override fun onCustomerRetrieved(customer: Customer) {
            val activity = activity ?: return
            activity.createSetupIntent(customer.id)
        }

        override fun onError(httpCode: Int, errorMessage: String, stripeError: StripeError?) {
            val activity = activity ?: return
            activity.displayError("Error getting payment method:. $errorMessage")
        }
    }

    companion object {
        private const val EXTRA_CART = "extra_cart"

        fun createIntent(activity: Activity, cart: StoreCart): Intent {
            return Intent(activity, PaymentActivity::class.java)
                .putExtra(EXTRA_CART, cart)
        }
    }
}
