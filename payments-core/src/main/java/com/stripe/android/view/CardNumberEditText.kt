package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.cards.DefaultStaticCardAccountRanges
import com.stripe.android.cards.StaticCardAccountRanges
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import androidx.appcompat.R as AppCompatR

/**
 * A [StripeEditText] that handles spacing out the digits of a credit card.
 */
@SuppressWarnings("LongParameterList")
class CardNumberEditText internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.editTextStyle,

    // TODO(mshafrir-stripe): make immutable after `CardWidgetViewModel` is integrated in `CardWidget` subclasses
    @get:VisibleForTesting
    var workContext: CoroutineContext,

    private val cardAccountRangeRepository: CardAccountRangeRepository,
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
) : StripeEditText(context, attrs, defStyleAttr) {

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = AppCompatR.attr.editTextStyle
    ) : this(
        context,
        attrs,
        defStyleAttr,
        Dispatchers.IO,
        { PaymentConfiguration.getInstance(context).publishableKey }
    )

    private constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        workContext: CoroutineContext,
        publishableKeySupplier: () -> String
    ) : this(
        context,
        attrs,
        defStyleAttr,
        workContext,
        DefaultCardAccountRangeRepositoryFactory(context).create(),
        DefaultStaticCardAccountRanges(),
        DefaultAnalyticsRequestExecutor(context),
        PaymentAnalyticsRequestFactory(
            context,
            publishableKeyProvider = publishableKeySupplier
        )
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

    internal val panLength: Int
        get() = accountRangeService.accountRange?.panLength
            ?: accountRangeService.staticCardAccountRanges.first(unvalidatedCardNumber)?.panLength
            ?: CardNumber.DEFAULT_PAN_LENGTH

    private val formattedPanLength: Int
        get() = panLength + CardNumber.getSpacePositions(panLength).size

    /**
     * Check whether or not the card number is valid
     */
    var isCardNumberValid: Boolean = false
        private set

    internal val validatedCardNumber: CardNumber.Validated?
        get() = unvalidatedCardNumber.validate(panLength)

    private val unvalidatedCardNumber: CardNumber.Unvalidated
        get() = CardNumber.Unvalidated(fieldText)

    private val isValid: Boolean
        get() = validatedCardNumber != null

    @VisibleForTesting
    val accountRangeService = CardAccountRangeService(
        cardAccountRangeRepository,
        workContext,
        staticCardAccountRanges,
        object : CardAccountRangeService.AccountRangeResultListener {
            override fun onAccountRangeResult(newAccountRange: AccountRange?) {
                updateLengthFilter()
                cardBrand = newAccountRange?.brand ?: CardBrand.Unknown
            }
        }
    )

    @JvmSynthetic
    internal var isLoadingCallback: (Boolean) -> Unit = {}

    private var loadingJob: Job? = null

    init {
        setNumberOnlyInputType()

        setErrorMessage(resources.getString(R.string.stripe_invalid_card_number))
        addTextChangedListener(CardNumberTextWatcher())

        internalFocusChangeListeners.add { _, hasFocus ->
            if (!hasFocus && unvalidatedCardNumber.isPartialEntry(panLength)) {
                shouldShowError = true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
        }

        updateLengthFilter()

        this.layoutDirection = LAYOUT_DIRECTION_LTR
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadingJob = CoroutineScope(workContext).launch {
            cardAccountRangeRepository.loading.collect {
                withContext(Dispatchers.Main) {
                    isLoadingCallback(it)
                }
            }
        }
    }

    override val accessibilityText: String
        get() {
            return resources.getString(R.string.stripe_acc_label_card_number_node, text)
        }

    override fun onDetachedFromWindow() {
        loadingJob?.cancel()
        loadingJob = null

        accountRangeService.cancelAccountRangeRepositoryJob()

        super.onDetachedFromWindow()
    }

    @JvmSynthetic
    internal fun updateLengthFilter(maxLength: Int = formattedPanLength) {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
    }

    /**
     * Updates the selection index based on the current (pre-edit) index, and
     * the size change of the number being input.
     *
     * @param newFormattedLength the post-edit length of the string
     * @param start the position in the string at which the edit action starts
     * @param addedDigits the number of new characters going into the string (zero for
     * delete)
     * @param panLength the maximum normalized length of the PAN
     * @return an index within the string at which to put the cursor
     */
    @JvmSynthetic
    internal fun calculateCursorPosition(
        newFormattedLength: Int,
        start: Int,
        addedDigits: Int,
        panLength: Int = this.panLength
    ): Int {
        val gapSet = CardNumber.getSpacePositions(panLength)

        val gapsJumped = gapSet.count { gap ->
            start <= gap && start + addedDigits >= gap
        }

        val skipBack = gapSet.any { gap ->
            // addedDigits can only be 0 if we are deleting,
            // so we need to check whether or not to skip backwards one space
            addedDigits == 0 && start == gap + 1
        }

        var newPosition = start + addedDigits + gapsJumped
        if (skipBack && newPosition > 0) {
            newPosition--
        }

        return if (newPosition <= newFormattedLength) {
            newPosition
        } else {
            newFormattedLength
        }
    }

    @JvmSynthetic
    internal fun onCardMetadataLoadedTooSlow() {
        analyticsRequestExecutor.executeAsync(
            paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.CardMetadataLoadedTooSlow)
        )
    }

    private inner class CardNumberTextWatcher : StripeTextWatcher() {
        private var latestChangeStart: Int = 0
        private var latestInsertionSize: Int = 0

        private var newCursorPosition: Int? = null
        private var formattedNumber: String? = null

        private var beforeCardNumber = unvalidatedCardNumber

        private var isPastedPan = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            isPastedPan = false
            beforeCardNumber = unvalidatedCardNumber

            latestChangeStart = start
            latestInsertionSize = after
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val cardNumber = CardNumber.Unvalidated(s?.toString().orEmpty())
            accountRangeService.onCardNumberChanged(cardNumber)

            isPastedPan = isPastedPan(start, before, count, cardNumber)

            if (isPastedPan) {
                updateLengthFilter(cardNumber.getFormatted(cardNumber.length).length)
            }

            if (isPastedPan) {
                cardNumber.length
            } else {
                panLength
            }.let { maxPanLength ->
                val formattedNumber = cardNumber.getFormatted(maxPanLength)
                newCursorPosition = calculateCursorPosition(
                    formattedNumber.length,
                    latestChangeStart,
                    latestInsertionSize,
                    maxPanLength
                )
                this.formattedNumber = formattedNumber
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if (shouldUpdateAfterChange) {
                setTextSilent(formattedNumber)
                newCursorPosition?.let {
                    setSelection(it.coerceIn(0, fieldText.length))
                }
            }

            formattedNumber = null
            newCursorPosition = null

            if (unvalidatedCardNumber.length == panLength) {
                val wasCardNumberValid = isCardNumberValid
                isCardNumberValid = isValid
                shouldShowError = !isValid

                if (accountRangeService.accountRange == null && unvalidatedCardNumber.isValidLuhn) {
                    // a complete PAN was inputted before the card service returned results
                    onCardMetadataLoadedTooSlow()
                }

                if (isComplete(wasCardNumberValid)) {
                    completionCallback()
                }
            } else if (unvalidatedCardNumber.isPartialEntry(panLength) &&
                !unvalidatedCardNumber.isPossibleCardBrand()
            ) {
                // Partial card number entered and brand is not yet determine, but possible.
                isCardNumberValid = isValid
                shouldShowError = true
            } else {
                isCardNumberValid = isValid
                // Don't show errors if we aren't full-length and the brand is known.
                // TODO (michelleb-stripe) Should set error message to incomplete, then in focus change if it isn't complete it will update it.
                shouldShowError = false
            }
        }

        private val shouldUpdateAfterChange: Boolean
            get() = (digitsAdded || !isLastKeyDelete) && formattedNumber != null

        /**
         * Have digits been added in this text change.
         */
        private val digitsAdded: Boolean
            get() = unvalidatedCardNumber.length > beforeCardNumber.length

        /**
         * If `true`, [completionCallback] will be invoked.
         */
        private fun isComplete(
            wasCardNumberValid: Boolean
        ) = !wasCardNumberValid && (
            unvalidatedCardNumber.isMaxLength ||
                (isValid && accountRangeService.accountRange != null)
            )

        /**
         * The [currentCount] characters beginning at [startPosition] have just replaced old text
         * that had length [previousCount]. If [currentCount] < [previousCount], digits were
         * deleted.
         */
        private fun isPastedPan(
            startPosition: Int,
            previousCount: Int,
            currentCount: Int,
            cardNumber: CardNumber.Unvalidated
        ): Boolean {
            return currentCount > previousCount && startPosition == 0 &&
                cardNumber.normalized.length >= CardNumber.MIN_PAN_LENGTH
        }
    }
}
