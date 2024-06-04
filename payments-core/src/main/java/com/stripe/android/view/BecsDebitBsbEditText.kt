package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import com.stripe.android.R
import androidx.appcompat.R as AppCompatR

internal class BecsDebitBsbEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    private val banks = BecsDebitBanks(context)

    var onBankChangedCallback: (BecsDebitBanks.Bank?) -> Unit = {}
    var onCompletedCallback: () -> Unit = {}

    internal val bsb: String?
        get() {
            errorMessage = when {
                fieldText.length < MIN_VALIDATION_THRESHOLD -> {
                    resources.getString(R.string.stripe_becs_widget_bsb_incomplete)
                }
                bank == null -> {
                    resources.getString(R.string.stripe_becs_widget_bsb_invalid)
                }
                fieldText.length < MAX_LENGTH -> {
                    resources.getString(R.string.stripe_becs_widget_bsb_incomplete)
                }
                else -> {
                    null
                }
            }

            return fieldText.filter { it.isDigit() }.takeIf { isComplete }
        }

    private val isComplete: Boolean
        get() {
            return bank != null && fieldText.length == MAX_LENGTH
        }

    private val bank: BecsDebitBanks.Bank?
        get() {
            return banks.byPrefix(fieldText)
        }

    init {
        filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        inputType = InputType.TYPE_CLASS_NUMBER

        addTextChangedListener(
            object : StripeTextWatcher() {
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

                    val isInvalid = bank == null && fieldText.length >= MIN_VALIDATION_THRESHOLD
                    errorMessage = if (isInvalid) {
                        resources.getString(R.string.stripe_becs_widget_bsb_invalid)
                    } else {
                        null
                    }
                    shouldShowError = errorMessage != null

                    onBankChangedCallback(bank)
                    updateIcon(isInvalid)

                    if (isComplete) {
                        onCompletedCallback()
                    }
                }
            }
        )
    }

    private fun updateIcon(isError: Boolean) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (isError) {
                R.drawable.stripe_ic_bank_error
            } else {
                R.drawable.stripe_ic_bank_becs
            },
            0,
            0,
            0
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
