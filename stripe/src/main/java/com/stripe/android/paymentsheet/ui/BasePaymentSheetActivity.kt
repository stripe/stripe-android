package com.stripe.android.paymentsheet.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal abstract class BasePaymentSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: SheetViewModel<*, *>

    abstract val rootView: View
    abstract val messageView: TextView

    abstract fun onUserCancel()
    abstract fun hideSheet()
    abstract fun setActivityResult(result: ResultType)

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

    protected companion object {
        const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
    }
}
