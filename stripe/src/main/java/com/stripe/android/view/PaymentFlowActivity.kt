package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.R
import com.stripe.android.databinding.PaymentFlowActivityBinding
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod

/**
 * Activity containing a two-part payment flow that allows users to provide a shipping address
 * as well as select a shipping method.
 */
class PaymentFlowActivity : StripeActivity() {

    private val viewBinding: PaymentFlowActivityBinding by lazy {
        viewStub.layoutResource = R.layout.payment_flow_activity
        val root = viewStub.inflate() as ViewGroup
        PaymentFlowActivityBinding.bind(root)
    }

    private val viewPager: PaymentFlowViewPager by lazy {
        viewBinding.shippingFlowViewpager
    }

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
            paymentSessionConfig.allowedShippingCountryCodes
        ) {
            viewModel.selectedShippingMethod = it
        }
    }

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentFlowActivityStarter.Args.create(intent)
        args.windowFlags?.let { window.addFlags(it) }

        val shippingInformation =
            viewModel.submittedShippingInfo
                ?: paymentSessionConfig.prepopulatedShippingInfo

        paymentFlowPagerAdapter.shippingMethods = viewModel.shippingMethods
        paymentFlowPagerAdapter.isShippingInfoSubmitted = viewModel.isShippingInfoSubmitted
        paymentFlowPagerAdapter.shippingInformation = shippingInformation
        paymentFlowPagerAdapter.selectedShippingMethod = viewModel.selectedShippingMethod

        viewPager.adapter = paymentFlowPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}

            override fun onPageSelected(i: Int) {
                title = paymentFlowPagerAdapter.getPageTitle(i)
                if (paymentFlowPagerAdapter.getPageAt(i) === PaymentFlowPage.ShippingInfo) {
                    viewModel.isShippingInfoSubmitted = false
                    paymentFlowPagerAdapter.isShippingInfoSubmitted = false
                }
            }

            override fun onPageScrollStateChanged(i: Int) {
            }
        })

        viewPager.currentItem = viewModel.currentPage
        title = paymentFlowPagerAdapter.getPageTitle(viewPager.currentItem)
    }

    public override fun onActionSave() {
        if (PaymentFlowPage.ShippingInfo ==
            paymentFlowPagerAdapter.getPageAt(viewPager.currentItem)) {
            onShippingInfoSubmitted()
        } else {
            onShippingMethodSave()
        }
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
            viewModel.currentPage += 1
            viewPager.currentItem = viewModel.currentPage
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
            return viewPager
                .findViewById<ShippingInfoWidget>(R.id.shipping_info_widget)
                .shippingInformation
        }

    private fun hasNextPage(): Boolean {
        return viewPager.currentItem + 1 < paymentFlowPagerAdapter.count
    }

    private fun hasPreviousPage(): Boolean {
        return viewPager.currentItem != 0
    }

    private fun onShippingMethodSave() {
        val selectedShippingMethod =
            viewPager
                .findViewById<SelectShippingMethodWidget>(R.id.select_shipping_method_widget)
                .selectedShippingMethod
        finishWithData(viewModel.paymentSessionData.copy(
            shippingMethod = selectedShippingMethod
        ))
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
            viewModel.currentPage -= 1
            viewPager.currentItem = viewModel.currentPage
        } else {
            super.onBackPressed()
        }
    }

    internal companion object {
        internal const val PRODUCT_TOKEN: String = "PaymentFlowActivity"
    }
}
