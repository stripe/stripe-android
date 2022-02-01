package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.textview.MaterialTextView
import com.stripe.android.stripe3ds2.init.ui.LabelCustomization

internal open class ThreeDS2TextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {

    open fun setText(text: String?, labelCustomization: LabelCustomization?) {
        setText(text)

        labelCustomization?.textColor?.let { textColor ->
            setTextColor(Color.parseColor(textColor))
        }

        labelCustomization?.textFontSize?.takeIf { it > 0 }?.let { textFontSize ->
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textFontSize.toFloat())
        }

        labelCustomization?.textFontName?.let { textFontName ->
            typeface = Typeface.create(textFontName, Typeface.NORMAL)
        }
    }
}
