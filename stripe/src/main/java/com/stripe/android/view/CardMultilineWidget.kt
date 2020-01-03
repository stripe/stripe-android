package com.stripe.android.view

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.CardUtils
import com.stripe.android.R
import com.stripe.android.model.Address
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CARD
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CVC
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_EXPIRY
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_POSTAL
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A multiline card input widget using the support design library's [TextInputLayout]
 * to match Material Design.
 */
class CardMultilineWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var shouldShowPostalCode: Boolean = DEFAULT_POSTAL_CODE_ENABLED
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val cardNumberEditText: CardNumberEditText
    private val expiryDateEditText: ExpiryDateEditText
    private val cvcEditText: CvcEditText
    private val postalCodeEditText: PostalCodeEditText
    private val cardNumberTextInputLayout: TextInputLayout
    private val expiryTextInputLayout: TextInputLayout
    private val cvcTextInputLayout: TextInputLayout
    private val postalInputLayout: TextInputLayout

    private var cardInputListener: CardInputListener? = null

    private var isEnabled: Boolean = false
    private var hasAdjustedDrawable: Boolean = false
    private var customCvcLabel: String? = null

    private var cardBrand: CardBrand = CardBrand.Unknown
    @ColorInt
    private val tintColorInt: Int

    private var cardHintText: String = resources.getString(R.string.card_number_hint)

    /**
     * Gets a [PaymentMethodCreateParams.Card] object from the user input, if all fields are
     * valid. If not, returns `null`.
     *
     * @return a valid [PaymentMethodCreateParams.Card] object based on user input, or
     * `null` if any field is invalid
     */
    override val paymentMethodCard: PaymentMethodCreateParams.Card?
        get() {
            return if (validateAllFields()) {
                expiryDateEditText.validDateFields?.let { (month, year) ->
                    PaymentMethodCreateParams.Card(
                        number = cardNumberEditText.cardNumber,
                        cvc = cvcEditText.text?.toString(),
                        expiryMonth = month,
                        expiryYear = year
                    )
                }
            } else {
                null
            }
        }

    /**
     * @return a valid [PaymentMethodCreateParams] object based on user input, or `null` if
     * any field is invalid. The object will include any billing details that the user entered.
     */
    override val paymentMethodCreateParams: PaymentMethodCreateParams?
        get() {
            return paymentMethodCard?.let {
                PaymentMethodCreateParams.create(it, paymentMethodBillingDetails)
            }
        }

    /**
     * @return a valid [PaymentMethod.BillingDetails] object based on user input, or
     * `null` if any field is invalid
     */
    val paymentMethodBillingDetails: PaymentMethod.BillingDetails?
        get() {
            return paymentMethodBillingDetailsBuilder?.build()
        }

    /**
     * @return a valid [PaymentMethod.BillingDetails.Builder] object based on user input, or
     * `null` if any field is invalid
     */
    val paymentMethodBillingDetailsBuilder: PaymentMethod.BillingDetails.Builder?
        get() = if (shouldShowPostalCode && validateAllFields()) {
            PaymentMethod.BillingDetails.Builder()
                .setAddress(Address.Builder()
                    .setPostalCode(postalCodeEditText.text?.toString())
                    .build()
                )
        } else {
            null
        }

    /**
     * Gets a [Card] object from the user input, if all fields are valid. If not, returns
     * `null`.
     *
     * @return a valid [Card] object based on user input, or `null` if any field is
     * invalid
     */
    override val card: Card?
        get() {
            return cardBuilder?.build()
        }

    override val cardBuilder: Card.Builder?
        get() {
            if (!validateAllFields()) {
                return null
            }

            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = requireNotNull(expiryDateEditText.validDateFields)
            val cvcValue = cvcEditText.text?.toString()
            val postalCode = postalCodeEditText.text?.toString()
                .takeIf { shouldShowPostalCode }

            return Card.Builder(cardNumber, cardDate.first, cardDate.second, cvcValue)
                .addressZip(postalCode)
                .loggingTokens(listOf(CARD_MULTILINE_TOKEN))
        }

    private val isCvcLengthValid: Boolean
        get() {
            return cardBrand.isValidCvc(cvcEditText.rawCvcValue)
        }

    private val cvcHelperText: Int
        @StringRes
        get() {
            return if (CardBrand.AmericanExpress == cardBrand) {
                R.string.cvc_multiline_helper_amex
            } else {
                R.string.cvc_multiline_helper
            }
        }

    private val pixelsToAdjust: Double =
        resources
            .getDimension(R.dimen.stripe_card_icon_multiline_padding_bottom)
            .toDouble()

    private val dynamicBufferInPixels: Int = BigDecimal(pixelsToAdjust)
        .setScale(0, RoundingMode.HALF_DOWN)
        .toInt()

    init {
        orientation = VERTICAL
        View.inflate(getContext(), R.layout.card_multiline_widget, this)

        cardNumberEditText = findViewById(R.id.et_card_number)
        expiryDateEditText = findViewById(R.id.et_expiry)
        cvcEditText = findViewById(R.id.et_cvc)
        postalCodeEditText = findViewById(R.id.et_postal_code)

        tintColorInt = cardNumberEditText.hintTextColors.defaultColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cardNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
            expiryDateEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
            postalCodeEditText.setAutofillHints(View.AUTOFILL_HINT_POSTAL_CODE)
        }

        // This sets the value of shouldShowPostalCode
        attrs?.let { checkAttributeSet(it) }

        cardNumberTextInputLayout = findViewById(R.id.tl_card_number)
        expiryTextInputLayout = findViewById(R.id.tl_expiry)
        // We dynamically set the hint of the CVC field, so we need to keep a reference.
        cvcTextInputLayout = findViewById(R.id.tl_cvc)
        postalInputLayout = findViewById(R.id.tl_postal_code)

        // configure postal code
        postalCodeEditText.configureForGlobal()
        postalInputLayout.hint = postalCodeEditText.hint
        postalCodeEditText.hint = ""

        initTextInputLayoutErrorHandlers(
            cardNumberTextInputLayout,
            expiryTextInputLayout,
            cvcTextInputLayout,
            postalInputLayout)

        initErrorMessages()
        initFocusChangeListeners()
        initDeleteEmptyListeners()

        cardNumberEditText.completionCallback = {
            expiryDateEditText.requestFocus()
            cardInputListener?.onCardComplete()
        }

        cardNumberEditText.brandChangeCallback = { brand ->
            cardBrand = brand
            updateBrandUi()
        }

        expiryDateEditText.completionCallback = {
            cvcEditText.requestFocus()
            cardInputListener?.onExpirationComplete()
        }

        cvcEditText.setAfterTextChangedListener(
            object : StripeEditText.AfterTextChangedListener {
                override fun onTextChanged(text: String) {
                    if (ViewUtils.isCvcMaximalLength(cardBrand, text)) {
                        updateBrandUi()
                        if (shouldShowPostalCode) {
                            postalCodeEditText.requestFocus()
                        }
                        cardInputListener?.onCvcComplete()
                    } else {
                        flipToCvcIconIfNotFinished()
                    }
                    cvcEditText.shouldShowError = false
                }
            })

        adjustViewForPostalCodeAttribute(shouldShowPostalCode)

        cardNumberEditText.updateLengthFilter()

        cardBrand = CardBrand.Unknown
        updateBrandUi()

        isEnabled = true
    }

    /**
     * Clear all entered data and hide all error messages.
     */
    override fun clear() {
        cardNumberEditText.setText("")
        expiryDateEditText.setText("")
        cvcEditText.setText("")
        postalCodeEditText.setText("")
        cardNumberEditText.shouldShowError = false
        expiryDateEditText.shouldShowError = false
        cvcEditText.shouldShowError = false
        postalCodeEditText.shouldShowError = false

        cardBrand = CardBrand.Unknown
        updateBrandUi()
    }

    /**
     * @param listener A [CardInputListener] to be notified of changes to the user's focused field
     */
    override fun setCardInputListener(listener: CardInputListener?) {
        this.cardInputListener = listener
    }

    override fun setCardHint(cardHint: String) {
        cardHintText = cardHint
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return `true` if all shown fields are valid, `false` otherwise
     */
    fun validateAllFields(): Boolean {
        val cardNumberIsValid = CardUtils.isValidCardNumber(cardNumberEditText.cardNumber)
        val expiryIsValid = expiryDateEditText.validDateFields != null
        val cvcIsValid = isCvcLengthValid
        cardNumberEditText.shouldShowError = !cardNumberIsValid
        expiryDateEditText.shouldShowError = !expiryIsValid
        cvcEditText.shouldShowError = !cvcIsValid

        val fields = listOf(
            cardNumberEditText, expiryDateEditText, cvcEditText, postalCodeEditText
        )
        for (field in fields) {
            if (field.shouldShowError) {
                field.requestFocus()
                break
            }
        }

        return cardNumberIsValid && expiryIsValid && cvcIsValid
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            updateBrandUi()
        }
    }

    /**
     * Set an optional CVC field label to override defaults, or `null` to use defaults.
     */
    fun setCvcLabel(cvcLabel: String?) {
        customCvcLabel = cvcLabel
        updateCvc()
    }

    fun setShouldShowPostalCode(shouldShowPostalCode: Boolean) {
        this.shouldShowPostalCode = shouldShowPostalCode
        adjustViewForPostalCodeAttribute(shouldShowPostalCode)
    }

    /**
     * Set the card number. Method does not change text field focus.
     *
     * @param cardNumber card number to be set
     */
    override fun setCardNumber(cardNumber: String?) {
        cardNumberEditText.setText(cardNumber)
    }

    override fun setExpiryDate(
        @IntRange(from = 1, to = 12) month: Int,
        @IntRange(from = 0, to = 9999) year: Int
    ) {
        expiryDateEditText.setText(DateUtils.createDateStringFromIntegerInput(month, year))
    }

    override fun setCvcCode(cvcCode: String?) {
        cvcEditText.setText(cvcCode)
    }

    /**
     * Checks whether the current card number is valid
     */
    fun validateCardNumber(): Boolean {
        val cardNumberIsValid = CardUtils.isValidCardNumber(cardNumberEditText.cardNumber)
        cardNumberEditText.shouldShowError = !cardNumberIsValid
        return cardNumberIsValid
    }

    /**
     * Expose a text watcher to receive updates when the card number is changed.
     */
    fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?) {
        cardNumberEditText.addTextChangedListener(cardNumberTextWatcher)
    }

    /**
     * Expose a text watcher to receive updates when the expiry date is changed.
     */
    fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?) {
        expiryDateEditText.addTextChangedListener(expiryDateTextWatcher)
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     */
    fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?) {
        cvcEditText.addTextChangedListener(cvcNumberTextWatcher)
    }

    /**
     * Expose a text watcher to receive updates when the cvc number is changed.
     */
    fun setPostalCodeTextWatcher(postalCodeTextWatcher: TextWatcher?) {
        postalCodeEditText.addTextChangedListener(postalCodeTextWatcher)
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        expiryTextInputLayout.isEnabled = enabled
        cardNumberTextInputLayout.isEnabled = enabled
        cvcTextInputLayout.isEnabled = enabled
        postalInputLayout.isEnabled = enabled
        isEnabled = enabled
    }

    private fun adjustViewForPostalCodeAttribute(shouldShowPostalCode: Boolean) {
        // Set the label/hint to the shorter value if we have three things in a row.
        @StringRes val expiryLabel = if (shouldShowPostalCode) {
            R.string.expiry_label_short
        } else {
            R.string.acc_label_expiry_date
        }
        expiryTextInputLayout.hint = resources.getString(expiryLabel)

        @IdRes val focusForward = if (shouldShowPostalCode) {
            R.id.et_postal_code
        } else {
            View.NO_ID
        }
        cvcEditText.nextFocusForwardId = focusForward
        cvcEditText.nextFocusDownId = focusForward

        val postalCodeVisibility = if (shouldShowPostalCode) {
            View.VISIBLE
        } else {
            View.GONE
        }
        postalInputLayout.visibility = postalCodeVisibility

        // If the postal code field is not shown, the CVC field is the last one in the form and the
        // action on the keyboard when the CVC field is focused should be "Done". Otherwise, show
        // the "Next" action.
        cvcEditText.imeOptions = if (postalCodeVisibility == View.GONE) {
            EditorInfo.IME_ACTION_DONE
        } else {
            EditorInfo.IME_ACTION_NEXT
        }

        val marginPixels = if (shouldShowPostalCode) {
            resources.getDimensionPixelSize(R.dimen.stripe_add_card_expiry_middle_margin)
        } else {
            0
        }
        val linearParams = cvcTextInputLayout.layoutParams as LayoutParams
        linearParams.setMargins(0, 0, marginPixels, 0)
        linearParams.marginEnd = marginPixels

        cvcTextInputLayout.layoutParams = linearParams
    }

    private fun checkAttributeSet(attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CardElement,
            0, 0)

        try {
            shouldShowPostalCode = a.getBoolean(
                R.styleable.CardElement_shouldShowPostalCode, DEFAULT_POSTAL_CODE_ENABLED
            )
        } finally {
            a.recycle()
        }
    }

    private fun flipToCvcIconIfNotFinished() {
        if (ViewUtils.isCvcMaximalLength(cardBrand, cvcEditText.text?.toString())) {
            return
        }

        updateDrawable(cardBrand.cvcIcon, true)
    }

    private fun initDeleteEmptyListeners() {
        expiryDateEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(cardNumberEditText))

        cvcEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(expiryDateEditText))

        // It doesn't matter whether or not the postal code is shown;
        // we can still say where you go when you delete an empty field from it.
        postalCodeEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(cvcEditText))
    }

    private fun initErrorMessages() {
        cardNumberEditText.setErrorMessage(context.getString(R.string.invalid_card_number))
        expiryDateEditText.setErrorMessage(context.getString(R.string.invalid_expiry_year))
        cvcEditText.setErrorMessage(context.getString(R.string.invalid_cvc))
        postalCodeEditText.setErrorMessage(context.getString(R.string.invalid_zip))
    }

    private fun initFocusChangeListeners() {
        cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                cardNumberEditText.setHintDelayed(cardHintText, CARD_NUMBER_HINT_DELAY)
                cardInputListener?.onFocusChange(FOCUS_CARD)
            } else {
                cardNumberEditText.hint = ""
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                expiryDateEditText.setHintDelayed(R.string.expiry_date_hint, COMMON_HINT_DELAY)
                cardInputListener?.onFocusChange(FOCUS_EXPIRY)
            } else {
                expiryDateEditText.hint = ""
            }
        }

        cvcEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                flipToCvcIconIfNotFinished()
                cvcEditText.setHintDelayed(cvcHelperText, COMMON_HINT_DELAY)
                cardInputListener?.onFocusChange(FOCUS_CVC)
            } else {
                updateBrandUi()
                cvcEditText.hint = ""
            }
        }

        postalCodeEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!shouldShowPostalCode) {
                return@OnFocusChangeListener
            }
            if (hasFocus) {
                postalCodeEditText.setHintDelayed(R.string.zip_helper, COMMON_HINT_DELAY)
                cardInputListener?.onFocusChange(FOCUS_POSTAL)
            } else {
                postalCodeEditText.hint = ""
            }
        }
    }

    private fun initTextInputLayoutErrorHandlers(
        cardInputLayout: TextInputLayout,
        expiryInputLayout: TextInputLayout,
        cvcTextInputLayout: TextInputLayout,
        postalInputLayout: TextInputLayout
    ) {
        cardNumberEditText.setErrorMessageListener(ErrorListener(cardInputLayout))
        expiryDateEditText.setErrorMessageListener(ErrorListener(expiryInputLayout))
        cvcEditText.setErrorMessageListener(ErrorListener(cvcTextInputLayout))
        postalCodeEditText.setErrorMessageListener(ErrorListener(postalInputLayout))
    }

    private fun updateBrandUi() {
        updateCvc()
        updateDrawable(cardBrand.icon, CardBrand.Unknown == cardBrand)
    }

    private fun updateCvc() {
        cvcEditText.updateBrand(cardBrand, customCvcLabel, cvcTextInputLayout)
    }

    private fun updateDrawable(@DrawableRes iconResourceId: Int, needsTint: Boolean) {
        val icon = ContextCompat.getDrawable(context, iconResourceId) ?: return
        val original = cardNumberEditText.compoundDrawablesRelative[0] ?: return
        val iconPadding = cardNumberEditText.compoundDrawablePadding
        icon.bounds = createDrawableBounds(original)

        val compatIcon = DrawableCompat.wrap(icon)
        if (needsTint) {
            DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
        }

        cardNumberEditText.compoundDrawablePadding = iconPadding
        cardNumberEditText.setCompoundDrawablesRelative(compatIcon, null, null, null)
    }

    private fun createDrawableBounds(drawable: Drawable): Rect {
        val newBounds = Rect()
        drawable.copyBounds(newBounds)

        if (!hasAdjustedDrawable) {
            newBounds.top = newBounds.top - dynamicBufferInPixels
            newBounds.bottom = newBounds.bottom - dynamicBufferInPixels
            hasAdjustedDrawable = true
        }

        return newBounds
    }

    internal companion object {
        internal const val CARD_MULTILINE_TOKEN = "CardMultilineView"
        internal const val CARD_NUMBER_HINT_DELAY = 120L
        internal const val COMMON_HINT_DELAY = 90L

        private const val DEFAULT_POSTAL_CODE_ENABLED = true
    }
}
