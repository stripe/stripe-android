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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentRelayContract
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.databinding.ActivityPaymentSheetBinding
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.Stripe3ds2CompletionContract
import com.stripe.android.paymentsheet.PaymentSheetViewModel.Amount
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.launch

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) }
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
            starterArgs?.sessionId,
            application
        )
    }

    private lateinit var paymentController: PaymentController

    private val paymentConfig: PaymentConfiguration by lazy {
        PaymentConfiguration.getInstance(application)
    }

    private val currencyFormatter = CurrencyFormatter()

    private val buyButtonStateObserver = { viewState: ViewState.PaymentSheet? ->
        viewBinding.buyButton.updateState(viewState?.convert())
    }

    private val googlePayButtonStateObserver = { viewState: ViewState.PaymentSheet? ->
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

        val paymentRelayLauncher = registerForActivityResult(
            PaymentRelayContract()
        ) {
            viewModel.onPaymentFlowResult(it)
        }
        val paymentBrowserAuthLauncher = registerForActivityResult(
            PaymentBrowserAuthContract(
                DefaultReturnUrl.create(application)
            )
        ) {
            viewModel.onPaymentFlowResult(it)
        }
        val stripe3ds2ChallengeLauncher = registerForActivityResult(
            Stripe3ds2CompletionContract()
        ) {
            viewModel.onPaymentFlowResult(it)
        }
        paymentController = StripePaymentController(
            application,
            paymentConfig.publishableKey,
            StripeApiRepository(
                application,
                paymentConfig.publishableKey
            ),
            true,
            paymentRelayLauncher = paymentRelayLauncher,
            paymentBrowserAuthLauncher = paymentBrowserAuthLauncher,
            stripe3ds2ChallengeLauncher = stripe3ds2ChallengeLauncher
        )

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

        viewModel.updatePaymentMethods()
        viewModel.fetchStripeIntent()

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        viewModel.fatal.observe(this) {
            closeSheet(PaymentSheetResult.Failed(it))
        }

        rootView.doOnNextLayout {
            // Show bottom sheet only after the Activity has been laid out so that it animates in
            bottomSheetController.expand()
        }

        setupBuyButton()

        viewModel.transition.observe(this) { event ->
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
                    paymentController.startConfirmAndAuth(
                        AuthActivityStarter.Host.create(this@PaymentSheetActivity),
                        confirmParams,
                        ApiRequest.Options(
                            apiKey = paymentConfig.publishableKey,
                            stripeAccount = paymentConfig.stripeAccountId
                        )
                    )
                }
            }
        }

        // This needs to be handled in the case where the google pay button on the add
        // fragment is listening for events.  The page still needs to know to close the sheet.
        viewModel.viewState.observe(this) { viewState ->
            if (viewState is ViewState.PaymentSheet.ProcessResult<*>) {
                processResult(viewState.result)
            }
        }
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
                        PaymentSheetAddCardFragment::class.java,
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
                        PaymentSheetAddCardFragment::class.java,
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
        viewModel.amount.observe(this) {
            viewBinding.buyButton.setLabel(getLabelText(it))
        }

        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomBuy)
            .observe(this, buyButtonStateObserver)
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomGooglePay)
            .observe(this, googlePayButtonStateObserver)

        viewModel.selection.observe(this) { paymentSelection ->
            val shouldShowGooglePay =
                paymentSelection == PaymentSelection.GooglePay && supportFragmentManager.findFragmentById(
                    fragmentContainerId
                ) is PaymentSheetListFragment

            viewBinding.googlePayButton.isVisible = shouldShowGooglePay
            viewBinding.buyButton.isVisible = !shouldShowGooglePay
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewModel.checkout(CheckoutIdentifier.SheetBottomGooglePay)
        }

        viewModel.config?.primaryButtonColor?.let {
            viewBinding.buyButton.backgroundTintList = it
        }

        viewBinding.buyButton.setOnClickListener {
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

    private fun processResult(stripeIntentResult: StripeIntentResult<*>) {
        when (stripeIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                closeSheet(PaymentSheetResult.Completed)
            }
            else -> {
                // TODO(mshafrir-stripe): handle other outcomes
            }
        }
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(PaymentSheetContract.Result(result).toBundle())
        )
    }

    override fun onUserCancel() {
        closeSheet(PaymentSheetResult.Canceled)
    }

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
