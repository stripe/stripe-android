package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.databinding.ActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.CurrencyFormatter
import com.stripe.android.ui.core.PaymentsThemeConfig
import com.stripe.android.ui.core.isSystemDarkTheme
import kotlinx.coroutines.launch
import java.security.InvalidParameterException

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentSheetBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            this,
            intent?.extras
        )

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentSheetContract.Args? by lazy {
        PaymentSheetContract.Args.fromIntent(intent)
    }

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val toolbar: MaterialToolbar by lazy { viewBinding.toolbar }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val messageView: TextView by lazy { viewBinding.message }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    override val testModeIndicator: TextView by lazy { viewBinding.testmode }
    private val buttonContainer: ViewGroup by lazy { viewBinding.buttonContainer }

    private val currencyFormatter = CurrencyFormatter()

    private val buyButtonStateObserver = { viewState: PaymentSheetViewState? ->
        updateErrorMessage(viewState?.errorMessage)
        viewBinding.buyButton.updateState(viewState?.convert())
    }

    private val googlePayButtonStateObserver = { viewState: PaymentSheetViewState? ->
        updateErrorMessage(viewState?.errorMessage)
        viewBinding.googlePayButton.updateState(viewState?.convert())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            setActivityResult(
                PaymentSheetResult.Failed(
                    IllegalArgumentException("PaymentSheet started without arguments.")
                )
            )
            finish()
            return
        } else {
            try {
                starterArgs.config?.validate()
                starterArgs.clientSecret.validate()
            } catch (e: InvalidParameterException) {
                setActivityResult(PaymentSheetResult.Failed(e))
                finish()
                return
            }
        }

        viewModel.registerFromActivity(this)
        viewModel.setupGooglePay(
            lifecycleScope,
            registerForActivityResult(
                GooglePayPaymentMethodLauncherContract(),
                viewModel::onGooglePayResult
            )
        )

        if (!viewModel.maybeFetchStripeIntent()) {
            // The buy button needs to be made visible since it is gone in the xml
            buttonContainer.isVisible = true
            viewBinding.buyButton.isVisible = true
        }

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        rootView.doOnNextLayout {
            // Show bottom sheet only after the Activity has been laid out so that it animates in
            bottomSheetController.expand()
        }

        setupBuyButton()

        viewModel.transition.observe(this) { transitionEvent ->
            transitionEvent?.let {
                updateErrorMessage()
                it.getContentIfNotHandled()?.let { transitionTarget ->
                    onTransitionTarget(
                        transitionTarget,
                        bundleOf(
                            EXTRA_STARTER_ARGS to starterArgs,
                            EXTRA_FRAGMENT_CONFIG to transitionTarget.fragmentConfig
                        )
                    )
                }
            }
        }

        viewModel.fragmentConfigEvent.observe(this) { event ->
            val config = event.getContentIfNotHandled()
            if (config != null) {

                // We only want to do this if the loading fragment is shown.  Otherwise this causes
                // a new fragment to be created if the activity was destroyed and recreated.
                if (supportFragmentManager.fragments.firstOrNull() is PaymentSheetLoadingFragment) {
                    val target = if (viewModel.paymentMethods.value.isNullOrEmpty()) {
                        viewModel.updateSelection(null)
                        PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet(config)
                    } else {
                        PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod(config)
                    }
                    viewModel.transitionTo(target)
                }
            }
        }

        viewModel.startConfirm.observe(this) { event ->
            val confirmParams = event.getContentIfNotHandled()
            if (confirmParams != null) {
                lifecycleScope.launch {
                    viewModel.confirmStripeIntent(confirmParams)
                }
            }
        }

        viewModel.paymentSheetResult.observe(this) {
            closeSheet(it)
        }

        viewModel.contentVisible.observe(this) {
            viewBinding.scrollView.isVisible = it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterFromActivity()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            updateErrorMessage()
        }
        super.onBackPressed()
    }

    private fun updateErrorMessage(userMessage: BaseSheetViewModel.UserErrorMessage? = null) {
        messageView.isVisible = userMessage != null
        messageView.text = userMessage?.message
    }

    private fun onTransitionTarget(
        transitionTarget: PaymentSheetViewModel.TransitionTarget,
        fragmentArgs: Bundle
    ) {
        supportFragmentManager.commit {
            when (transitionTarget) {
                is PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    addToBackStack(null)
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddPaymentMethodFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    replace(
                        fragmentContainerId,
                        PaymentSheetListFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddPaymentMethodFragment::class.java,
                        fragmentArgs
                    )
                }
            }
        }

        buttonContainer.isVisible = true
    }

    private fun setupBuyButton() {
        if (viewModel.isProcessingPaymentIntent) {
            viewModel.amount.observe(this) {
                viewBinding.buyButton.setLabel(getLabelText(requireNotNull(it)))
            }
        } else {
            viewBinding.buyButton.setLabel(
                resources.getString(R.string.stripe_paymentsheet_setup_button_label)
            )
        }

        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomBuy)
            .observe(this, buyButtonStateObserver)
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomGooglePay)
            .observe(this, googlePayButtonStateObserver)

        viewModel.selection.observe(this) { paymentSelection ->
            updateErrorMessage()

            val shouldShowGooglePay =
                paymentSelection ==
                    PaymentSelection.GooglePay && supportFragmentManager.findFragmentById(
                    fragmentContainerId
                ) is PaymentSheetListFragment

            if (shouldShowGooglePay) {
                viewBinding.googlePayButton.bringToFront()
                viewBinding.googlePayButton.isVisible = true
                viewBinding.buyButton.isVisible = false
            } else {
                viewBinding.buyButton.bringToFront()
                viewBinding.buyButton.isVisible = true
                viewBinding.googlePayButton.isVisible = false
            }
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewModel.setContentVisible(false)
            updateErrorMessage()
            viewModel.checkout(CheckoutIdentifier.SheetBottomGooglePay)
        }

        val isDark = baseContext.isSystemDarkTheme()
        viewBinding.buyButton.setDefaultBackGroundColor(
            if (viewModel.config?.primaryButtonColor != null) {
                viewModel.config?.primaryButtonColor
            } else {
                ColorStateList.valueOf(
                    PaymentsThemeConfig.colors(isDark).primary.toArgb()
                )
            }
        )

        viewBinding.buyButton.setOnClickListener {
            updateErrorMessage()
            viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.buyButton.isEnabled = isEnabled
            viewBinding.googlePayButton.isEnabled = isEnabled
        }

        viewBinding.bottomSheet.setBackgroundColor(
            PaymentsThemeConfig.colors(isDark).surface.toArgb()
        )
        viewBinding.toolbar.setBackgroundColor(
            PaymentsThemeConfig.colors(isDark).surface.toArgb()
        )
    }

    private fun getLabelText(amount: Amount): String {
        return resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(amount.value, amount.currencyCode)
        )
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(PaymentSheetContract.Result(result).toBundle())
        )
    }

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
