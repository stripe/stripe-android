package com.stripe.android.checkout

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.databinding.ActivityCheckoutBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class CheckoutActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityCheckoutBinding.inflate(layoutInflater)
    }
    private val bottomSheetBehavior by lazy {
        BottomSheetBehavior.from(viewBinding.bottomSheet)
    }
    private val viewModel by viewModels<CheckoutViewModel> {
        CheckoutViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // Handle taps outside of bottom sheet
        viewBinding.root.setOnClickListener {
            animateOut()
        }
        viewModel.error.observe(this) {
            // TODO: Communicate error to caller
            Toast.makeText(this, "Received error: ${it.message}", Toast.LENGTH_LONG).show()
            animateOut()
        }

        setupBottomSheet()

        // TODO: Add loading state
        supportFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentContainer.id, CheckoutPaymentMethodsListFragment())
            .commitAllowingStateLoss()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.isHideable = true
        // Start hidden and then animate in after delay
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        lifecycleScope.launch {
            delay(ANIMATE_IN_DELAY)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }

                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
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
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
