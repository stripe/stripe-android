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
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentFlowActivityStarter.Args.create(intent)

        val customerSession = CustomerSession.getInstance()
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY)
        viewStub.layoutResource = R.layout.activity_shipping_flow
        viewStub.inflate()

        paymentSessionData = savedInstanceState?.getParcelable(STATE_PAYMENT_SESSION_DATA)
            ?: requireNotNull(args.paymentSessionData) {
                "PaymentFlowActivity launched without PaymentSessionData"
            }

        val paymentSessionConfig = args.paymentSessionConfig

        val shippingInformation =
            savedInstanceState?.getParcelable(STATE_SHIPPING_INFO)
                ?: paymentSessionConfig.prepopulatedShippingInfo

        paymentFlowPagerAdapter = PaymentFlowPagerAdapter(
            this,
            paymentSessionConfig,
            customerSession,
            shippingInformation,
            savedInstanceState?.getParcelable(STATE_SHIPPING_METHOD),
            paymentSessionConfig.allowedShippingCountryCodes
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
                    val shippingMethods: List<ShippingMethod>? =
                        intent.getParcelableArrayListExtra(
                            PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS
                        )
                    val defaultShippingMethod: ShippingMethod? = intent
                        .getParcelableExtra(PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD)

                    onShippingInfoValidated(
                        CustomerSession.getInstance(),
                        shippingMethods.orEmpty(),
                        defaultShippingMethod
                    )
                } else {
                    setCommunicatingProgress(false)
                    val shippingInfoError = intent
                        .getStringExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_ERROR)
                    if (!shippingInfoError.isNullOrEmpty()) {
                        showError(shippingInfoError)
                    } else {
                        showError(getString(R.string.invalid_shipping_information))
                    }
                    paymentSessionData = paymentSessionData.copy(shippingInformation = null)
                }
            }
        }

        title = paymentFlowPagerAdapter.getPageTitle(shipping_flow_viewpager.currentItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        shipping_flow_viewpager.currentItem = savedInstanceState.getInt(STATE_CURRENT_ITEM, 0)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_PAYMENT_SESSION_DATA, paymentSessionData)
        outState.putParcelable(STATE_SHIPPING_INFO, shippingInfo)
        outState.putParcelable(STATE_SHIPPING_METHOD, selectedShippingMethod)
        outState.putInt(STATE_CURRENT_ITEM, shipping_flow_viewpager.currentItem)
    }

    @JvmSynthetic
    internal fun onShippingInfoSaved(
        shippingInformation: ShippingInformation?,
        shippingMethods: List<ShippingMethod> = emptyList(),
        defaultShippingMethod: ShippingMethod? = null
    ) {
        onShippingMethodsReady(shippingMethods, defaultShippingMethod)
        paymentSessionData = paymentSessionData.copy(
            shippingInformation = shippingInformation
        )
    }

    private fun onShippingInfoValidated(
        customerSession: CustomerSession,
        shippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod?
    ) {
        paymentSessionData.shippingInformation?.let {
            customerSession.setCustomerShippingInformation(it,
                CustomerShippingInfoSavedListener(
                    this, shippingMethods, defaultShippingMethod
                )
            )
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
            finishWithData(paymentSessionData)
        }
    }

    private fun onShippingInfoSubmitted() {
        hideKeyboard()

        shippingInfo?.let { shippingInfo ->
            paymentSessionData = paymentSessionData.copy(shippingInformation = shippingInfo)
            setCommunicatingProgress(true)
            broadcastShippingInfoSubmitted(shippingInfo)
        }
    }

    private val shippingInfo: ShippingInformation?
        get() {
            val shippingInfoWidget: ShippingInfoWidget = findViewById(R.id.shipping_info_widget)
            return shippingInfoWidget.rawShippingInformation
        }

    private val selectedShippingMethod: ShippingMethod?
        get() {
            return if (PaymentFlowPagerEnum.SHIPPING_METHOD ==
                paymentFlowPagerAdapter.getPageAt(shipping_flow_viewpager.currentItem)) {
                val selectShippingMethodWidget: SelectShippingMethodWidget =
                    findViewById(R.id.select_shipping_method_widget)
                selectShippingMethodWidget.selectedShippingMethod
            } else {
                null
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
        finishWithData(paymentSessionData.copy(
            shippingMethod = selectShippingMethodWidget.selectedShippingMethod
        ))
        finish()
    }

    private fun finishWithData(paymentSessionData: PaymentSessionData) {
        setResult(Activity.RESULT_OK,
            Intent().putExtra(EXTRA_PAYMENT_SESSION_DATA, paymentSessionData)
        )
        finish()
    }

    override fun onBackPressed() {
        if (hasPreviousPage()) {
            shipping_flow_viewpager.currentItem = shipping_flow_viewpager.currentItem - 1
        } else {
            super.onBackPressed()
        }
    }

    private class CustomerShippingInfoSavedListener internal constructor(
        activity: PaymentFlowActivity,
        private val shippingMethods: List<ShippingMethod>,
        private val defaultShippingMethod: ShippingMethod?
    ) : CustomerSession.CustomerRetrievalListener {
        private val activityRef: WeakReference<PaymentFlowActivity> = WeakReference(activity)

        override fun onCustomerRetrieved(customer: Customer) {
            activityRef.get()?.onShippingInfoSaved(
                customer.shippingInformation, shippingMethods, defaultShippingMethod
            )
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            activityRef.get()?.showError(errorMessage)
        }
    }

    internal companion object {
        internal const val TOKEN_PAYMENT_FLOW_ACTIVITY: String = "PaymentFlowActivity"
        internal const val TOKEN_SHIPPING_INFO_SCREEN: String = "ShippingInfoScreen"
        internal const val TOKEN_SHIPPING_METHOD_SCREEN: String = "ShippingMethodScreen"

        private const val STATE_PAYMENT_SESSION_DATA = "state_payment_session_data"
        private const val STATE_SHIPPING_INFO: String = "state_shipping_info"
        private const val STATE_SHIPPING_METHOD: String = "state_shipping_method"
        private const val STATE_CURRENT_ITEM: String = "state_current_item"
    }
}
