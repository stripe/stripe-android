package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.model.Card

class CvcEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    /**
     * @return the inputted CVC value if valid; otherwise, null
     */
    val cvcValue: String?
        get() {
            return rawCvcValue.takeIf { isValid }
        }

    private val rawCvcValue: String
        get() {
            return text.toString().trim()
        }

    private var isAmex: Boolean = false

    private val isValid: Boolean
        get() {
            val cvcLength = rawCvcValue.length
            return if (isAmex && cvcLength == Card.CVC_LENGTH_AMERICAN_EXPRESS) {
                true
            } else {
                cvcLength == Card.CVC_LENGTH_COMMON
            }
        }

    init {
        setHint(R.string.cvc_number_hint)
        maxLines = 1
        filters = INPUT_FILTER_AMEX

        inputType = InputType.TYPE_NUMBER_VARIATION_PASSWORD
        keyListener = DigitsKeyListener.getInstance(false, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        }
    }

    /**
     * @param brand - the [Card.CardBrand] used to update the view
     * @param customHintText - optional user-specified hint text
     * @param textInputLayout - if specified, hint text will be set on this [TextInputLayout]
     * instead of directly on the [CvcEditText]
     */
    @JvmSynthetic
    internal fun updateBrand(
        @Card.CardBrand brand: String,
        customHintText: String? = null,
        textInputLayout: TextInputLayout? = null
    ) {
        isAmex = Card.CardBrand.AMERICAN_EXPRESS == brand
        filters = if (isAmex) {
            INPUT_FILTER_AMEX
        } else {
            INPUT_FILTER_COMMON
        }

        val hintText = customHintText
            ?: if (isAmex) {
                resources.getString(R.string.cvc_amex_hint)
            } else {
                resources.getString(R.string.cvc_number_hint)
            }

        if (textInputLayout != null) {
            textInputLayout.hint = hintText
        } else {
            this.hint = hintText
        }
    }

    private companion object {
        private val INPUT_FILTER_AMEX: Array<InputFilter> =
            arrayOf(InputFilter.LengthFilter(Card.CVC_LENGTH_AMERICAN_EXPRESS))

        private val INPUT_FILTER_COMMON: Array<InputFilter> =
            arrayOf(InputFilter.LengthFilter(Card.CVC_LENGTH_COMMON))
    }
}
