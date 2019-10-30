package com.stripe.android.view

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.STATE_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.PaymentSessionData
import com.stripe.android.R
import com.stripe.android.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.activity_shipping_flow.*

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
class PaymentFlowActivity : StripeActivity() {

    private lateinit var shippingInfoSubmittedBroadcastReceiver: BroadcastReceiver
    private lateinit var paymentFlowPagerAdapter: PaymentFlowPagerAdapter
    private lateinit var paymentSessionData: PaymentSessionData
    private var shippingInformationSubmitted: ShippingInformation? = null
    private val validShippingMethods: MutableList<ShippingMethod> = mutableListOf()
    private var defaultShippingMethod: ShippingMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentFlowActivityStarter.Args.create(intent)

        val customerSession = CustomerSession.getInstance()
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY)
        viewStub.layoutResource = R.layout.activity_shipping_flow
        viewStub.inflate()
        paymentSessionData = requireNotNull(args.paymentSessionData) {
            "PaymentFlowActivity launched without PaymentSessionData"
        }

        paymentFlowPagerAdapter = PaymentFlowPagerAdapter(
            this, args.paymentSessionConfig, customerSession
        )
        shipping_flow_viewpager.adapter = paymentFlowPagerAdapter
        shipping_flow_viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}

            override fun onPageSelected(i: Int) {
                title = paymentFlowPagerAdapter.getPageTitle(i)
                if (paymentFlowPagerAdapter.getPageAt(i) === PaymentFlowPagerEnum.SHIPPING_INFO) {
                    paymentFlowPagerAdapter.hideShippingPage()
                }
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })
        shippingInfoSubmittedBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isShippingInfoValid = intent.getBooleanExtra(
                    PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID,
                    false)
                if (isShippingInfoValid) {
                    onShippingInfoValidated(CustomerSession.getInstance())

                    val intentShippingMethods: List<ShippingMethod>? =
                        intent.getParcelableArrayListExtra(
                            PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS
                        )
                    if (intentShippingMethods != null) {
                        validShippingMethods.clear()
                        validShippingMethods.addAll(intentShippingMethods)
                    }
                    defaultShippingMethod = intent
                        .getParcelableExtra(PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD)
                } else {
                    setCommunicatingProgress(false)
                    val shippingInfoError = intent
                        .getStringExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_ERROR)
                    if (!shippingInfoError.isNullOrEmpty()) {
                        showError(shippingInfoError)
                    } else {
                        showError(getString(R.string.invalid_shipping_information))
                    }
                    shippingInformationSubmitted = null
                }
            }
        }

        title = paymentFlowPagerAdapter.getPageTitle(shipping_flow_viewpager.currentItem)
    }

    public override fun onActionSave() {
        if (PaymentFlowPagerEnum.SHIPPING_INFO ==
            paymentFlowPagerAdapter.getPageAt(shipping_flow_viewpager.currentItem)) {
            onShippingInfoSubmitted()
        } else {
            onShippingMethodSave()
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(shippingInfoSubmittedBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            shippingInfoSubmittedBroadcastReceiver,
            IntentFilter(PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED)
        )
    }

    @JvmSynthetic
    internal fun onShippingInfoSaved(shippingInformation: ShippingInformation?) {
        onShippingMethodsReady(validShippingMethods, defaultShippingMethod)
        paymentSessionData.shippingInformation = shippingInformation
    }

    private fun onShippingInfoValidated(customerSession: CustomerSession) {
        shippingInformationSubmitted?.let {
            customerSession.setCustomerShippingInformation(it,
                CustomerShippingInfoSavedListener(this))
        }
    }

    private fun onShippingMethodsReady(
        validShippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod?
    ) {
        setCommunicatingProgress(false)
        paymentFlowPagerAdapter.setShippingMethods(validShippingMethods, defaultShippingMethod)
        paymentFlowPagerAdapter.setShippingInfoSaved(true)
        if (hasNextPage()) {
            shipping_flow_viewpager.currentItem = shipping_flow_viewpager.currentItem + 1
        } else {
            paymentSessionData.shippingInformation = shippingInformationSubmitted
            setResult(Activity.RESULT_OK,
                Intent().putExtra(STATE_PAYMENT_SESSION_DATA, paymentSessionData))
            finish()
        }
    }

    private fun onShippingInfoSubmitted() {
        hideKeyboard()

        val shippingInfoWidget: ShippingInfoWidget = findViewById(R.id.shipping_info_widget)
        shippingInfoWidget.shippingInformation?.let { shippingInformation ->
            shippingInformationSubmitted = shippingInformation
            setCommunicatingProgress(true)
            broadcastShippingInfoSubmitted(shippingInformation)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputMethodManager.isAcceptingText) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    private fun broadcastShippingInfoSubmitted(shippingInformation: ShippingInformation) {
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(
                Intent(PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED)
                    .putExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA, shippingInformation)
            )
    }

    private fun hasNextPage(): Boolean {
        return shipping_flow_viewpager.currentItem + 1 < paymentFlowPagerAdapter.count
    }

    private fun hasPreviousPage(): Boolean {
        val currentPageIndex = shipping_flow_viewpager.currentItem
        return currentPageIndex != 0
    }

    private fun onShippingMethodSave() {
        val selectShippingMethodWidget: SelectShippingMethodWidget =
            findViewById(R.id.select_shipping_method_widget)
        paymentSessionData.shippingMethod = selectShippingMethodWidget.selectedShippingMethod
        setResult(Activity.RESULT_OK,
            Intent()
                .putExtra(STATE_PAYMENT_SESSION_DATA, paymentSessionData)
        )
        finish()
    }

    override fun onBackPressed() {
        if (hasPreviousPage()) {
            shipping_flow_viewpager.currentItem = shipping_flow_viewpager.currentItem - 1
            return
        }
        super.onBackPressed()
    }

    private class CustomerShippingInfoSavedListener internal constructor(
        activity: PaymentFlowActivity
    ) : CustomerSession.CustomerRetrievalListener {
        private val activityRef: WeakReference<PaymentFlowActivity> = WeakReference(activity)

        override fun onCustomerRetrieved(customer: Customer) {
            activityRef.get()?.onShippingInfoSaved(customer.shippingInformation)
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            activityRef.get()?.showError(errorMessage)
        }
    }

    companion object {
        internal const val TOKEN_PAYMENT_FLOW_ACTIVITY: String = "PaymentFlowActivity"
        internal const val TOKEN_SHIPPING_INFO_SCREEN: String = "ShippingInfoScreen"
        internal const val TOKEN_SHIPPING_METHOD_SCREEN: String = "ShippingMethodScreen"
    }
}
