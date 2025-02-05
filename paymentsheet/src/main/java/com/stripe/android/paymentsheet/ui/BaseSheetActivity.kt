package com.stripe.android.paymentsheet.ui

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.fadeOut

internal abstract class BaseSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: BaseSheetViewModel

    val linkHandler: LinkHandler
        get() = viewModel.linkHandler

    protected var earlyExitDueToIllegalState: Boolean = false

    abstract fun setActivityResult(result: ResultType)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (earlyExitDueToIllegalState) {
            return
        }

        renderEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            viewModel.handleBackPressed()
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
