package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import com.google.android.material.button.MaterialButton
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization

/**
 * A button that can be customized per the 3DS2 spec
 */
internal open class ThreeDS2Button @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    /**
     * Customize this button with the given button customization
     *
     * @param buttonCustomization customization to apply to this button
     */
    fun setButtonCustomization(buttonCustomization: ButtonCustomization?) {
        if (buttonCustomization == null) {
            return
        }

        buttonCustomization.textColor?.let {
            setTextColor(parseColor(it))
        }

        buttonCustomization.backgroundColor?.let {
            backgroundTintList = ColorStateList.valueOf(parseColor(it))
        }

        buttonCustomization.cornerRadius.takeIf { it >= 0 }?.let {
            cornerRadius = it
        }

        buttonCustomization.textFontSize.takeIf { it > 0 }?.let { textFontSize ->
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textFontSize.toFloat())
        }

        buttonCustomization.textFontName?.let {
            typeface = Typeface.create(it, Typeface.NORMAL)
        }
    }

    @VisibleForTesting
    @ColorInt
    internal fun parseColor(hexColor: String): Int {
        return Color.parseColor(hexColor)
    }
}
