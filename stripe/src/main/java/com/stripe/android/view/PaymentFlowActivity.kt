package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.R
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.android.synthetic.main.activity_enter_shipping_info.*
import kotlinx.android.synthetic.main.activity_shipping_flow.*

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
class PaymentFlowActivity : StripeActivity() {

    private val customerSession: CustomerSession by lazy {
        CustomerSession.getInstance()
    }
    private val args: PaymentFlowActivityStarter.Args by lazy {
        PaymentFlowActivityStarter.Args.create(intent)
    }
    private val paymentSessionConfig: PaymentSessionConfig by lazy {
        args.paymentSessionConfig
    }
    private val viewModel: PaymentFlowViewModel by lazy {
        ViewModelProvider(
            this,
            PaymentFlowViewModel.Factory(customerSession, args.paymentSessionData)
        )[PaymentFlowViewModel::class.java]
    }

    private val paymentFlowPagerAdapter: PaymentFlowPagerAdapter by lazy {
        PaymentFlowPagerAdapter(
            this,
            paymentSessionConfig,
            customerSession,
            paymentSessionConfig.allowedShippingCountryCodes
        )
    }

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentFlowActivityStarter.Args.create(intent)
        args.windowFlags?.let { window.addFlags(it) }

        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_FLOW_ACTIVITY)
        viewStub.layoutResource = R.layout.activity_shipping_flow
        viewStub.inflate()

        val shippingInformation =
            savedInstanceState?.getParcelable(STATE_SHIPPING_INFO)
                ?: paymentSessionConfig.prepopulatedShippingInfo

        paymentFlowPagerAdapter.shippingMethods = viewModel.shippingMethods
        paymentFlowPagerAdapter.isShippingInfoSubmitted = viewModel.isShippingInfoSubmitted
        paymentFlowPagerAdapter.shippingInformation = shippingInformation
        paymentFlowPagerAdapter.selectedShippingMethod = savedInstanceState?.getParcelable(STATE_SHIPPING_METHOD)

        shipping_flow_viewpager.adapter = paymentFlowPagerAdapter
        shipping_flow_viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}

            override fun onPageSelected(i: Int) {
                title = paymentFlowPagerAdapter.getPageTitle(i)
                if (paymentFlowPagerAdapter.getPageAt(i) === PaymentFlowPage.SHIPPING_INFO) {
                    viewModel.isShippingInfoSubmitted = false
                    paymentFlowPagerAdapter.isShippingInfoSubmitted = false
                }
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })

        title = paymentFlowPagerAdapter.getPageTitle(shipping_flow_viewpager.currentItem)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_SHIPPING_INFO, shippingInfo)
        outState.putParcelable(STATE_SHIPPING_METHOD, selectedShippingMethod)
        outState.putInt(STATE_CURRENT_ITEM, shipping_flow_viewpager.currentItem)
    }

    @JvmSynthetic
    internal fun onShippingInfoSaved(
        shippingInformation: ShippingInformation?,
        shippingMethods: List<ShippingMethod> = emptyList()
    ) {
        onShippingMethodsReady(shippingMethods)
        viewModel.paymentSessionData = viewModel.paymentSessionData.copy(
            shippingInformation = shippingInformation
        )
    }

    private fun onShippingInfoValidated(shippingMethods: List<ShippingMethod>) {
        viewModel.paymentSessionData.shippingInformation?.let { shippingInfo ->
            viewModel.saveCustomerShippingInformation(shippingInfo)
                .observe(this, Observer {
                    when (it) {
                        is PaymentFlowViewModel.SaveCustomerShippingInfoResult.Success -> {
                            onShippingInfoSaved(
                                it.customer.shippingInformation,
                                shippingMethods
                            )
                        }
                        is PaymentFlowViewModel.SaveCustomerShippingInfoResult.Error -> {
                            showError(it.errorMessage)
                        }
                    }
                })
        }
    }

    private fun onShippingMethodsReady(
        shippingMethods: List<ShippingMethod>
    ) {
        isProgressBarVisible = false
        paymentFlowPagerAdapter.shippingMethods = shippingMethods
        paymentFlowPagerAdapter.isShippingInfoSubmitted = true

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
            isProgressBarVisible = true

            validateShippingInformation(
                paymentSessionConfig.shippingInformationValidator,
                paymentSessionConfig.shippingMethodsFactory,
                shippingInfo
            )
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
            when (it) {
                is PaymentFlowViewModel.ValidateShippingInfoResult.Success -> {
                    // show shipping methods screen
                    onShippingInfoValidated(it.shippingMethods)
                }
                is PaymentFlowViewModel.ValidateShippingInfoResult.Error -> {
                    // show error on current screen
                    onShippingInfoError(it.errorMessage)
                }
            }
        })
    }

    private fun onShippingInfoError(errorMessage: String?) {
        isProgressBarVisible = false
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
