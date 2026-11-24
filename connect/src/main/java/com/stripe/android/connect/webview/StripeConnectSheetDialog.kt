package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A bottom sheet over a dimmed screen that shows a window opened by an inline component.
 *
 * The sheet draws only a grabber; the web content draws its own header and close button. Until [reveal], the sheet
 * is laid out but invisible and doesn't take touches, so Connect.js can draw and measure its dialog. A swipe down,
 * a tap outside, or back dismisses it and calls [onDismissed], or calls [onCloseRequested] while it isn't
 * dismissible.
 */
@SuppressLint("ViewConstructor")
@Suppress("TooManyFunctions", "MagicNumber")
internal class StripeConnectSheetDialog(
    activity: Activity,
    private val webView: WebView,
    private val onDismissed: () -> Unit,
    private val onCloseRequested: () -> Unit,
) : ComponentDialog(activity, android.R.style.Theme_Translucent_NoTitleBar) {

    private val density = activity.resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density).roundToInt()

    private val root = FrameLayout(activity)

    @VisibleForTesting
    internal val scrim = View(activity)
    private val sheet = LinearLayout(activity)

    @VisibleForTesting
    internal val handleArea = LinearLayout(activity)
    private val grabber = View(activity)
    private val sheetBackground = GradientDrawable()

    private var contentHeight: Float? = null
    private var topInset = 0
    private var bottomInset = 0

    private var isRevealed = false
    private var isClosing = false
    private var isDismissible = true

    init {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        onBackPressedDispatcher.addCallback(this) {
            requestDismiss()
        }
        buildViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(root)
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setWindowAnimations(0)
            @Suppress("DEPRECATION")
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            // Edge to edge, so the scrim covers the system bars instead of leaving them undimmed.
            WindowCompat.setDecorFitsSystemWindows(this, false)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION")
            statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isStatusBarContrastEnforced = false
                isNavigationBarContrastEnforced = false
            }
            if (!isRevealed) {
                addFlags(STAGED_WINDOW_FLAGS)
            }
        }
    }

    /**
     * Updates the sheet's content height, in CSS pixels, and colors.
     */
    fun bind(height: Float?, isDismissible: Boolean, @ColorInt backgroundColor: Int, @ColorInt textColor: Int) {
        this.isDismissible = isDismissible
        contentHeight = height
        updateColors(backgroundColor, textColor)
        updateSheetHeight()
    }

    fun updateColors(@ColorInt backgroundColor: Int, @ColorInt textColor: Int) {
        sheetBackground.setColor(backgroundColor)
        // The web content only paints its own height, so the web view's background fills the rest.
        webView.setBackgroundColor(backgroundColor)
        grabber.background = GradientDrawable().apply {
            cornerRadius = dp(2).toFloat()
            setColor(ColorUtils.setAlphaComponent(textColor, 0x33))
        }
    }

    /**
     * Shows the staged sheet to the user. Safe to call more than once.
     */
    fun reveal() {
        if (isRevealed || isClosing) return
        isRevealed = true
        window?.clearFlags(STAGED_WINDOW_FLAGS)
        root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        sheet.alpha = 1f
        sheet.translationY = max(sheet.height, dp(MIN_REVEAL_OFFSET_DP)).toFloat()
        sheet.animate()
            .translationY(0f)
            .setDuration(REVEAL_ANIMATION_MILLIS)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
        scrim.animate().alpha(1f).setDuration(REVEAL_ANIMATION_MILLIS).start()
    }

    /**
     * Animates the sheet away because its window closed, then calls [onClosed]. Safe to call more than once.
     */
    fun close(onClosed: () -> Unit) {
        animateAway(onClosed)
    }

    /**
     * Dismisses immediately without telling anyone, for when the sheet's state already says it's gone or the
     * activity is going away.
     */
    fun release() {
        isClosing = true
        sheet.animate().cancel()
        scrim.animate().cancel()
        (webView.parent as? ViewGroup)?.removeView(webView)
        if (isShowing) {
            dismiss()
        }
    }

    private fun requestDismiss() {
        if (!isRevealed || isClosing) return
        if (isDismissible) {
            animateAway(onDismissed)
        } else {
            sheet.animate().translationY(0f).setDuration(ANIMATION_MILLIS).start()
            onCloseRequested()
        }
    }

    private fun animateAway(onDone: () -> Unit) {
        if (isClosing) return
        isClosing = true
        if (!isRevealed || !isShowing) {
            release()
            onDone()
            return
        }
        scrim.animate().alpha(0f).setDuration(ANIMATION_MILLIS).start()
        sheet.animate()
            .translationY(sheet.height.toFloat())
            .setDuration(ANIMATION_MILLIS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                release()
                onDone()
            }
            .start()
    }

    private fun updateSheetHeight() {
        if (root.height == 0) return

        val maxSheetHeight = root.height - topInset - dp(TOP_GAP_DP)
        handleArea.measure(
            View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val maxContentHeight = max(0, maxSheetHeight - handleArea.measuredHeight - bottomInset)
        // Before Connect.js measures the dialog, lay it out at full height so it can render.
        val contentHeight = contentHeight?.let { ceil(it * density).toInt() } ?: maxContentHeight
        val webViewHeight = min(max(contentHeight, dp(MIN_CONTENT_HEIGHT_DP)), maxContentHeight)
        if (webView.layoutParams?.height != webViewHeight) {
            webView.updateLayoutParams<LinearLayout.LayoutParams> { height = webViewHeight }
        }
    }

    private fun buildViews() {
        root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

        scrim.setBackgroundColor(SCRIM_COLOR)
        scrim.alpha = 0f
        scrim.setOnClickListener { requestDismiss() }
        root.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))

        sheet.orientation = LinearLayout.VERTICAL
        sheetBackground.cornerRadii = FloatArray(8) { i -> if (i < 4) dp(CORNER_RADIUS_DP).toFloat() else 0f }
        sheet.background = sheetBackground
        sheet.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = dp(CORNER_RADIUS_DP)
                outline.setRoundRect(0, 0, view.width, view.height + radius, radius.toFloat())
            }
        }
        sheet.clipToOutline = true
        sheet.elevation = dp(16).toFloat()
        sheet.isClickable = true
        sheet.alpha = 0f
        root.addView(sheet, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        handleArea.orientation = LinearLayout.VERTICAL
        handleArea.minimumHeight = dp(HANDLE_AREA_HEIGHT_DP)
        handleArea.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        handleArea.addView(grabber, LinearLayout.LayoutParams(dp(32), dp(4)))
        handleArea.setOnTouchListener(DragToDismissListener())
        sheet.addView(handleArea, LinearLayout.LayoutParams(MATCH, WRAP))

        (webView.parent as? ViewGroup)?.removeView(webView)
        sheet.addView(webView, LinearLayout.LayoutParams(MATCH, dp(HANDLE_AREA_HEIGHT_DP)))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            topInset = bars.top
            bottomInset = max(bars.bottom, ime.bottom)
            sheet.updatePadding(bottom = bottomInset)
            updateSheetHeight()
            WindowInsetsCompat.CONSUMED
        }
        root.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) {
                root.post { updateSheetHeight() }
            }
        }
    }

    /**
     * Drags the sheet by its handle. A long or fast drag down asks to dismiss it; anything else springs back.
     */
    private inner class DragToDismissListener : View.OnTouchListener {
        private var startY = 0f
        private var velocityTracker: VelocityTracker? = null

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (!isRevealed || isClosing) return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                    sheet.animate().cancel()
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    sheet.translationY = max(0f, event.rawY - startY)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onDragEnd(event)
            }
            return true
        }

        private fun onDragEnd(event: MotionEvent) {
            val tracker = velocityTracker
            tracker?.addMovement(event)
            tracker?.computeCurrentVelocity(1000)
            val velocity = (tracker?.yVelocity ?: 0f) / density
            tracker?.recycle()
            velocityTracker = null

            val distance = event.rawY - startY
            val isSwipe = event.actionMasked == MotionEvent.ACTION_UP &&
                (distance > dp(SWIPE_DISTANCE_DP) || velocity > SWIPE_VELOCITY_DP_PER_SECOND)
            if (isSwipe) {
                requestDismiss()
            } else {
                sheet.animate().translationY(0f).setDuration(ANIMATION_MILLIS).start()
            }
        }
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val STAGED_WINDOW_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        const val SCRIM_COLOR = 0x66000000
        const val CORNER_RADIUS_DP = 28
        const val TOP_GAP_DP = 24
        const val MIN_CONTENT_HEIGHT_DP = 120
        const val HANDLE_AREA_HEIGHT_DP = 28
        const val MIN_REVEAL_OFFSET_DP = 120
        const val SWIPE_DISTANCE_DP = 80
        const val SWIPE_VELOCITY_DP_PER_SECOND = 800f
        const val ANIMATION_MILLIS = 200L
        const val REVEAL_ANIMATION_MILLIS = 300L
    }
}
