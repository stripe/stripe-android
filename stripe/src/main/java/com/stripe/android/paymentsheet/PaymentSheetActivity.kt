package com.stripe.android.paymentsheet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.databinding.ActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.AnimationConstants
import com.stripe.android.paymentsheet.ui.BasePaymentSheetActivity
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.ui.Toolbar

internal class PaymentSheetActivity : BasePaymentSheetActivity<PaymentResult>() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        PaymentSheetViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) }
        )

    @VisibleForTesting
    internal val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(viewBinding.bottomSheet)
    }

    private val bottomSheetController: BottomSheetController by lazy {
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

    private val starterArgs: PaymentSheetActivityStarter.Args? by lazy {
        PaymentSheetActivityStarter.Args.fromIntent(intent)
    }

    override val rootView: View by lazy { viewBinding.root }

    override val messageView: TextView by lazy {
        viewBinding.message
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

        viewModel.updatePaymentMethods()
        viewModel.fetchPaymentIntent()

        setContentView(viewBinding.root)

        viewModel.fatal.observe(this) {
            animateOut(
                PaymentResult.Failed(
                    it,
                    paymentIntent = viewModel.paymentIntent.value
                )
            )
        }
        viewModel.sheetMode.observe(this) { mode ->
            when (mode) {
                SheetMode.Full -> {
                    viewBinding.toolbar.showBack()
                }
                SheetMode.FullCollapsed,
                SheetMode.Wrapped -> {
                    viewBinding.toolbar.showClose()
                }
                else -> {
                    // mode == null
                }
            }

            viewBinding.bottomSheet.layoutParams = viewBinding.bottomSheet.layoutParams.apply {
                height = mode.height
            }

            bottomSheetController.updateState(mode)

            updateRootViewClickHandling(mode.isDraggable)
        }

        bottomSheetController.shouldFinish.observe(this) { shouldFinish ->
            if (shouldFinish) {
                finish()
            }
        }
        bottomSheetController.setup()

        viewModel.googlePayCompletion.observe(this, ::onActionCompleted)

        setupBuyButton()
        supportFragmentManager.commit {
            replace(
                fragmentContainerId,
                PaymentSheetLoadingFragment()
            )
        }

        viewModel.transition.observe(this) { transitionTarget ->
            if (transitionTarget != null) {
                onTransitionTarget(
                    transitionTarget,
                    bundleOf(EXTRA_STARTER_ARGS to starterArgs)
                )
            }
        }

        viewBinding.toolbar.action.observe(this) { action ->
            when (action) {
                Toolbar.Action.Close -> {
                    onUserCancel()
                }
                Toolbar.Action.Back -> {
                    viewModel.transitionTo(
                        PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod
                    )
                }
            }
        }

        viewModel.fetchAddPaymentMethodConfig().observe(this) { config ->
            if (config != null) {
                val target = if (config.paymentMethods.isEmpty()) {
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet
                } else {
                    PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod
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
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    addToBackStack(null)
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddCardFragment().also {
                            it.arguments = fragmentArgs
                        }
                    )
                }
                PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                    replace(
                        fragmentContainerId,
                        PaymentSheetPaymentMethodsListFragment().also {
                            it.arguments = fragmentArgs
                        }
                    )
                }
                PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddCardFragment().also {
                            it.arguments = fragmentArgs
                        }
                    )
                }
            }
        }
        viewModel.updateMode(transitionTarget.sheetMode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupBuyButton() {
        viewBinding.buyButton.completedAnimation.observe(this) { completedState ->
            completedState?.paymentIntentResult?.let(::onActionCompleted)
        }

        viewModel.viewState.observe(this) { state ->
            if (state != null) {
                viewBinding.buyButton.updateState(state)
            }
        }

        viewModel.selection.observe(this) { paymentSelection ->
            val shouldShowGooglePay = paymentSelection == PaymentSelection.GooglePay

            viewBinding.googlePayButton.isVisible = shouldShowGooglePay
            viewBinding.buyButton.isVisible = !shouldShowGooglePay
            viewBinding.buyButton.isEnabled = paymentSelection != null
        }

        viewBinding.googlePayButton.setOnClickListener {
            viewModel.checkout(this)
        }

        viewBinding.buyButton.setOnClickListener {
            viewModel.checkout(this)
        }

        viewModel.processing.observe(this) { isProcessing ->
            viewBinding.toolbar.updateProcessing(isProcessing)

            viewBinding.buyButton.isEnabled = !isProcessing
        }
    }

    private fun onActionCompleted(paymentIntentResult: PaymentIntentResult) {
        when (paymentIntentResult.outcome) {
            StripeIntentResult.Outcome.SUCCEEDED -> {
                animateOut(
                    PaymentResult.Succeeded(paymentIntentResult.intent)
                )
            }
            else -> {
                // TODO(mshafrir-stripe): handle other outcomes
            }
        }
    }

    override fun setActivityResult(result: PaymentResult) {
        setResult(
            result.resultCode,
            Intent()
                .putExtras(PaymentSheet.Result(result).toBundle())
        )
    }

    override fun onUserCancel() {
        animateOut(
            PaymentResult.Cancelled(
                viewModel.fatal.value,
                paymentIntent = viewModel.paymentIntent.value
            )
        )
    }

    override fun hideSheet() {
        bottomSheetController.hide()
    }

    internal companion object {
        internal const val EXTRA_STARTER_ARGS = BasePaymentSheetActivity.EXTRA_STARTER_ARGS
    }
}
