package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import com.stripe.android.cards.Cvc
import com.stripe.android.model.CardBrand
import androidx.appcompat.R as AppCompatR

/**
 * A [StripeEditText] for CVC input.
 */
class CvcEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    private val unvalidatedCvc: Cvc.Unvalidated
        get() {
            return Cvc.Unvalidated(fieldText)
        }

    internal val cvc: Cvc.Validated?
        get() {
            return unvalidatedCvc.validate(cardBrand.maxCvcLength)
        }

    private var cardBrand: CardBrand = CardBrand.Unknown

    // invoked when a valid CVC has been entered
    @JvmSynthetic
    internal var completionCallback: () -> Unit = {}

    init {
        setErrorMessage(resources.getString(R.string.stripe_invalid_cvc))
        setHint(R.string.stripe_cvc_number_hint)
        maxLines = 1
        filters = createFilters(CardBrand.Unknown)

        setNumberOnlyInputType()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        }

        doAfterTextChanged {
            shouldShowError = false
            if (cardBrand.isMaxCvc(unvalidatedCvc.normalized)) {
                completionCallback()
            }
        }

        internalFocusChangeListeners.add { _, hasFocus ->
            if (!hasFocus && unvalidatedCvc.isPartialEntry(cardBrand.maxCvcLength)) {
                // TODO (michelleb-stripe) Should set error message to incomplete
                shouldShowError = true
            }
        }

        layoutDirection = LAYOUT_DIRECTION_LTR
    }

    override val accessibilityText: String
        get() {
            return resources.getString(R.string.stripe_acc_label_cvc_node, text)
        }

    /**
     * @param cardBrand the [CardBrand] used to update the view
     * @param customHintText optional user-specified hint text
     * @param customPlaceholderText optional user-specified placeholder text
     * @param textInputLayout if specified, hint text will be set on this [TextInputLayout]
     * instead of directly on the [CvcEditText]
     */
    @JvmSynthetic
    internal fun updateBrand(
        cardBrand: CardBrand,
        customHintText: String? = null,
        customPlaceholderText: String? = null,
        textInputLayout: TextInputLayout? = null
    ) {
        this.cardBrand = cardBrand
        filters = createFilters(cardBrand)

        val hintText = customHintText
            ?: if (cardBrand == CardBrand.AmericanExpress) {
                resources.getString(R.string.stripe_cvc_amex_hint)
            } else {
                resources.getString(R.string.stripe_cvc_number_hint)
            }

        // Only show an error when we update the branch if text is entered
        // and the Cvc does not validate
        if (unvalidatedCvc.normalized.isNotEmpty()) {
            shouldShowError = unvalidatedCvc.validate(cardBrand.maxCvcLength) == null
            // TODO(michelleb-stripe): Should truncate CVC on a brand name change.
        }

        if (textInputLayout != null) {
            textInputLayout.hint = hintText

            textInputLayout.placeholderText = customPlaceholderText
                ?: resources.getString(
                    when (cardBrand) {
                        CardBrand.AmericanExpress -> R.string.stripe_cvc_multiline_helper_amex
                        else -> R.string.stripe_cvc_multiline_helper
                    }
                )
        } else {
            this.hint = hintText
        }
    }

    private fun createFilters(cardBrand: CardBrand): Array<InputFilter> {
        return arrayOf(InputFilter.LengthFilter(cardBrand.maxCvcLength))
    }
}
