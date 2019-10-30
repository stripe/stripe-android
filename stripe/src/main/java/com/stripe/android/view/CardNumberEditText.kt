package com.stripe.android.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.VisibleForTesting
import com.stripe.android.CardUtils
import com.stripe.android.R
import com.stripe.android.StripeTextUtils
import com.stripe.android.model.Card

/**
 * A [StripeEditText] that handles spacing out the digits of a credit card.
 */
class CardNumberEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    @VisibleForTesting
    @Card.CardBrand
    @get:Card.CardBrand
    var cardBrand: String = Card.CardBrand.UNKNOWN
        internal set

    private var cardBrandChangeListener: CardBrandChangeListener? = null
    private var cardNumberCompleteListener: CardNumberCompleteListener? = null

    var lengthMax: Int = MAX_LENGTH_COMMON
        private set

    private var ignoreChanges = false

    /**
     * Check whether or not the card number is valid
     */
    var isCardNumberValid: Boolean = false
        private set

    /**
     * Gets a usable form of the card number. If the text is "4242 4242 4242 4242", this
     * method will return "4242424242424242". If the card number is invalid, this returns
     * `null`.
     *
     * @return a space-free version of the card number, or `null` if the number is invalid
     */
    val cardNumber: String?
        get() = if (isCardNumberValid) {
            StripeTextUtils.removeSpacesAndHyphens(text?.toString())
        } else {
            null
        }

    init {
        listenForTextChanges()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.text = resources.getString(R.string.acc_label_card_number_node, text)
    }

    @JvmSynthetic
    internal fun setCardNumberCompleteListener(listener: CardNumberCompleteListener) {
        cardNumberCompleteListener = listener
    }

    @JvmSynthetic
    internal fun setCardBrandChangeListener(listener: CardBrandChangeListener) {
        cardBrandChangeListener = listener
        // Immediately display the brand if known, in case this method is invoked when
        // partial data already exists.
        cardBrandChangeListener?.onCardBrandChanged(cardBrand)
    }

    @JvmSynthetic
    internal fun updateLengthFilter() {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(lengthMax))
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newLength the post-edit length of the string
     * @param editActionStart the position in the string at which the edit action starts
     * @param editActionAddition the number of new characters going into the string (zero for
     * delete)
     * @return an index within the string at which to put the cursor
     */
    @JvmSynthetic
    internal fun updateSelectionIndex(
        newLength: Int,
        editActionStart: Int,
        editActionAddition: Int
    ): Int {
        var gapsJumped = 0
        val gapSet = if (Card.CardBrand.AMERICAN_EXPRESS == cardBrand) {
            SPACE_SET_AMEX
        } else {
            SPACE_SET_COMMON
        }

        var skipBack = false
        gapSet.forEach { gap ->
            if (editActionStart <= gap && editActionStart + editActionAddition > gap) {
                gapsJumped++
            }

            // editActionAddition can only be 0 if we are deleting,
            // so we need to check whether or not to skip backwards one space
            if (editActionAddition == 0 && editActionStart == gap + 1) {
                skipBack = true
            }
        }

        var newPosition: Int = editActionStart + editActionAddition + gapsJumped
        if (skipBack && newPosition > 0) {
            newPosition--
        }

        return if (newPosition <= newLength) {
            newPosition
        } else {
            newLength
        }
    }

    private fun listenForTextChanges() {
        addTextChangedListener(object : TextWatcher {
            var latestChangeStart: Int = 0
            var latestInsertionSize: Int = 0

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (!ignoreChanges) {
                    latestChangeStart = start
                    latestInsertionSize = after
                }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (ignoreChanges) {
                    return
                }

                if (start < 4) {
                    updateCardBrandFromNumber(s.toString())
                }

                if (start > 16) {
                    // no need to do formatting if we're past all of the spaces.
                    return
                }

                val spacelessNumber = StripeTextUtils.removeSpacesAndHyphens(s.toString())
                    ?: return

                val cardParts = ViewUtils.separateCardNumberGroups(
                    spacelessNumber, cardBrand)
                val formattedNumberBuilder = StringBuilder()
                for (i in cardParts.indices) {
                    if (cardParts[i] == null) {
                        break
                    }

                    if (i != 0) {
                        formattedNumberBuilder.append(' ')
                    }
                    formattedNumberBuilder.append(cardParts[i])
                }

                val formattedNumber = formattedNumberBuilder.toString()
                val cursorPosition = updateSelectionIndex(formattedNumber.length,
                    latestChangeStart, latestInsertionSize)

                ignoreChanges = true
                setText(formattedNumber)
                setSelection(cursorPosition)
                ignoreChanges = false
            }

            override fun afterTextChanged(s: Editable) {
                if (s.length == lengthMax) {
                    val before = isCardNumberValid
                    isCardNumberValid = CardUtils.isValidCardNumber(s.toString())
                    shouldShowError = !isCardNumberValid
                    if (!before && isCardNumberValid) {
                        cardNumberCompleteListener?.onCardNumberComplete()
                    }
                } else {
                    isCardNumberValid = CardUtils.isValidCardNumber(text?.toString())
                    // Don't show errors if we aren't full-length.
                    shouldShowError = false
                }
            }
        })
    }

    private fun updateCardBrand(@Card.CardBrand brand: String) {
        if (cardBrand == brand) {
            return
        }

        cardBrand = brand

        cardBrandChangeListener?.onCardBrandChanged(cardBrand)

        val oldLength = lengthMax
        lengthMax = getLengthForBrand(cardBrand)
        if (oldLength == lengthMax) {
            return
        }

        updateLengthFilter()
    }

    private fun updateCardBrandFromNumber(partialNumber: String) {
        updateCardBrand(CardUtils.getPossibleCardType(partialNumber))
    }

    internal interface CardNumberCompleteListener {
        fun onCardNumberComplete()
    }

    internal interface CardBrandChangeListener {
        fun onCardBrandChanged(@Card.CardBrand brand: String)
    }

    companion object {
        private const val MAX_LENGTH_COMMON = 19
        // Note that AmEx and Diners Club have the same length
        // because Diners Club has one more space, but one less digit.
        private const val MAX_LENGTH_AMEX_DINERS = 17

        private val SPACE_SET_COMMON = setOf(4, 9, 14)
        private val SPACE_SET_AMEX = setOf(4, 11)

        private fun getLengthForBrand(@Card.CardBrand cardBrand: String): Int {
            return when (cardBrand) {
                Card.CardBrand.AMERICAN_EXPRESS, Card.CardBrand.DINERS_CLUB ->
                    MAX_LENGTH_AMEX_DINERS
                else -> MAX_LENGTH_COMMON
            }
        }
    }
}
