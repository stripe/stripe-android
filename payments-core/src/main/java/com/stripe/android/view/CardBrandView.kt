package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.doOnNextLayout
import com.stripe.android.databinding.CardBrandViewBinding
import com.stripe.android.model.CardBrand
import kotlin.properties.Delegates

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
    private val progressView = viewBinding.progress

    @ColorInt
    internal var tintColorInt: Int = 0

    var isLoading: Boolean by Delegates.observable(
        false
    ) { _, wasLoading, isLoading ->
        if (wasLoading != isLoading) {
            updateIcon()

            if (isLoading) {
                progressView.show()
            } else {
                progressView.hide()
            }
        }
    }

    var brand: CardBrand by Delegates.observable(
        CardBrand.Unknown
    ) { _, prevValue, newValue ->
        if (prevValue != newValue) {
            updateIcon()
        }
    }

    var shouldShowCvc: Boolean by Delegates.observable(
        false
    ) { _, prevValue, newValue ->
        if (prevValue != newValue) {
            updateIcon()
        }
    }

    var shouldShowErrorIcon: Boolean by Delegates.observable(
        false
    ) { _, prevValue, newValue ->
        if (prevValue != newValue) {
            updateIcon()
        }
    }

    init {
        isClickable = false
        isFocusable = false

        doOnNextLayout {
            updateIcon()
        }
    }

    private fun updateIcon() {
        when {
            isLoading -> {
                renderBrandIcon()
            }
            shouldShowErrorIcon -> {
                iconView.setImageResource(brand.errorIcon)
            }
            shouldShowCvc -> {
                iconView.setImageResource(brand.cvcIcon)
                applyTint()
            }
            else -> {
                renderBrandIcon()
            }
        }
    }

    private fun renderBrandIcon() {
        iconView.setImageResource(brand.icon)

        if (brand == CardBrand.Unknown) {
            applyTint()
        }
    }

    private fun applyTint() {
        iconView.setImageDrawable(
            DrawableCompat.unwrap(
                DrawableCompat.wrap(iconView.drawable).also { compatIcon ->
                    DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
                }
            )
        )
    }
}
