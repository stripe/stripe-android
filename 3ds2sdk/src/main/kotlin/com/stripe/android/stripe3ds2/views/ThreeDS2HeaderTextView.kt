package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization

/**
 * A text view which uses Header values from the given Label Customization.
 */
internal class ThreeDS2HeaderTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ThreeDS2TextView(context, attrs, defStyleAttr) {
    override fun setText(text: String?, labelCustomization: LabelCustomization?) {
        setText(text)
        if (labelCustomization != null) {
            labelCustomization.headingTextColor?.let { headingTextColor ->
                setTextColor(Color.parseColor(headingTextColor))
            }

            labelCustomization.headingTextFontSize.takeIf { it > 0 }?.let { headingTextFontSize ->
                setTextSize(TypedValue.COMPLEX_UNIT_SP, headingTextFontSize.toFloat())
            }

            labelCustomization.headingTextFontName?.let { headingTextFontName ->
                typeface = Typeface.create(headingTextFontName, Typeface.NORMAL)
            }
        }
    }
}
