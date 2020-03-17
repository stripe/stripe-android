package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import com.stripe.android.R

internal class BecsDebitAccountNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    val accountNumber: String?
        get() {
            errorMessage = when {
                fieldText.isBlank() -> {
                    resources.getString(R.string.becs_widget_account_number_required)
                }
                fieldText.length < minLength -> {
                    resources.getString(R.string.becs_widget_account_number_incomplete)
                }
                else -> {
                    null
                }
            }
            return fieldText.takeIf { errorMessage == null }
        }

    var minLength: Int = DEFAULT_MIN_LENGTH

    init {
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        keyListener = DigitsKeyListener.getInstance(false, true)

        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                shouldShowError = false
            }
        })
    }

    internal companion object {
        internal const val DEFAULT_MIN_LENGTH = 5
        private const val MAX_LENGTH = 9
    }
}
