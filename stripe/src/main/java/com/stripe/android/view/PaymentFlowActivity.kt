package com.stripe.android.view

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.R
import com.stripe.android.model.Customer
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.android.synthetic.main.activity_enter_shipping_info.*
import kotlinx.android.synthetic.main.activity_shipping_flow.*

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
class PaymentFlowActivity : StripeActivity() {

    private lateinit var paymentFlowPagerAdapter: PaymentFlowPagerAdapter
    private lateinit var paymentSessionConfig: PaymentSessionConfig
    private lateinit var customerSession: CustomerSession
    private lateinit var viewModel: PaymentFlowViewModel

    private var shippingInfoSubmittedBroadcastReceiver: BroadcastReceiver? = null

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentFlowActivityStarter.Args.create(intent)

        customerSession = CustomerSession.getInstance()
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY)
        viewStub.layoutResource = R.layout.activity_shipping_flow
        viewStub.inflate()

        viewModel = ViewModelProviders.of(
            this,
            PaymentFlowViewModel.Factory(customerSession, args.paymentSessionData)
        )[PaymentFlowViewModel::class.java]

        paymentSessionConfig = args.paymentSessionConfig

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
                if (paymentFlowPagerAdapter.getPageAt(i) === PaymentFlowPage.SHIPPING_INFO) {
                    paymentFlowPagerAdapter.hideShippingPage()
                }
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })

        if (paymentSessionConfig.shippingInformationValidator == null) {
            shippingInfoSubmittedBroadcastReceiver = createShippingInfoSubmittedBroadcastReceiver()
        }

        title = paymentFlowPagerAdapter.getPageTitle(shipping_flow_viewpager.currentItem)
    }

    private fun createShippingInfoSubmittedBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
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
                        shippingMethods.orEmpty(),
                        defaultShippingMethod
                    )
                } else {
                    val errorMessage =
                        intent.getStringExtra(PaymentFlowExtras.EXTRA_SHIPPING_INFO_ERROR)
                    onShippingInfoError(errorMessage)
                }
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        shipping_flow_viewpager.currentItem = savedInstanceState.getInt(STATE_CURRENT_ITEM, 0)
    }

    public override fun onActionSave() {
        if (PaymentFlowPage.SHIPPING_INFO ==
            paymentFlowPagerAdapter.getPageAt(shipping_flow_viewpager.currentItem)) {
            onShippingInfoSubmitted()
        } else {
            onShippingMethodSave()
        }
    }

    override fun onPause() {
        super.onPause()

        shippingInfoSubmittedBroadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
    }

    override fun onResume() {
        super.onResume()

        shippingInfoSubmittedBroadcastReceiver?.let {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                it,
                IntentFilter(PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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
        viewModel.paymentSessionData = viewModel.paymentSessionData.copy(
            shippingInformation = shippingInformation
        )
    }

    private fun onShippingInfoValidated(
        shippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod? = null
    ) {
        viewModel.paymentSessionData.shippingInformation?.let { shippingInfo ->
            viewModel.saveCustomerShippingInformation(shippingInfo)
                .observe(this, Observer {
                    when (it.status) {
                        PaymentFlowViewModel.Status.SUCCESS -> {
                            val customer = it.data as Customer
                            onShippingInfoSaved(
                                customer.shippingInformation,
                                shippingMethods,
                                defaultShippingMethod
                            )
                        }
                        PaymentFlowViewModel.Status.ERROR -> {
                            showError(it.data as String)
                        }
                    }
                })
        }
    }

    private fun onShippingMethodsReady(
        shippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod?
    ) {
        setCommunicatingProgress(false)
        paymentFlowPagerAdapter.setShippingMethods(shippingMethods, defaultShippingMethod)
        paymentFlowPagerAdapter.setShippingInfoSaved(true)
        if (hasNextPage()) {
            shipping_flow_viewpager.currentItem = shipping_flow_viewpager.currentItem + 1
        } else {
            finishWithData(viewModel.paymentSessionData)
        }
    }

    private fun onShippingInfoSubmitted() {
        keyboardController.hide()

        shippingInfo?.let { shippingInfo ->
            viewModel.paymentSessionData = viewModel.paymentSessionData.copy(
                shippingInformation = shippingInfo
            )
            setCommunicatingProgress(true)

            val shippingInfoValidator =
                paymentSessionConfig.shippingInformationValidator
            if (shippingInfoValidator != null) {
                validateShippingInformation(
                    shippingInfoValidator,
                    paymentSessionConfig.shippingMethodsFactory,
                    shippingInfo
                )
            } else {
                broadcastShippingInfoSubmitted(shippingInfo)
            }
        }
    }

    private val shippingInfo: ShippingInformation?
        get() {
            return shipping_info_widget.shippingInformation
        }

    private val selectedShippingMethod: ShippingMethod?
        get() {
            return if (PaymentFlowPage.SHIPPING_METHOD ==
                paymentFlowPagerAdapter.getPageAt(shipping_flow_viewpager.currentItem)) {
                val selectShippingMethodWidget: SelectShippingMethodWidget =
                    findViewById(R.id.select_shipping_method_widget)
                selectShippingMethodWidget.selectedShippingMethod
            } else {
                null
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
        finishWithData(viewModel.paymentSessionData.copy(
            shippingMethod = selectShippingMethodWidget.selectedShippingMethod
        ))
        finish()
    }

    private fun validateShippingInformation(
        shippingInfoValidator: PaymentSessionConfig.ShippingInformationValidator,
        shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory?,
        shippingInformation: ShippingInformation
    ) {
        viewModel.validateShippingInformation(
            shippingInfoValidator,
            shippingMethodsFactory,
            shippingInformation
        ).observe(this, Observer {
            when (it.status) {
                PaymentFlowViewModel.Status.SUCCESS -> {
                    val shippingMethods = it.data as List<ShippingMethod>
                    // show shipping methods screen
                    onShippingInfoValidated(shippingMethods)
                }
                PaymentFlowViewModel.Status.ERROR -> {
                    val errorMessage = it.data as String
                    // show error on current screen
                    onShippingInfoError(errorMessage)
                }
            }
        })
    }

    private fun onShippingInfoError(errorMessage: String?) {
        setCommunicatingProgress(false)
        if (!errorMessage.isNullOrEmpty()) {
            showError(errorMessage)
        } else {
            showError(getString(R.string.invalid_shipping_information))
        }
        viewModel.paymentSessionData = viewModel.paymentSessionData.copy(
            shippingInformation = null
        )
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

    internal companion object {
        internal const val TOKEN_PAYMENT_FLOW_ACTIVITY: String = "PaymentFlowActivity"
        internal const val TOKEN_SHIPPING_INFO_SCREEN: String = "ShippingInfoScreen"
        internal const val TOKEN_SHIPPING_METHOD_SCREEN: String = "ShippingMethodScreen"

        private const val STATE_SHIPPING_INFO: String = "state_shipping_info"
        private const val STATE_SHIPPING_METHOD: String = "state_shipping_method"
        private const val STATE_CURRENT_ITEM: String = "state_current_item"
    }
}
