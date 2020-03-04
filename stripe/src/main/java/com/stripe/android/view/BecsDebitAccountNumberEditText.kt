package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.util.AttributeSet

internal class BecsDebitAccountNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    init {
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        keyListener = DigitsKeyListener.getInstance(false, true)

        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                shouldShowError = false
            }
        })
    }

    private companion object {
        private const val MAX_LENGTH = 9
    }
}
