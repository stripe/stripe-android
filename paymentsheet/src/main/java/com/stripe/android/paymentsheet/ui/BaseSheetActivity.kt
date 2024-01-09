package com.stripe.android.paymentsheet.ui

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.fadeOut

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

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            // In Oreo, Activities where `android:windowIsTranslucent=true` can't request
            // orientation. See https://stackoverflow.com/a/50832408/11103900
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

    private fun renderEdgeToEdge() {
        if (Build.VERSION.SDK_INT < 30) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
