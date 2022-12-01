package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.GooglePayDividerUi
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.AnimationConstants
import kotlinx.coroutines.launch
import java.security.InvalidParameterException

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentSheetBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

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
    override val linkAuthView: ComposeView by lazy { viewBinding.linkAuth }
    override val toolbar: MaterialToolbar by lazy { viewBinding.toolbar }
    override val testModeIndicator: TextView by lazy { viewBinding.testmode }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val header: ComposeView by lazy { viewBinding.header }
    override val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    override val messageView: TextView by lazy { viewBinding.message }
    override val notesView: ComposeView by lazy { viewBinding.notes }
    override val primaryButton: PrimaryButton by lazy { viewBinding.buyButton }
    override val bottomSpacer: View by lazy { viewBinding.bottomSpacer }

    private val buttonContainer: ViewGroup by lazy { viewBinding.buttonContainer }
    private val topContainer by lazy { viewBinding.topContainer }
    private val googlePayButton by lazy { viewBinding.googlePayButton }
    private val linkButton by lazy { viewBinding.linkButton }
    private val topMessage by lazy { viewBinding.topMessage }
    private val googlePayDivider by lazy { viewBinding.googlePayDivider }

    private val buyButtonStateObserver = { viewState: PaymentSheetViewState? ->
        updateErrorMessage(messageView, viewState?.errorMessage)
        viewBinding.buyButton.updateState(viewState?.convert())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val validatedArgs = initializeArgs()
        super.onCreate(savedInstanceState)

        val error = validatedArgs.exceptionOrNull()
        if (error != null) {
            setActivityResult(PaymentSheetResult.Failed(error))
            finish()
            return
        }

        viewModel.registerFromActivity(this)

        viewModel.setupGooglePay(
            lifecycleScope,
            registerForActivityResult(
                GooglePayPaymentMethodLauncherContract(),
                viewModel::onGooglePayResult
            )
        )

        starterArgs?.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        rootView.doOnNextLayout {
            // Show bottom sheet only after the Activity has been laid out so that it animates in
            bottomSheetController.expand()
        }

        setupTopContainer()

        linkButton.apply {
            onClick = { config ->
                viewModel.launchLink(config, launchedDirectly = false)
            }
            linkPaymentLauncher = viewModel.linkLauncher
        }

        viewModel.transition.observe(this) { transitionEvent ->
            transitionEvent?.let {
                clearErrorMessages()
                it.getContentIfNotHandled()?.let { transitionTarget ->
                    onTransitionTarget(transitionTarget)
                }
            }
        }

        if (savedInstanceState == null) {
            viewModel.transitionToFirstScreenWhenReady()
        }

        viewModel.startConfirm.observe(this) { event ->
            val confirmParams = event.getContentIfNotHandled()
            if (confirmParams != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
                lifecycleScope.launch {
                    viewModel.confirmStripeIntent(confirmParams)
                }
            }
        }

        viewModel.paymentSheetResult.observe(this) {
            closeSheet(it)
        }

        viewModel.buttonsEnabled.observe(this) { enabled ->
            linkButton.isEnabled = enabled
            googlePayButton.isEnabled = enabled
        }

        viewModel.selection.observe(this) {
            clearErrorMessages()
            resetPrimaryButtonState()
        }

        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomBuy)
            .observe(this, buyButtonStateObserver)
    }

    private fun initializeArgs(): Result<PaymentSheetContract.Args?> {
        val starterArgs = this.starterArgs

        val result = if (starterArgs == null) {
            val error = IllegalArgumentException("PaymentSheet started without arguments.")
            Result.failure(error)
        } else {
            try {
                starterArgs.config?.validate()
                starterArgs.clientSecret.validate()
                starterArgs.config?.appearance?.parseAppearance()
                Result.success(starterArgs)
            } catch (e: InvalidParameterException) {
                Result.failure(e)
            }
        }

        earlyExitDueToIllegalState = result.isFailure
        return result
    }

    private fun onTransitionTarget(
        transitionTarget: PaymentSheetViewModel.TransitionTarget,
    ) {
        val fragmentArgs = bundleOf(EXTRA_STARTER_ARGS to starterArgs)

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

    override fun resetPrimaryButtonState() {
        viewBinding.buyButton.updateState(PrimaryButton.State.Ready)

        val customLabel = starterArgs?.config?.primaryButtonLabel

        val label = if (customLabel != null) {
            starterArgs?.config?.primaryButtonLabel
        } else if (viewModel.isProcessingPaymentIntent) {
            viewModel.amount.value?.buildPayButtonLabel(resources)
        } else {
            getString(R.string.stripe_setup_button_label)
        }

        viewBinding.buyButton.setLabel(label)

        viewBinding.buyButton.setOnClickListener {
            clearErrorMessages()
            viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)
        }
    }

    private fun setupTopContainer() {
        setupGooglePayButton()
        val dividerText = resources.getString(
            if (viewModel.supportedPaymentMethods.size == 1 &&
                viewModel.supportedPaymentMethods.map { it.code }.contains(
                        LpmRepository.HardcodedCard.code
                    )
            ) {
                R.string.stripe_paymentsheet_or_pay_with_card
            } else {
                R.string.stripe_paymentsheet_or_pay_using
            }
        )
        viewModel.showTopContainer.observe(this) { visible ->
            linkButton.isVisible = viewModel.isLinkEnabled.value == true
            googlePayButton.isVisible = viewModel.isGooglePayReady.value == true
            topContainer.isVisible = visible
            // We have to set the UI after we know it's visible. Setting UI on a GONE or INVISIBLE
            // view will cause tests to hang indefinitely.
            if (visible) {
                googlePayDivider.apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        PaymentsTheme {
                            GooglePayDividerUi(dividerText)
                        }
                    }
                }
            }
        }
    }

    private fun setupGooglePayButton() {
        googlePayButton.setOnClickListener {
            // The scroll will be made visible onResume of the activity
            viewModel.setContentVisible(false)
            viewModel.lastSelectedPaymentMethod = viewModel.selection.value
            viewModel.updateSelection(PaymentSelection.GooglePay)
        }

        viewModel.selection.observe(this) { paymentSelection ->
            if (paymentSelection == PaymentSelection.GooglePay) {
                viewModel.checkout(CheckoutIdentifier.SheetTopGooglePay)
            }
        }

        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetTopGooglePay)
            .observe(this) { viewState ->
                if (viewState is PaymentSheetViewState.Reset) {
                    // If Google Pay was cancelled or failed, re-select the form payment method
                    viewModel.updateSelection(viewModel.lastSelectedPaymentMethod)
                }

                updateErrorMessage(topMessage, viewState?.errorMessage)
                googlePayButton.updateState(viewState?.convert())
            }
    }

    override fun clearErrorMessages() {
        super.clearErrorMessages()
        updateErrorMessage(topMessage)
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(PaymentSheetContract.Result(result).toBundle())
        )
    }

    override fun onDestroy() {
        if (!earlyExitDueToIllegalState) {
            viewModel.unregisterFromActivity()
        }
        super.onDestroy()
    }

    /**
     * Convert a [PaymentSheetViewState] to a [PrimaryButton.State]
     */
    private fun PaymentSheetViewState.convert(): PrimaryButton.State {
        return when (this) {
            is PaymentSheetViewState.Reset ->
                PrimaryButton.State.Ready
            is PaymentSheetViewState.StartProcessing ->
                PrimaryButton.State.StartProcessing
            is PaymentSheetViewState.FinishProcessing ->
                PrimaryButton.State.FinishProcessing(this.onComplete)
        }
    }

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
