package com.stripe.android.paymentsheet

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
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.R
import com.stripe.android.databinding.ActivityCheckoutBinding
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
        ActivityCheckoutBinding.inflate(layoutInflater)
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
            animateOut()
        }
        viewModel.error.observe(this) {
            // TODO: Communicate error to caller
            Snackbar.make(viewBinding.coordinator, "Received error: ${it.message}", Snackbar.LENGTH_LONG).show()
        }
        viewModel.paymentIntentResult.observe(this) {
            // TOOD: Communicate result to caller
            animateOut()
        }

        setupBottomSheet()
        setupBuyButton()

        // TODO: Add loading state
        supportFragmentManager.commit {
            replace(fragmentContainerId, PaymentSheetPaymentMethodsListFragment())
        }

        viewModel.transition.observe(this) {
            supportFragmentManager.commit {
                when (it) {
                    PaymentSheetViewModel.TransitionTarget.AddCard -> {
                        setCustomAnimations(
                            R.anim.stripe_paymentsheet_transition_enter_from_right,
                            R.anim.stripe_paymentsheet_transition_exit_to_left,
                            R.anim.stripe_paymentsheet_transition_enter_from_left,
                            R.anim.stripe_paymentsheet_transition_exit_to_right
                        )
                        addToBackStack(null)
                        replace(fragmentContainerId, PaymentSheetAddCardFragment())
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
        // TOOD(smaskell): Set text based on currency & amount in payment intent
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
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
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

    private fun animateOut() {
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
            animateOut()
        }
    }

    private companion object {
        private const val ANIMATE_IN_DELAY = 300L
    }
}
