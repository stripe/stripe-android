package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.databinding.ActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.PaymentSheetViewModel.SheetMode
import com.stripe.android.paymentsheet.model.ViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PaymentSheetActivity : AppCompatActivity() {
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

    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentSheetBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            viewModelFactory
        )[PaymentSheetViewModel::class.java]
    }

    private val fragmentContainerId: Int
        @IdRes
        get() = viewBinding.fragmentContainer.id

    private val starterArgs: PaymentSheetActivityStarter.Args? by lazy {
        PaymentSheetActivityStarter.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val starterArgs = this.starterArgs
        if (starterArgs == null) {
            setPaymentSheetResult(
                PaymentResult.Failed(
                    IllegalArgumentException("PaymentSheet started without arguments."),
                    null
                )
            )
            finish()
            return
        }
        val fragmentArgs = bundleOf(EXTRA_STARTER_ARGS to starterArgs)

        setContentView(viewBinding.root)

        // Handle taps outside of bottom sheet
        viewBinding.root.setOnClickListener {
            onUserCancel()
        }
        viewModel.error.observe(this) {
            animateOut(
                PaymentResult.Failed(
                    it,
                    paymentIntent = viewModel.paymentIntent.value
                )
            )
        }
        viewModel.sheetMode.observe(this) { mode ->
            when (mode) {
                SheetMode.Full,
                SheetMode.FullCollapsed -> {
                    viewBinding.close.visibility = View.GONE
                    viewBinding.back.visibility = View.VISIBLE
                }
                SheetMode.Wrapped -> {
                    viewBinding.close.visibility = View.VISIBLE
                    viewBinding.back.visibility = View.GONE
                }
                else -> {
                    // mode == null
                }
            }

            viewBinding.bottomSheet.layoutParams = viewBinding.bottomSheet.layoutParams.apply {
                height = mode.height
            }
            if (bottomSheetBehavior.state != STATE_HIDDEN) {
                bottomSheetBehavior.state = mode.behaviourState
            }
        }

        setupBottomSheet()
        setupBuyButton()
        supportFragmentManager.commit {
            replace(
                fragmentContainerId,
                PaymentSheetLoadingFragment().also {
                    it.arguments = fragmentArgs
                }
            )
        }

        viewModel.transition.observe(this) { transactionTarget ->
            supportFragmentManager.commit {
                when (transactionTarget) {
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull -> {
                        setCustomAnimations(
                            R.anim.stripe_paymentsheet_transition_fade_in,
                            R.anim.stripe_paymentsheet_transition_fade_out,
                            R.anim.stripe_paymentsheet_transition_fade_in,
                            R.anim.stripe_paymentsheet_transition_fade_out,
                        )
                        addToBackStack(null)
                        replace(
                            fragmentContainerId,
                            PaymentSheetAddCardFragment().also {
                                it.arguments = fragmentArgs
                            }
                        )
                        viewModel.updateMode(SheetMode.Full)
                    }
                    PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                        replace(
                            fragmentContainerId,
                            PaymentSheetPaymentMethodsListFragment().also {
                                it.arguments = fragmentArgs
                            }
                        )
                        viewModel.updateMode(SheetMode.Wrapped)
                    }
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                        replace(
                            fragmentContainerId,
                            PaymentSheetAddCardFragment().also {
                                it.arguments = fragmentArgs
                            }
                        )
                        viewModel.updateMode(SheetMode.FullCollapsed)
                    }
                }
            }
        }

        viewBinding.close.setOnClickListener { onUserCancel() }
        viewBinding.back.setOnClickListener {
            viewModel.transitionTo(PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupBuyButton() {
        viewModel.viewState.observe(this) { state ->
            when (state) {
                is ViewState.Ready -> {
                    viewBinding.buyButton.onReadyState(state)
                }
                ViewState.Confirming -> {
                    viewBinding.buyButton.onConfirmingState()
                }
                is ViewState.Completed -> {
                    viewBinding.buyButton.onCompletedState {
                        val result = state.paymentIntentResult
                        when (result.outcome) {
                            StripeIntentResult.Outcome.SUCCEEDED -> {
                                animateOut(
                                    PaymentResult.Succeeded(result.intent)
                                )
                            }
                            else -> {
                                // TODO(mshafrir-stripe): handle other outcomes
                            }
                        }
                    }
                }
                else -> {
                    // no-op
                }
            }
        }

        viewModel.selection.observe(this) {
            // TODO(smaskell): show Google Pay button when GooglePay selected
            viewBinding.buyButton.isEnabled = it != null
        }
        viewBinding.buyButton.setOnClickListener {
            viewModel.checkout(this)
        }

        viewModel.processing.observe(this) { isProcessing ->
            viewBinding.close.isEnabled = !isProcessing
            viewBinding.back.isEnabled = !isProcessing

            viewBinding.buyButton.isEnabled = !isProcessing
        }
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isHideable = true
        // Start hidden and then animate in after delay
        bottomSheetBehavior.state = STATE_HIDDEN

        lifecycleScope.launch {
            delay(ANIMATE_IN_DELAY)
            bottomSheetBehavior.state = viewModel.sheetMode.value?.behaviourState ?: STATE_EXPANDED
            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == STATE_HIDDEN) {
                            finish()
                        }
                    }
                }
            )
        }
    }

    private fun animateOut(
        paymentResult: PaymentResult
    ) {
        setPaymentSheetResult(paymentResult)

        // When the bottom sheet finishes animating to its new state,
        // the callback will finish the activity
        bottomSheetBehavior.state = STATE_HIDDEN
    }

    private fun setPaymentSheetResult(
        paymentResult: PaymentResult
    ) {
        val resultCode = when (paymentResult) {
            is PaymentResult.Succeeded -> {
                Activity.RESULT_OK
            }
            is PaymentResult.Cancelled,
            is PaymentResult.Failed -> {
                Activity.RESULT_CANCELED
            }
        }
        setResult(
            resultCode,
            Intent()
                .putExtras(PaymentSheet.Result(paymentResult).toBundle())
        )
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            onUserCancel()
        }
    }

    private fun onUserCancel() {
        animateOut(
            PaymentResult.Cancelled(
                viewModel.error.value,
                paymentIntent = viewModel.paymentIntent.value
            )
        )
    }

    internal companion object {
        internal const val ANIMATE_IN_DELAY = 300L

        internal const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
    }
}
