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
import android.util.Log
import androidx.annotation.DrawableRes
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
    @Deprecated("Please use fetchIcon instead.")
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
            Log.d("Jay", "Width: ${delegate.intrinsicWidth} - Height: ${delegate.intrinsicHeight}")
            withContext(Dispatchers.Main) {
                super.setBounds(0, 0, delegate.intrinsicWidth, delegate.intrinsicHeight)
                invalidateSelf()
            }
        }
    }

    override fun getIntrinsicHeight(): Int = delegate.intrinsicHeight

    override fun getIntrinsicWidth(): Int = delegate.intrinsicWidth

    override fun setColorFilter(color: Int, mode: PorterDuff.Mode) {
        delegate.setColorFilter(color, mode)
    }

//    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
//        delegate.setBounds(left, top, right, bottom)
//    }
//
//    override fun setBounds(bounds: Rect) {
//        delegate.setBounds(bounds)
//    }
//
    override fun getDirtyBounds(): Rect {
        return bounds
    }

//    override fun setChangingConfigurations(configs: Int) {
//        delegate.setChangingConfigurations(configs)
//    }
//
//    override fun getChangingConfigurations(): Int {
//        return delegate.getChangingConfigurations()
//    }

    override fun setDither(dither: Boolean) {
        delegate.setDither(dither)
    }

    override fun setFilterBitmap(filter: Boolean) {
        delegate.setFilterBitmap(filter)
    }

    override fun isFilterBitmap(): Boolean {
        return delegate.isFilterBitmap()
    }

//    override fun getCallback(): Callback? {
//        return delegate.getCallback()
//    }

//    override fun invalidateSelf() {
//        delegate.invalidateSelf()
//    }

//    override fun scheduleSelf(what: Runnable, `when`: Long) {
//        delegate.scheduleSelf(what, `when`)
//    }
//
//    override fun unscheduleSelf(what: Runnable) {
//        delegate.unscheduleSelf(what)
//    }
//
//    override fun getLayoutDirection(): Int {
//        return delegate.getLayoutDirection()
//    }
//
//    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
//        return delegate.onLayoutDirectionChanged(layoutDirection)
//    }

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

    override fun setTintBlendMode(blendMode: BlendMode?) {
        delegate.setTintBlendMode(blendMode)
    }

    override fun getColorFilter(): ColorFilter? {
        return delegate.getColorFilter()
    }

    override fun clearColorFilter() {
        delegate.clearColorFilter()
    }

//    override fun setHotspot(x: Float, y: Float) {
//        delegate.setHotspot(x, y)
//    }
//
//    override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
//        delegate.setHotspotBounds(left, top, right, bottom)
//    }
//
//    override fun getHotspotBounds(outRect: Rect) {
//        delegate.getHotspotBounds(outRect)
//    }
//
//    override fun isProjected(): Boolean {
//        return delegate.isProjected()
//    }
//
//    override fun isStateful(): Boolean {
//        return delegate.isStateful()
//    }
//
//    override fun hasFocusStateSpecified(): Boolean {
//        return delegate.hasFocusStateSpecified()
//    }

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

//    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
//        return delegate.setVisible(visible, restart)
//    }
//
//    override fun setAutoMirrored(mirrored: Boolean) {
//        delegate.setAutoMirrored(mirrored)
//    }
//
//    override fun isAutoMirrored(): Boolean {
//        return delegate.isAutoMirrored()
//    }

    override fun applyTheme(t: Resources.Theme) {
        delegate.applyTheme(t)
    }

    override fun canApplyTheme(): Boolean {
        return delegate.canApplyTheme()
    }

    override fun getTransparentRegion(): Region? {
        return delegate.getTransparentRegion()
    }

//    override fun onStateChange(state: IntArray): Boolean {
//        return delegate.onStateChange(state)
//    }
//
//    override fun onLevelChange(level: Int): Boolean {
//        return delegate.onLevelChange(level)
//    }
//
//    override fun onBoundsChange(bounds: Rect) {
//        delegate.onBoundsChange(bounds)
//    }

    override fun getMinimumWidth(): Int {
        return delegate.getMinimumWidth()
    }

    override fun getMinimumHeight(): Int {
        return delegate.getMinimumHeight()
    }

    override fun getPadding(padding: Rect): Boolean {
        return delegate.getPadding(padding)
    }

    override fun getOpticalInsets(): Insets {
        return delegate.getOpticalInsets()
    }

    override fun getOutline(outline: Outline) {
        delegate.getOutline(outline)
    }

    override fun mutate(): Drawable {
        return delegate.mutate()
    }

//    override fun inflate(r: Resources, parser: XmlPullParser, attrs: AttributeSet) {
//        delegate.inflate(r, parser, attrs)
//    }
//
//    override fun inflate(
//        r: Resources,
//        parser: XmlPullParser,
//        attrs: AttributeSet,
//        theme: Resources.Theme?
//    ) {
//        delegate.inflate(r, parser, attrs, theme)
//    }
//
//    override fun getConstantState(): ConstantState? {
//        return delegate.getConstantState()
//    }

    override fun draw(canvas: Canvas) {
        delegate.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        delegate.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        delegate.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = delegate.opacity
}
