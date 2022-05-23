package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
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
import com.stripe.android.ui.core.elements.LpmRepository.SupportedPaymentMethod
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.GooglePayDividerUi
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.isSystemDarkTheme
import com.stripe.android.ui.core.shouldUseDarkDynamicColor
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
                starterArgs.config?.appearance?.parseAppearance()
            } catch (e: InvalidParameterException) {
                setActivityResult(PaymentSheetResult.Failed(e))
                finish()
                return
            }
        }
        super.onCreate(savedInstanceState)

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

        setupTopContainer()

        linkButton.apply {
            onClick = viewModel::launchLink
            linkPaymentLauncher = viewModel.linkLauncher
        }

        viewModel.transition.observe(this) { transitionEvent ->
            transitionEvent?.let {
                clearErrorMessages()
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

    override fun resetPrimaryButtonState() {
        viewBinding.buyButton.updateState(PrimaryButton.State.Ready)

        if (viewModel.isProcessingPaymentIntent) {
            viewBinding.buyButton.setLabel(
                viewModel.amount.value?.buildPayButtonLabel(resources)
            )
        } else {
            viewBinding.buyButton.setLabel(
                resources.getString(R.string.stripe_setup_button_label)
            )
        }

        viewBinding.buyButton.setOnClickListener {
            clearErrorMessages()
            viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)
        }
    }

    private fun setupTopContainer() {
        setupGooglePayButton()
        val dividerText = resources.getString(
            if (viewModel.supportedPaymentMethods.size == 1 &&
                viewModel.supportedPaymentMethods.contains(SupportedPaymentMethod.Card)
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
                        GooglePayDividerUi(dividerText)
                    }
                }
            }
        }
    }

    private fun setupGooglePayButton() {
        viewModel.config?.appearance?.getColors(isSystemDarkTheme())?.surface?.let {
            googlePayButton.setBackgroundColor(Color(it).shouldUseDarkDynamicColor())
        }

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

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
