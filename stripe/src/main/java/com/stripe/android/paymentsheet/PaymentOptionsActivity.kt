package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.R
import com.stripe.android.databinding.StripeActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.Toolbar

/**
 * An `Activity` for selecting a payment option.
 */
internal class PaymentOptionsActivity : BaseSheetActivity<PaymentOptionResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        StripeActivityPaymentOptionsBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentOptionsViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) }
        )

    override val viewModel: PaymentOptionsViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentOptionContract.Args? by lazy {
        PaymentOptionContract.Args.fromIntent(intent)
    }

    @VisibleForTesting
    internal val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottomSheet) }

    override val bottomSheetController: BottomSheetController by lazy {
        BottomSheetController(
            bottomSheetBehavior = bottomSheetBehavior,
            lifecycleScope = lifecycleScope
        )
    }

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val toolbar: Toolbar by lazy { viewBinding.toolbar }
    override val scrollView: ScrollView by lazy { viewBinding.scrollView }
    override val messageView: TextView by lazy { viewBinding.message }

    override val eventReporter: EventReporter by lazy {
        DefaultEventReporter(
            mode = EventReporter.Mode.Custom,
            starterArgs?.sessionId,
            application
        )
    }

    @VisibleForTesting
    private val viewStateObserver = { viewState: ViewState.PaymentOptions? ->
        val addButton = viewBinding.addButton
        when (viewState) {
            is ViewState.PaymentOptions.Ready -> addButton.updateState(
                PrimaryButton.State.Ready(getString(R.string.stripe_paymentsheet_add_button_label))
            )
            is ViewState.PaymentOptions.StartProcessing -> addButton.updateState(
                PrimaryButton.State.StartProcessing
            )
            is ViewState.PaymentOptions.FinishProcessing -> addButton.updateState(
                PrimaryButton.State.FinishProcessing(viewState.onComplete)
            )
            is ViewState.PaymentOptions.ProcessResult -> processResult(
                viewState.result
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            finish()
            return
        }

        starterArgs.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        viewModel.fatal.observe(this) {
            closeSheet(
                PaymentOptionResult.Failed(it)
            )
        }
        bottomSheetController.shouldFinish.observe(this) { shouldFinish ->
            if (shouldFinish) {
                finish()
            }
        }
        bottomSheetController.setup()

        setupAddButton(viewBinding.addButton)

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

        viewModel.fetchFragmentConfig().observe(this) { config ->
            if (config != null) {
                viewModel.transitionTo(
                    // It would be nice to see this condition move into the PaymentOptionsListFragment
                    // where we also jump to a new unsaved card. However this move require
                    // the transition target to specify when to and when not to add things to the
                    // backstack.
                    if (starterArgs.paymentMethods.isEmpty() && !config.isGooglePayReady) {
                        PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodSheet(config)
                    } else {
                        PaymentOptionsViewModel.TransitionTarget.SelectSavedPaymentMethod(config)
                    }
                )
            }
        }

        viewBinding.toolbar.action.observe(this) { action ->
            if (action != null) {
                when (action) {
                    Toolbar.Action.Close -> {
                        onUserCancel()
                    }
                    Toolbar.Action.Back -> {
                        onUserBack()
                    }
                }
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
                    viewBinding.addButton.isVisible = fragment is PaymentOptionsAddCardFragment
                }
            },
            false
        )
    }

    private fun setupAddButton(addButton: PrimaryButton) {
        viewModel.viewState.observe(this, viewStateObserver)

        addButton.setOnClickListener {
            viewModel.onUserSelection()
        }

        viewModel.processing.observe(this) { isProcessing ->
            viewBinding.toolbar.updateProcessing(isProcessing)
        }

        viewModel.ctaEnabled.observe(this) { isEnabled ->
            addButton.isEnabled = isEnabled
        }
    }

    private fun onTransitionTarget(
        transitionTarget: PaymentOptionsViewModel.TransitionTarget,
        fragmentArgs: Bundle
    ) {
        supportFragmentManager.commit {
            when (transitionTarget) {
                is PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    addToBackStack(null)

                    replace(
                        fragmentContainerId,
                        PaymentOptionsAddCardFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentOptionsViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                    replace(
                        fragmentContainerId,
                        PaymentOptionsListFragment::class.java,
                        fragmentArgs
                    )
                }
                is PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    replace(
                        fragmentContainerId,
                        PaymentOptionsAddCardFragment::class.java,
                        fragmentArgs
                    )
                }
            }
        }
    }

    private fun processResult(result: PaymentOptionResult) {
        closeSheet(result)
    }

    override fun setActivityResult(result: PaymentOptionResult) {
        setResult(
            result.resultCode,
            Intent()
                .putExtras(result.toBundle())
        )
    }

    override fun onUserCancel() {
        closeSheet(viewModel.getPaymentOptionResult())
    }

    internal companion object {
        internal const val EXTRA_FRAGMENT_CONFIG = BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
