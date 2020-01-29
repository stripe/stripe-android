package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.model.CardBrand

/**
 * A [StripeEditText] for CVC input.
 */
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

    internal val rawCvcValue: String
        @JvmSynthetic
        get() {
            return fieldText.trim()
        }

    private var cardBrand: CardBrand = CardBrand.Unknown

    private val isValid: Boolean
        get() {
            return cardBrand.isValidCvc(rawCvcValue)
        }

    // invoked when a valid CVC has been entered
    @JvmSynthetic
    internal var completionCallback: () -> Unit = {}

    init {
        setErrorMessage(resources.getString(R.string.invalid_cvc))
        setHint(R.string.cvc_number_hint)
        maxLines = 1
        filters = createFilters(CardBrand.Unknown)

        inputType = InputType.TYPE_NUMBER_VARIATION_PASSWORD
        keyListener = DigitsKeyListener.getInstance(false, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        }

        addTextChangedListener(object : StripeTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                shouldShowError = false
                if (cardBrand.isMaxCvc(rawCvcValue)) {
                    completionCallback()
                }
            }
        })
    }

    override val accessibilityText: String?
        get() {
            return resources.getString(R.string.acc_label_cvc_node, text)
        }

    /**
     * @param cardBrand the [CardBrand] used to update the view
     * @param customHintText optional user-specified hint text
     * @param textInputLayout if specified, hint text will be set on this [TextInputLayout]
     * instead of directly on the [CvcEditText]
     */
    @JvmSynthetic
    internal fun updateBrand(
        cardBrand: CardBrand,
        customHintText: String? = null,
        textInputLayout: TextInputLayout? = null
    ) {
        this.cardBrand = cardBrand
        filters = createFilters(cardBrand)

        val hintText = customHintText
            ?: if (cardBrand == CardBrand.AmericanExpress) {
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

    private fun createFilters(cardBrand: CardBrand): Array<InputFilter> {
        return arrayOf(InputFilter.LengthFilter(cardBrand.maxCvcLength))
    }
}
