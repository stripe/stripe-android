package com.stripe.android.view

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import com.stripe.android.R

class PostalCodeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    init {
        setHint(R.string.acc_label_zip)
        maxLines = 1
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))

        inputType = InputType.TYPE_CLASS_NUMBER
        keyListener = DigitsKeyListener.getInstance(false, true)
    }

    private companion object {
        private const val MAX_LENGTH = 5
    }
}
