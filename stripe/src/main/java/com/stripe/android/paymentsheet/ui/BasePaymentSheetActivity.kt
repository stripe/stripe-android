package com.stripe.android.paymentsheet.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.KeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal abstract class BasePaymentSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: SheetViewModel<*, *>

    abstract val rootView: View
    abstract val messageView: TextView

    abstract fun onUserCancel()
    abstract fun hideSheet()
    abstract fun setActivityResult(result: ResultType)

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.userMessage.observe(this) { userMessage ->
            messageView.isVisible = userMessage != null
            messageView.text = userMessage?.message
        }

        updateRootViewClickHandling(true)
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

    protected fun animateOut(
        result: ResultType
    ) {
        setActivityResult(result)
        hideSheet()
    }

    protected fun updateRootViewClickHandling(isDraggable: Boolean) {
        if (isDraggable) {
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
        lifecycleScope.launch {
            // add the smallest possible delay before invoking `onBackPressed()` to prevent
            // layout issues
            delay(1)
            rootView.post {
                onBackPressed()
            }
        }
    }

    protected companion object {
        const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
    }
}
