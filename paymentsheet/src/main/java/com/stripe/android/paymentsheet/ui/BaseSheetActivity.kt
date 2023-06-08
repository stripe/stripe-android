package com.stripe.android.paymentsheet.ui

import android.animation.LayoutTransition
import android.content.pm.ActivityInfo
import android.graphics.Insets
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.stripe.android.paymentsheet.BottomSheetController
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.isSystemDarkTheme
import com.stripe.android.utils.AnimationConstants
import kotlin.math.roundToInt

internal abstract class BaseSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: BaseSheetViewModel

    val linkHandler: LinkHandler
        get() = viewModel.linkHandler

    @VisibleForTesting
    internal val bottomSheetBehavior by lazy { BottomSheetBehavior.from(bottomSheet) }

    private val bottomSheetController: BottomSheetController by lazy {
        BottomSheetController(bottomSheetBehavior = bottomSheetBehavior)
    }

    abstract val rootView: ViewGroup
    abstract val bottomSheet: ViewGroup

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

        bottomSheet.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        bottomSheetController.setup(bottomSheet)

        bottomSheetController.shouldFinish.launchAndCollectIn(this) { shouldFinish ->
            if (shouldFinish) {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback {
            viewModel.handleBackPressed()
        }

        viewModel.processing.launchAndCollectIn(this) { isProcessing ->
            updateRootViewClickHandling(isProcessing)
        }

        // Make `bottomSheet` clickable to prevent clicks on the bottom sheet from triggering
        // `rootView`'s click listener
        bottomSheet.isClickable = true

        val isDark = baseContext.isSystemDarkTheme()
        viewModel.config?.let {
            bottomSheet.setBackgroundColor(
                Color(it.appearance.getColors(isDark).surface).toArgb()
            )
        }

        setSheetWidthForTablets()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(AnimationConstants.FADE_IN, AnimationConstants.FADE_OUT)
    }

    protected fun closeSheet(
        result: ResultType
    ) {
        // TODO(mlb): Consider if this needs to be an abstract function
        setActivityResult(result)
        bottomSheetController.hide()
    }

    private fun updateRootViewClickHandling(isProcessing: Boolean) {
        if (!isProcessing) {
            // Handle taps outside of bottom sheet
            rootView.setOnClickListener {
                viewModel.onUserCancel()
            }
        } else {
            rootView.setOnClickListener(null)
            rootView.isClickable = false
        }
    }

    private fun setSheetWidthForTablets() {
        if (!resources.getBoolean(R.bool.stripe_is_tablet)) {
            return
        }

        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

            windowMetrics.bounds.width() - insets.left - insets.right
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            displayMetrics.widthPixels
        }

        val params = bottomSheet.layoutParams
        val clParams = params as CoordinatorLayout.LayoutParams
        clParams.gravity = clParams.gravity or Gravity.CENTER_HORIZONTAL
        clParams.width = (screenWidth * TABLET_WIDTH_RATIO).roundToInt()
        bottomSheet.layoutParams = clParams
    }

    internal companion object {
        const val EXTRA_STARTER_ARGS = "com.stripe.android.paymentsheet.extra_starter_args"
        const val TABLET_WIDTH_RATIO = .6
    }

    internal data class ToolbarResources(
        @DrawableRes val icon: Int,
        @StringRes val description: Int
    )
}
