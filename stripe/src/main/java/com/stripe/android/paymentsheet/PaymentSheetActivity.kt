package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
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
import com.stripe.android.paymentsheet.ui.Toolbar
import com.stripe.android.view.AuthActivityStarter
import java.util.Currency
import java.util.Locale

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
        BottomSheetController(
            bottomSheetBehavior = bottomSheetBehavior,
            sheetModeLiveData = viewModel.sheetMode,
            lifecycleScope
        )
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

    override val rootView: View by lazy { viewBinding.root }
    override val bottomSheet: ConstraintLayout by lazy { viewBinding.bottomSheet }
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val toolbar: Toolbar by lazy { viewBinding.toolbar }
    override val messageView: TextView by lazy { viewBinding.message }

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

    private val buyButtonLabel: String by lazy {
        resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount
        )
    }

    private val currencyFormatter = CurrencyFormatter()
    private fun getLabelText(viewState: ViewState.PaymentSheet.Ready): String {
        val currency = Currency.getInstance(
            viewState.currencyCode.toUpperCase(Locale.ROOT)
        )

        return resources.getString(
            R.string.stripe_paymentsheet_pay_button_amount,
            currencyFormatter.format(viewState.amount, currency)
        )
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
        appbar.isInvisible = true

        viewModel.fatal.observe(this) {
            closeSheet(
                PaymentResult.Failed(
                    it,
                    paymentIntent = viewModel.paymentIntent.value
                )
            )
        }

        bottomSheetController.shouldFinish.observe(this) { shouldFinish ->
            if (shouldFinish) {
                finish()
            }
        }
        bottomSheetController.setup()

        setupBuyButton()
        supportFragmentManager.commit {
            replace(
                fragmentContainerId,
                PaymentSheetLoadingFragment::class.java,
                bundleOf(
                    EXTRA_STARTER_ARGS to starterArgs
                )
            )
        }

        viewModel.transition.observe(this) { transitionTarget ->
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

        viewBinding.toolbar.action.observe(this) { action ->
            when (action) {
                Toolbar.Action.Close -> {
                    onUserCancel()
                }
                Toolbar.Action.Back -> {
                    onUserBack()
                }
                else -> {
                }
            }
        }

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
                    replace(
                        fragmentContainerId,
                        PaymentSheetListFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddCardFragment::class.java,
                        fragmentArgs
                    )
                }
            }
        }

        // When using commit on the fragments, the fragment transaction happens
        // at some later time.  In order to get an accurate backstack count
        // we need to make sure the transactions have completed.  In API 24+ you can use commitNow
        // By using commitNow, only the items in the runnable will be commited,
        // executePendingTransactions will run all the transactions even ones that were not just
        // commited.
        supportFragmentManager.executePendingTransactions()
        viewBinding.buyButton.isVisible = true
        appbar.isVisible = true
        viewModel.updateMode(transitionTarget.sheetMode)
    }

    private val viewStateObserver = { viewState: ViewState.PaymentSheet? ->
        viewBinding.buyButton.updateState(
            state = when (viewState) {
                null -> null
                is ViewState.PaymentSheet.Ready -> PrimaryButton.State.Ready(
                    getLabelText(viewState)
                )
                is ViewState.PaymentSheet.StartProcessing -> PrimaryButton.State.StartProcessing
                is ViewState.PaymentSheet.FinishProcessing -> PrimaryButton.State.FinishProcessing
            },
            completeCallback = {
                onActionCompleted(
                    (viewState as ViewState.PaymentSheet.FinishProcessing).result
                )
            }
        )
    }

    private fun setupBuyButton() {
        viewBinding.buyButton.setLabelText(buyButtonLabel)

        viewModel.viewState.observe(this, viewStateObserver)

        viewModel.selection.observe(this) { paymentSelection ->
            val shouldShowGooglePay = paymentSelection == PaymentSelection.GooglePay

            viewBinding.googlePayButton.isVisible = shouldShowGooglePay
            viewBinding.buyButton.isVisible = !shouldShowGooglePay
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewModel.checkout()
        }

        viewBinding.buyButton.setOnClickListener {
            viewModel.checkout()
        }

        viewModel.processing.observe(this) { isProcessing ->
            viewBinding.toolbar.updateProcessing(isProcessing)
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            viewBinding.buyButton.isEnabled = isEnabled
        }
    }

    private fun onActionCompleted(paymentIntentResult: PaymentIntentResult) {
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
