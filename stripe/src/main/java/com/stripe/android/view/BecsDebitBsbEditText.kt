package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import com.stripe.android.R

internal class BecsDebitBsbEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    private val banks = BecsDebitBanks(context)

    var onBankChangedCallback: (BecsDebitBanks.Bank?) -> Unit = {}
    var onInputErrorCallback: (Boolean) -> Unit = {}
    var onCompletedCallback: () -> Unit = {}

    init {
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        keyListener = DigitsKeyListener.getInstance(false, true)

        addTextChangedListener(object : StripeTextWatcher() {
            private var ignoreChanges = false
            private var newCursorPosition: Int? = null
            private var formattedBsb: String? = null

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreChanges) {
                    return
                }

                // skip formatting if past the separator
                if (start > 4) {
                    return
                }

                val bsb = s?.toString().orEmpty().filter { it.isDigit() }
                formattedBsb = formatBsb(bsb)
                newCursorPosition = formattedBsb?.length
            }

            override fun afterTextChanged(s: Editable?) {
                super.afterTextChanged(s)

                if (ignoreChanges) {
                    return
                }

                ignoreChanges = true
                if (!isLastKeyDelete && formattedBsb != null) {
                    setText(formattedBsb)
                    newCursorPosition?.let {
                        setSelection(it.coerceIn(0, fieldText.length))
                    }
                }
                formattedBsb = null
                newCursorPosition = null
                ignoreChanges = false

                val bank = banks.byPrefix(fieldText)
                val isError = bank == null && fieldText.length >= MIN_VALIDATION_THRESHOLD
                onBankChangedCallback(bank)
                onInputErrorCallback(isError)
                updateIcon(isError)

                if (bank != null && fieldText.length == MAX_LENGTH) {
                    onCompletedCallback()
                }
            }
        })
    }

    private fun updateIcon(isError: Boolean) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isError) {
                R.drawable.stripe_ic_bank_error
            } else {
                R.drawable.stripe_ic_bank
            },
            0, 0, 0
        )
    }

    /**
     * Correctly formats a BSB.
     *
     * For example, `"032003"` will be formatted as `"032-003"`.
     */
    private fun formatBsb(bsb: String): String {
        return if (bsb.length >= 3) {
            listOf(
                bsb.take(3),
                bsb.takeLast(bsb.length - 3)
            ).joinToString(separator = SEPARATOR)
        } else {
            bsb
        }
    }

    private companion object {
        private const val MAX_LENGTH = 7
        private const val MIN_VALIDATION_THRESHOLD = 2
        private const val SEPARATOR = "-"
    }
}
