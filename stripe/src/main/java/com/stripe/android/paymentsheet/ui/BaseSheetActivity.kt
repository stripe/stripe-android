package com.stripe.android.paymentsheet.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.stripe.android.paymentsheet.BottomSheetController
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.view.KeyboardController

internal abstract class BaseSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: BaseSheetViewModel<*>
    abstract val bottomSheetController: BottomSheetController
    abstract val eventReporter: EventReporter

    abstract val rootView: View
    abstract val bottomSheet: ViewGroup
    abstract val appbar: AppBarLayout
    abstract val toolbar: Toolbar
    abstract val messageView: TextView

    abstract fun onUserCancel()
    abstract fun setActivityResult(result: ResultType)

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = PaymentSheetFragmentFactory(eventReporter)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                toolbar.showClose()
            } else {
                toolbar.showBack()
            }
        }

        super.onCreate(savedInstanceState)

        viewModel.userMessage.observe(this) { userMessage ->
            messageView.isVisible = userMessage != null
            messageView.text = userMessage?.message
        }

        viewModel.processing.observe(this) { isProcessing ->
            updateRootViewClickHandling(isProcessing)
        }

        // Make `bottomSheet` clickable to prevent clicks on the bottom sheet from triggering
        // `rootView`'s click listener
        bottomSheet.isClickable = true
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            viewModel.onBackPressed()
            super.onBackPressed()
        } else {
            onUserCancel()
        }
    }

    protected fun closeSheet(
        result: ResultType
    ) {
        setActivityResult(result)
        bottomSheetController.hide()
    }

    private fun updateRootViewClickHandling(isProcessing: Boolean) {
        if (!isProcessing) {
            // Handle taps outside of bottom sheet
            rootView.setOnClickListener {
                onUserCancel()
            }
        } else {
            rootView.setOnClickListener(null)
            rootView.isClickable = false
        }
    }

    protected fun onUserBack() {
        keyboardController.hide()
        onBackPressed()
    }

    internal companion object {
        const val EXTRA_FRAGMENT_CONFIG = "com.stripe.android.paymentsheet.extra_fragment_config"
        const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
    }
}
