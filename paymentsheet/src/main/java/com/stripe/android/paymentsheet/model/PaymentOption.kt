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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The customer's selected payment option.
 */
data class PaymentOption
@Deprecated("Not intended for public use.")
constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @Deprecated("Please use icon() instead.")
    @DrawableRes
    val drawableResourceId: Int,

    /**
     * A label that describes the payment option.
     *
     * For example, "····4242" for a Visa ending in 4242.
     */
    val label: String
) {
    // These aren't part of the primary constructor in order to maintain binary compatibility.
    internal var lightThemeIconUrl: String? = null
        private set

    internal var darkThemeIconUrl: String? = null
        private set

    private var imageLoader: suspend (PaymentOption) -> Drawable = {
        throw IllegalStateException("Must pass in an image loader to use iconDrawable.")
    }

    @Suppress("DEPRECATION")
    internal constructor(
        @DrawableRes drawableResourceId: Int,
        label: String,
        lightThemeIconUrl: String?,
        darkThemeIconUrl: String?,
        imageLoader: suspend (PaymentOption) -> Drawable
    ) : this(drawableResourceId, label) {
        this.lightThemeIconUrl = lightThemeIconUrl
        this.darkThemeIconUrl = darkThemeIconUrl
        this.imageLoader = imageLoader
    }

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    fun icon(): Drawable {
        return DelegateDrawable(ShapeDrawable(), imageLoader, this)
    }
}

private class DelegateDrawable(
    @Volatile private var delegate: Drawable,
    private val imageLoader: suspend (PaymentOption) -> Drawable,
    private val paymentOption: PaymentOption,
) : Drawable() {
    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            delegate = imageLoader(paymentOption)
            withContext(Dispatchers.Main) {
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
        return delegate.getAlpha()
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
        return delegate.getColorFilter()
    }

    override fun clearColorFilter() {
        delegate.clearColorFilter()
    }

    override fun setState(stateSet: IntArray): Boolean {
        return delegate.setState(stateSet)
    }

    override fun getState(): IntArray {
        return delegate.getState()
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
        return delegate.getTransparentRegion()
    }

    override fun onBoundsChange(bounds: Rect) {
        delegate.bounds = bounds
    }

    override fun getMinimumWidth(): Int {
        return delegate.getMinimumWidth()
    }

    override fun getMinimumHeight(): Int {
        return delegate.getMinimumHeight()
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
