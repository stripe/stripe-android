package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import com.stripe.android.CardUtils
import com.stripe.android.R
import com.stripe.android.StripeTextUtils
import com.stripe.android.cards.Bin
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.LegacyCardAccountRangeRepository
import com.stripe.android.cards.LocalCardAccountRangeSource
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [StripeEditText] that handles spacing out the digits of a credit card.
 */
class CardNumberEditText internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle,

    // TODO(mshafrir-stripe): make immutable after `CardWidgetViewModel` is integrated in `CardWidget` subclasses
    internal var workDispatcher: CoroutineDispatcher,

    private val cardAccountRangeRepository: CardAccountRangeRepository
) : StripeEditText(context, attrs, defStyleAttr) {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
    ) : this(
        context,
        attrs,
        defStyleAttr,
        Dispatchers.IO,
        LegacyCardAccountRangeRepository(LocalCardAccountRangeSource())
    )

    @VisibleForTesting
    var cardBrand: CardBrand = CardBrand.Unknown
        internal set(value) {
            val prevBrand = field
            field = value
            if (value != prevBrand) {
                brandChangeCallback(cardBrand)
                updateLengthFilter()
            }
        }

    @JvmSynthetic
    internal var brandChangeCallback: (CardBrand) -> Unit = {}
        set(callback) {
            field = callback

            // Immediately display the brand if known, in case this method is invoked when
            // partial data already exists.
            callback(cardBrand)
        }

    // invoked when a valid card has been entered
    @JvmSynthetic
    internal var completionCallback: () -> Unit = {}

    @Deprecated("Will be removed in upcoming major release.")
    val lengthMax: Int
        get() {
            return cardBrand.getMaxLengthWithSpacesForCardNumber(fieldText)
        }

    private var panLength = CardNumber.DEFAULT_PAN_LENGTH

    private val formattedPanLength: Int
        get() = panLength + CardNumber.getSpacePositions(panLength).size

    private var ignoreChanges = false

    /**
     * Check whether or not the card number is valid
     */
    var isCardNumberValid: Boolean = false
        private set

    /**
     * A normalized form of the card number. If the entered card number is "4242 4242 4242 4242",
     * this will be "4242424242424242". If the entered card number is invalid, this is `null`.
     */
    val cardNumber: String?
        get() = if (isCardNumberValid) {
            StripeTextUtils.removeSpacesAndHyphens(fieldText)
        } else {
            null
        }

    @VisibleForTesting
    internal var accountRangeRepositoryJob: Job? = null

    @JvmSynthetic
    internal var isProcessingCallback: (Boolean) -> Unit = {}

    init {
        setErrorMessage(resources.getString(R.string.invalid_card_number))
        listenForTextChanges()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
        }

        updateLengthFilter()
    }

    override val accessibilityText: String?
        get() {
            return resources.getString(R.string.acc_label_card_number_node, text)
        }

    override fun onDetachedFromWindow() {
        cancelAccountRangeRepositoryJob()

        super.onDetachedFromWindow()
    }

    @JvmSynthetic
    internal fun updateLengthFilter() {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(formattedPanLength))
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
        val gapSet = cardBrand.getSpacePositionsForCardNumber(fieldText)

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
        addTextChangedListener(object : StripeTextWatcher() {
            private var latestChangeStart: Int = 0
            private var latestInsertionSize: Int = 0

            private var newCursorPosition: Int? = null
            private var formattedNumber: String? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!ignoreChanges) {
                    latestChangeStart = start
                    latestInsertionSize = after
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // skip formatting if we're past the last possible space position
                if (ignoreChanges || start > 16) {
                    return
                }

                val spacelessNumber = StripeTextUtils.removeSpacesAndHyphens(
                    s?.toString().orEmpty()
                ).orEmpty()

                val cardNumber = CardNumber.Unvalidated(spacelessNumber)
                updateCardBrand(cardNumber.bin)

                val formattedNumber = cardNumber.getFormatted(panLength)
                this.newCursorPosition = updateSelectionIndex(
                    formattedNumber.length,
                    latestChangeStart,
                    latestInsertionSize
                )
                this.formattedNumber = formattedNumber
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreChanges) {
                    return
                }

                ignoreChanges = true
                if (!isLastKeyDelete && formattedNumber != null) {
                    setText(formattedNumber)
                    newCursorPosition?.let {
                        setSelection(it.coerceIn(0, fieldText.length))
                    }
                }
                formattedNumber = null
                newCursorPosition = null

                ignoreChanges = false

                if (fieldText.length == formattedPanLength) {
                    val wasCardNumberValid = isCardNumberValid
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    shouldShowError = !isCardNumberValid
                    if (!wasCardNumberValid && isCardNumberValid) {
                        completionCallback()
                    }
                } else {
                    isCardNumberValid = CardUtils.isValidCardNumber(fieldText)
                    // Don't show errors if we aren't full-length.
                    shouldShowError = false
                }
            }
        })
    }

    @JvmSynthetic
    internal fun updateCardBrand(inputBin: Bin?) {
        // cancel in-flight job
        cancelAccountRangeRepositoryJob()

        accountRangeRepositoryJob = CoroutineScope(workDispatcher).launch {
            if (inputBin != null) {
                isProcessingCallback(true)
                onAccountRangeResult(
                    cardAccountRangeRepository.getAccountRange(inputBin.value)
                )
            } else {
                onAccountRangeResult(null)
            }
        }
    }

    private fun cancelAccountRangeRepositoryJob() {
        accountRangeRepositoryJob?.cancel()
        accountRangeRepositoryJob = null
    }

    private suspend fun onAccountRangeResult(
        accountRange: CardMetadata.AccountRange?
    ) = withContext(Dispatchers.Main) {
        panLength = accountRange?.panLength ?: CardNumber.DEFAULT_PAN_LENGTH
        cardBrand = accountRange?.brand ?: CardBrand.Unknown
        isProcessingCallback(false)
    }
}
