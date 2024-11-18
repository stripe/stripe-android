package com.stripe.android.common.ui

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
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DelegateDrawable(
    private val imageLoader: suspend () -> Drawable,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : Drawable() {
    @Volatile
    private var delegate: Drawable = ShapeDrawable()

    init {
        scope.launch {
            delegate = imageLoader()
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
