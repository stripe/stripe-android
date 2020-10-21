package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
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
        PaymentSheetViewModel.Factory {
            application
        }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // Handle taps outside of bottom sheet
        viewBinding.root.setOnClickListener {
            onUserCancel()
        }
        viewModel.error.observe(this) {
            animateOut(
                PaymentSheet.CompletionStatus.Failed(
                    it,
                    // TODO: Set payment intent if available
                    paymentIntent = null
                )
            )
        }
        viewModel.sheetMode.observe(this) { mode ->
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
            replace(fragmentContainerId, PaymentSheetLoadingFragment())
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
                        replace(fragmentContainerId, PaymentSheetAddCardFragment())
                        viewModel.updateMode(SheetMode.Full)
                    }
                    PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod -> {
                        replace(fragmentContainerId, PaymentSheetPaymentMethodsListFragment())
                        viewModel.updateMode(SheetMode.Wrapped)
                    }
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodSheet -> {
                        replace(fragmentContainerId, PaymentSheetAddCardFragment())
                        viewModel.updateMode(SheetMode.FullCollapsed)
                    }
                }
            }
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
                                    PaymentSheet.CompletionStatus.Succeeded(result.intent)
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

    private fun animateOut(status: PaymentSheet.CompletionStatus) {
        val resultCode = when (status) {
            is PaymentSheet.CompletionStatus.Succeeded -> {
                Activity.RESULT_OK
            }
            is PaymentSheet.CompletionStatus.Cancelled,
            is PaymentSheet.CompletionStatus.Failed -> {
                Activity.RESULT_CANCELED
            }
        }
        setResult(
            resultCode,
            Intent().putExtras(PaymentSheet.Result(status).toBundle())
        )
        // When the bottom sheet finishes animating to its new state,
        // the callback will finish the activity
        bottomSheetBehavior.state = STATE_HIDDEN
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
            PaymentSheet.CompletionStatus.Cancelled(
                viewModel.error.value,
                // TODO: set payment intent if available
                paymentIntent = null
            )
        )
    }

    internal companion object {
        internal const val ANIMATE_IN_DELAY = 300L
    }
}
