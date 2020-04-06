package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import com.stripe.android.databinding.CardBrandViewBinding
import com.stripe.android.model.CardBrand

internal class CardBrandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: CardBrandViewBinding = CardBrandViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val iconView = viewBinding.icon

    @ColorInt
    internal var tintColorInt: Int = 0

    init {
        isClickable = false
        isFocusable = false
    }

    internal fun showBrandIcon(brand: CardBrand, shouldShowErrorIcon: Boolean) {
        if (shouldShowErrorIcon) {
            iconView.setImageResource(brand.errorIcon)
        } else {
            iconView.setImageResource(brand.icon)

            if (brand == CardBrand.Unknown) {
                applyTint()
            }
        }
    }

    internal fun showCvcIcon(brand: CardBrand) {
        iconView.setImageResource(brand.cvcIcon)
        applyTint()
    }

    internal fun applyTint() {
        val icon = iconView.drawable
        val compatIcon = DrawableCompat.wrap(icon)
        DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
        iconView.setImageDrawable(DrawableCompat.unwrap(compatIcon))
    }
}
