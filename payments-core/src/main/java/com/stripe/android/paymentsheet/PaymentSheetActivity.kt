package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.R
import com.stripe.android.databinding.ActivityPaymentSheetBinding
import com.stripe.android.googlepaylauncher.StripeGooglePayContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel.Amount
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.launch

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            { eventReporter }
        )

    @VisibleForTesting
    internal val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottomSheet) }

    override val bottomSheetController: BottomSheetController by lazy {
        BottomSheetController(bottomSheetBehavior = bottomSheetBehavior)
    }

    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentSheetBinding.inflate(layoutInflater)
    }

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

    private val starterArgs: PaymentSheetContract.Args? by lazy {
        PaymentSheetContract.Args.fromIntent(intent)
    }

    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val toolbar: MaterialToolbar by lazy { viewBinding.toolbar }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val messageView: TextView by lazy { viewBinding.message }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }

    override val eventReporter: EventReporter by lazy {
        DefaultEventReporter(
            mode = EventReporter.Mode.Complete,
            application
        )
    }

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
        }

        viewModel.registerFromActivity(this)

        val googlePayLauncher = registerForActivityResult(
            StripeGooglePayContract()
        ) {
            viewModel.onGooglePayResult(it)
        }
        viewModel.launchGooglePay.observe(this) { event ->
            val args = event.getContentIfNotHandled()
            if (args != null) {
                googlePayLauncher.launch(args)
            }
        }

        viewModel.fetchStripeIntent()
        viewModel.initializeBillingRepository()

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        rootView.doOnNextLayout {
            // Show bottom sheet only after the Activity has been laid out so that it animates in
            bottomSheetController.expand()
        }

        setupBuyButton()

        viewModel.transition.observe(this) { event ->
            updateErrorMessage()
            val transitionTarget = event.getContentIfNotHandled()
            if (transitionTarget != null) {
                onTransitionTarget(
                    transitionTarget,
                    bundleOf(
                        EXTRA_STARTER_ARGS to starterArgs,
                        EXTRA_FRAGMENT_CONFIG to transitionTarget.fragmentConfig
                    )
                )
            }
        }

        if (savedInstanceState == null) {
            // Only fetch initial state if the activity is being created for the first time.
            // Otherwise the FragmentManager will correctly restore the previous state.
            fetchConfig()
        }

        viewModel.startConfirm.observe(this) { event ->
            val confirmParams = event.getContentIfNotHandled()
            if (confirmParams != null) {
                lifecycleScope.launch {
                    viewModel.confirmStripeIntent(
                        AuthActivityStarterHost.create(this@PaymentSheetActivity),
                        confirmParams
                    )
                }
            }
        }

        viewModel.paymentSheetResult.observe(this) {
            closeSheet(it)
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

    private fun fetchConfig() {
        viewModel.fetchFragmentConfig().observe(this) { config ->
            if (config != null) {
                val target = if (config.paymentMethods.isEmpty()) {
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet(config)
                } else {
                    PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod(config)
                }
                viewModel.transitionTo(target)
            }
        }
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

        fragmentContainerParent.doOnNextLayout {
            // Update visibility on next layout to avoid a two-step UI update
            appbar.isVisible = true
        }
    }

    private fun setupBuyButton() {
        if (viewModel.isProcessingPaymentIntent) {
            viewModel.amount.observe(this) {
                viewBinding.buyButton.setLabel(getLabelText(it))
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
                paymentSelection == PaymentSelection.GooglePay && supportFragmentManager.findFragmentById(
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
            updateErrorMessage()
            viewModel.checkout(CheckoutIdentifier.SheetBottomGooglePay)
        }

        viewModel.config?.primaryButtonColor?.let {
            viewBinding.buyButton.backgroundTintList = it
        }

        viewBinding.buyButton.setOnClickListener {
            updateErrorMessage()
            viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.buyButton.isEnabled = isEnabled
        }
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
