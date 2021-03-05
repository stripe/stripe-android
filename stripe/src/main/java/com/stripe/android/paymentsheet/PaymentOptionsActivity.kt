package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.databinding.StripeActivityPaymentOptionsBinding
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
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
            sheetModeLiveData = viewModel.sheetMode,
            lifecycleScope
        )
    }

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

    override val rootView: View by lazy { viewBinding.root }
    override val bottomSheet: ConstraintLayout by lazy { viewBinding.bottomSheet }
    override val appbar: AppBarLayout by lazy { viewBinding.appbar }
    override val toolbar: Toolbar by lazy { viewBinding.toolbar }
    override val messageView: TextView by lazy { viewBinding.message }

    override val eventReporter: EventReporter by lazy {
        DefaultEventReporter(
            mode = EventReporter.Mode.Custom,
            starterArgs?.sessionId,
            application
        )
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
                    // where we also jump to a new unsaved card.  However this move require
                    // the transition target to specify when to and when not to add things to the
                    // backstack.
                    if (starterArgs.paymentMethods.isEmpty()) {
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

        viewModel.userSelection.observe(this) { paymentSelection ->
            if (paymentSelection != null) {
                onActionCompleted(paymentSelection)
            }
        }
    }

    private fun setupAddButton(addButton: AddButton) {
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

        // When using commit on the fragments, the fragment transaction happens
        // at some later time.  In order to get an accurate backstack count
        // we need to make sure the transactions have completed.  In API 24+ you can use commitNow
        supportFragmentManager.executePendingTransactions()
        viewBinding.addButton.isVisible = transitionTarget !is PaymentOptionsViewModel.TransitionTarget.SelectSavedPaymentMethod
        viewModel.updateMode(transitionTarget.sheetMode)
    }

    private fun onActionCompleted(paymentSelection: PaymentSelection) {
        closeSheet(
            PaymentOptionResult.Succeeded(paymentSelection)
        )
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
