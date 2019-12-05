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
        configureForUs()

        maxLines = 1
        inputType = InputType.TYPE_CLASS_NUMBER
        keyListener = DigitsKeyListener.getInstance(false, true)
    }

    /**
     * Configure the field for United States users
     */
    @JvmSynthetic
    internal fun configureForUs() {
        setHint(R.string.address_label_zip_code)
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_US))
    }

    /**
     * Configure the field for global users
     */
    @JvmSynthetic
    internal fun configureForGlobal() {
        setHint(R.string.address_label_postal_code)
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_GLOBAL))
    }

    private companion object {
        private const val MAX_LENGTH_US = 5
        private const val MAX_LENGTH_GLOBAL = 13
    }
}
