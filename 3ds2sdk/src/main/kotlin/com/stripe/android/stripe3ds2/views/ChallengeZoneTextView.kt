package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.stripe3ds2.databinding.StripeChallengeZoneTextViewBinding
import com.stripe.android.stripe3ds2.init.ui.TextBoxCustomization

internal class ChallengeZoneTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), FormField {

    internal val infoLabel: TextInputLayout
    internal val textEntryView: TextInputEditText

    override val userEntry: String
        get() = textEntryView.text?.toString().orEmpty()

    init {
        val viewBinding = StripeChallengeZoneTextViewBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        infoLabel = viewBinding.label
        textEntryView = viewBinding.textEntry
    }

    fun setTextEntryLabel(label: String?) {
        infoLabel.hint = label
    }

    fun setText(text: String) {
        textEntryView.setText(text)
    }

    fun setTextBoxCustomization(textBoxCustomization: TextBoxCustomization?) {
        if (textBoxCustomization == null) {
            return
        }

        textBoxCustomization.textColor?.let { textColor ->
            textEntryView.setTextColor(Color.parseColor(textColor))
        }

        textBoxCustomization.textFontSize.takeIf { it > 0 }?.let { textFontSize ->
            textEntryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textFontSize.toFloat())
        }

        if (textBoxCustomization.cornerRadius >= 0) {
            val radius = textBoxCustomization.cornerRadius.toFloat()
            infoLabel.setBoxCornerRadii(radius, radius, radius, radius)
        }

        textBoxCustomization.borderColor?.let { borderColor ->
            infoLabel.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            infoLabel.boxStrokeColor = Color.parseColor(borderColor)
        }

        textBoxCustomization.hintTextColor?.let { hintTextColor ->
            infoLabel.defaultHintTextColor = ColorStateList.valueOf(Color.parseColor(hintTextColor))
        }
    }
}
