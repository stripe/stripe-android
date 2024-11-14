package com.stripe.android.paymentsheet.model

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Insets
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.uicore.image.rememberDrawablePainter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The customer's selected payment option.
 */
@OptIn(DelicateCoroutinesApi::class)
data class PaymentOption internal constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @Deprecated("Please use icon() instead.")
    val drawableResourceId: Int,
    /**
     * A label that describes the payment option.
     *
     * For example, "···· 4242" for a Visa ending in 4242.
     */
    val label: String,
    internal val lightThemeIconUrl: String?,
    internal val darkThemeIconUrl: String?,
    internal val imageLoader: suspend (PaymentOption) -> Drawable,
    private val delegateDrawableScope: CoroutineScope = GlobalScope,
    private val delegateDrawableDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    @Deprecated("Not intended for public use.")
    constructor(
        @DrawableRes
        drawableResourceId: Int,
        label: String
    ) : this(
        drawableResourceId = drawableResourceId,
        label = label,
        lightThemeIconUrl = null,
        darkThemeIconUrl = null,
        imageLoader = errorImageLoader,
    )

    /**
     * A [Painter] to draw the icon associated with this [PaymentOption].
     */
    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(icon())

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    fun icon(): Drawable {
        return DelegateDrawable(
            ShapeDrawable(),
            imageLoader,
            this,
            delegateDrawableScope,
            delegateDrawableDispatcher
        )
    }
}

private val errorImageLoader: suspend (PaymentOption) -> Drawable = {
    throw IllegalStateException("Must pass in an image loader to use icon() or iconPainter.")
}

private class DelegateDrawable(
    @Volatile private var delegate: Drawable,
    private val imageLoader: suspend (PaymentOption) -> Drawable,
    private val paymentOption: PaymentOption,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : Drawable() {
    init {
        scope.launch {
            delegate = imageLoader(paymentOption)
            withContext(dispatcher) {
                super.setBounds(0, 0, delegate.intrinsicWidth, delegate.intrinsicHeight)
                invalidateSelf()
            }
        }
    }

    override fun getIntrinsicHeight(): Int {
        return delegate.intrinsicHeight
    }

    override fun getIntrinsicWidth(): Int {
        return delegate.intrinsicWidth
    }

    @Deprecated("Deprecated in Java")
    override fun setColorFilter(color: Int, mode: PorterDuff.Mode) {
        @Suppress("DEPRECATION")
        delegate.setColorFilter(color, mode)
    }

    @Deprecated("Deprecated in Java")
    override fun setDither(dither: Boolean) {
        @Suppress("DEPRECATION")
        delegate.setDither(dither)
    }

    override fun setFilterBitmap(filter: Boolean) {
        delegate.isFilterBitmap = filter
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun isFilterBitmap(): Boolean {
        return delegate.isFilterBitmap
    }

    override fun getAlpha(): Int {
        return delegate.alpha
    }

    override fun setTint(tintColor: Int) {
        delegate.setTint(tintColor)
    }

    override fun setTintList(tint: ColorStateList?) {
        delegate.setTintList(tint)
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        delegate.setTintMode(tintMode)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun setTintBlendMode(blendMode: BlendMode?) {
        delegate.setTintBlendMode(blendMode)
    }

    override fun getColorFilter(): ColorFilter? {
        return delegate.colorFilter
    }

    override fun clearColorFilter() {
        delegate.clearColorFilter()
    }

    override fun setState(stateSet: IntArray): Boolean {
        return delegate.setState(stateSet)
    }

    override fun getState(): IntArray {
        return delegate.state
    }

    override fun jumpToCurrentState() {
        delegate.jumpToCurrentState()
    }

    override fun getCurrent(): Drawable {
        return delegate
    }

    override fun applyTheme(t: Resources.Theme) {
        delegate.applyTheme(t)
    }

    override fun canApplyTheme(): Boolean {
        return delegate.canApplyTheme()
    }

    override fun getTransparentRegion(): Region? {
        return delegate.transparentRegion
    }

    override fun onBoundsChange(bounds: Rect) {
        delegate.bounds = bounds
    }

    override fun getMinimumWidth(): Int {
        return delegate.minimumWidth
    }

    override fun getMinimumHeight(): Int {
        return delegate.minimumHeight
    }

    override fun getPadding(padding: Rect): Boolean {
        return delegate.getPadding(padding)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun getOpticalInsets(): Insets {
        return delegate.opticalInsets
    }

    override fun getOutline(outline: Outline) {
        delegate.getOutline(outline)
    }

    override fun mutate(): Drawable {
        return this
    }

    override fun draw(canvas: Canvas) {
        delegate.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        delegate.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        delegate.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        @Suppress("DEPRECATION")
        return delegate.opacity
    }
}
