package com.stripe.android.view

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import androidx.core.widget.doAfterTextChanged
import com.stripe.android.R
import androidx.appcompat.R as AppCompatR

internal class BecsDebitAccountNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    val accountNumber: String?
        get() {
            errorMessage = when {
                fieldText.isBlank() -> {
                    resources.getString(R.string.stripe_becs_widget_account_number_required)
                }
                fieldText.length < minLength -> {
                    resources.getString(R.string.stripe_becs_widget_account_number_incomplete)
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
        inputType = InputType.TYPE_CLASS_NUMBER

        doAfterTextChanged {
            shouldShowError = false
        }
    }

    internal companion object {
        internal const val DEFAULT_MIN_LENGTH = 5
        private const val MAX_LENGTH = 9
    }
}
