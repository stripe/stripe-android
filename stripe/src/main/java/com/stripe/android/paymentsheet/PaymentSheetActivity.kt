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
import com.stripe.android.PaymentIntentResult
import com.stripe.android.PaymentRelayContract
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.databinding.ActivityPaymentSheetBinding
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.payments.Stripe3ds2CompletionContract
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.view.AuthActivityStarter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class PaymentSheetActivity : BaseSheetActivity<PaymentResult>() {
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

    private var job: Job? = null

    private lateinit var paymentController: PaymentController

    private val paymentConfig: PaymentConfiguration by lazy {
        PaymentConfiguration.getInstance(application)
    }

    private val currencyFormatter = CurrencyFormatter()
    private fun getLabelText(viewState: ViewState.PaymentSheet.Ready): String {
        return resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(viewState.amount, viewState.currencyCode)
        )
    }

    private val viewStateObserver = { viewState: ViewState.PaymentSheet? ->
        when (viewState) {
            is ViewState.PaymentSheet.Ready -> viewBinding.buyButton.updateState(
                PrimaryButton.State.Ready(getLabelText(viewState))
            )
            is ViewState.PaymentSheet.StartProcessing -> viewBinding.buyButton.updateState(
                PrimaryButton.State.StartProcessing
            )
            is ViewState.PaymentSheet.FinishProcessing -> viewBinding.buyButton.updateState(
                PrimaryButton.State.FinishProcessing(viewState.onComplete)
            )
            is ViewState.PaymentSheet.ProcessResult -> processResult(
                viewState.result
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            setActivityResult(
                PaymentResult.Failed(
                    IllegalArgumentException("PaymentSheet started without arguments."),
                    null
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
        val paymentAuthWebViewLauncher = registerForActivityResult(
            PaymentAuthWebViewContract()
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
            paymentAuthWebViewLauncher = paymentAuthWebViewLauncher,
            stripe3ds2ChallengeLauncher = stripe3ds2ChallengeLauncher
        )

        val googlePayLauncher = registerForActivityResult(
            StripeGooglePayContract()
        ) {
            viewModel.onGooglePayResult(it)
        }
        viewModel.launchGooglePay.observe(this) { args ->
            if (args != null) {
                googlePayLauncher.launch(args)
            }
        }

        viewModel.updatePaymentMethods()
        viewModel.fetchPaymentIntent()

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        viewModel.fatal.observe(this) {
            closeSheet(
                PaymentResult.Failed(
                    it,
                    paymentIntent = viewModel.paymentIntent.value
                )
            )
        }

        rootView.doOnNextLayout {
            // Show bottom sheet only after the Activity has been laid out so that it animates in
            bottomSheetController.expand()
        }

        setupBuyButton()

        if (savedInstanceState == null) {
            // Only fetch initial state if the activity is being created for the first time.
            // Otherwise the FragmentManager will correctly restore the previous state.
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

        viewModel.startConfirm.observe(this) { confirmParams ->
            paymentController.startConfirmAndAuth(
                AuthActivityStarter.Host.create(this),
                confirmParams,
                ApiRequest.Options(
                    apiKey = paymentConfig.publishableKey,
                    stripeAccount = paymentConfig.stripeAccountId
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        job = viewModel.transitionFlow
            .onEach { transitionTarget ->
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
            .launchIn(lifecycleScope)
    }

    override fun onStop() {
        super.onStop()
        job?.cancel()
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
        viewModel.viewState.observe(this, viewStateObserver)

        viewModel.selection.observe(this) { paymentSelection ->
            val shouldShowGooglePay =
                paymentSelection == PaymentSelection.GooglePay && supportFragmentManager.findFragmentById(
                    fragmentContainerId
                ) is PaymentSheetListFragment

            viewBinding.googlePayButton.isVisible = shouldShowGooglePay
            viewBinding.buyButton.isVisible = !shouldShowGooglePay
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewModel.checkout()
        }

        viewBinding.buyButton.setOnClickListener {
            viewModel.checkout()
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.buyButton.isEnabled = isEnabled
        }
    }

    private fun processResult(paymentIntentResult: PaymentIntentResult) {
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                closeSheet(
                    PaymentResult.Completed(paymentIntentResult.intent)
                )
            }
            else -> {
                // TODO(mshafrir-stripe): handle other outcomes
            }
        }
    }

    override fun setActivityResult(result: PaymentResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(PaymentSheetContract.Result(result).toBundle())
        )
    }

    override fun onUserCancel() {
        closeSheet(
            PaymentResult.Canceled(
                viewModel.fatal.value,
                paymentIntent = viewModel.paymentIntent.value
            )
        )
    }

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
