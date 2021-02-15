package com.stripe.android.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.CardNumber
import com.stripe.android.databinding.CardMultilineWidgetBinding
import com.stripe.android.model.Address
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ExpirationDate
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlin.properties.Delegates

/**
 * A multiline card input widget that uses Material Components for Android.
 *
 * To enable 19-digit card support, [PaymentConfiguration.init] must be called before
 * [CardMultilineWidget] is instantiated.
 */
class CardMultilineWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var shouldShowPostalCode: Boolean = CardWidget.DEFAULT_POSTAL_CODE_ENABLED
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val viewBinding = CardMultilineWidgetBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    internal val cardNumberEditText = viewBinding.etCardNumber
    internal val expiryDateEditText = viewBinding.etExpiry
    internal val cvcEditText = viewBinding.etCvc
    internal val postalCodeEditText = viewBinding.etPostalCode

    internal val secondRowLayout = viewBinding.secondRowLayout
    internal val cardNumberTextInputLayout = viewBinding.tlCardNumber
    internal val expiryTextInputLayout = viewBinding.tlExpiry
    internal val cvcInputLayout = viewBinding.tlCvc
    internal val postalInputLayout = viewBinding.tlPostalCode

    private val textInputLayouts = listOf(
        cardNumberTextInputLayout,
        expiryTextInputLayout,
        cvcInputLayout,
        postalInputLayout
    )

    private var cardInputListener: CardInputListener? = null
    private var cardValidCallback: CardValidCallback? = null
    private val cardValidTextWatcher = object : StripeTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
        }
    }
    private val invalidFields: Set<CardValidCallback.Fields>
        get() {
            return listOfNotNull(
                CardValidCallback.Fields.Number.takeIf {
                    validatedCardNumber == null
                },
                CardValidCallback.Fields.Expiry.takeIf {
                    expirationDate == null
                },
                CardValidCallback.Fields.Cvc.takeIf {
                    cvcEditText.cvc == null
                }
            ).toSet()
        }

    private var isEnabled: Boolean = false
    private var customCvcLabel: String? = null

    private var cardBrand: CardBrand = CardBrand.Unknown

    internal val brand: CardBrand
        @JvmSynthetic
        get() = cardBrand

    @ColorInt
    private val tintColorInt: Int

    /**
     * If [shouldShowPostalCode] is true and [postalCodeRequired] is true, then postal code is a
     * required field.
     *
     * If [shouldShowPostalCode] is false, this value is ignored.
     *
     * Note that some countries do not have postal codes, so requiring postal code will prevent
     * those users from submitting this form successfully.
     */
    var postalCodeRequired: Boolean = CardWidget.DEFAULT_POSTAL_CODE_REQUIRED

    /**
     * If [shouldShowPostalCode] is true and [usZipCodeRequired] is true, then postal code is a
     * required field and must be a 5-digit US zip code.
     *
     * If [shouldShowPostalCode] is false, this value is ignored.
     */
    var usZipCodeRequired: Boolean by Delegates.observable(
        CardWidget.DEFAULT_US_ZIP_CODE_REQUIRED
    ) { _, _, zipCodeRequired ->
        if (zipCodeRequired) {
            postalCodeEditText.config = PostalCodeEditText.Config.US
        } else {
            postalCodeEditText.config = PostalCodeEditText.Config.Global
        }
    }

    /**
     * A [PaymentMethodCreateParams.Card] representing the card details if all fields are valid;
     * otherwise `null`
     */
    override val paymentMethodCard: PaymentMethodCreateParams.Card?
        get() {
            return cardParams?.let {
                PaymentMethodCreateParams.Card(
                    number = it.number,
                    cvc = it.cvc,
                    expiryMonth = it.expMonth,
                    expiryYear = it.expYear,
                    attribution = it.attribution
                )
            }
        }

    /**
     * A [PaymentMethodCreateParams] representing the card details and postal code if all fields
     * are valid; otherwise `null`
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
     * A [PaymentMethod.BillingDetails.Builder] representing the card details and postal code if all fields are valid;
     * otherwise `null`
     */
    val paymentMethodBillingDetailsBuilder: PaymentMethod.BillingDetails.Builder?
        get() = if (shouldShowPostalCode && validateAllFields()) {
            PaymentMethod.BillingDetails.Builder()
                .setAddress(
                    Address.Builder()
                        .setPostalCode(postalCodeEditText.postalCode)
                        .build()
                )
        } else {
            null
        }

    /**
     * A [Card] representing the card details and postal code if all fields are valid;
     * otherwise `null`
     */
    @Deprecated("Use cardParams", ReplaceWith("cardParams"))
    override val card: Card?
        get() {
            return cardBuilder?.build()
        }

    /**
     * A [CardParams] representing the card details and postal code if all fields are valid;
     * otherwise `null`
     */
    override val cardParams: CardParams?
        get() {
            if (!validateAllFields()) {
                shouldShowErrorIcon = true
                return null
            }

            shouldShowErrorIcon = false

            val expirationDate = requireNotNull(expiryDateEditText.validatedDate)
            val cvcValue = cvcEditText.text?.toString()
            val postalCode = postalCodeEditText.text?.toString()
                .takeIf { shouldShowPostalCode }

            return CardParams(
                brand = brand,
                loggingTokens = setOf(CARD_MULTILINE_TOKEN),
                number = validatedCardNumber?.value.orEmpty(),
                expMonth = expirationDate.month,
                expYear = expirationDate.year,
                cvc = cvcValue,
                address = Address.Builder()
                    .setPostalCode(postalCode.takeUnless { it.isNullOrBlank() })
                    .build()
            )
        }

    /**
     * A [Card.Builder] representing the card details and postal code if all fields are valid;
     * otherwise `null`
     */
    @Deprecated("Use cardParams", ReplaceWith("cardParams"))
    override val cardBuilder: Card.Builder?
        get() {
            if (!validateAllFields()) {
                shouldShowErrorIcon = true
                return null
            }

            shouldShowErrorIcon = false

            val expirationDate = requireNotNull(expiryDateEditText.validatedDate)
            val cvcValue = cvcEditText.text?.toString()
            val postalCode = postalCodeEditText.text?.toString()
                .takeIf { shouldShowPostalCode }

            return Card.Builder(
                number = validatedCardNumber?.value,
                expMonth = expirationDate.month,
                expYear = expirationDate.year,
                cvc = cvcValue
            )
                .addressZip(postalCode)
                .loggingTokens(setOf(CARD_MULTILINE_TOKEN))
        }

    private val validatedCardNumber: CardNumber.Validated?
        get() {
            return cardNumberEditText.validatedCardNumber
        }

    private val expirationDate: ExpirationDate.Validated?
        get() = expiryDateEditText.validatedDate

    private val allFields: Collection<StripeEditText>
        get() {
            return listOf(
                cardNumberEditText,
                expiryDateEditText,
                cvcEditText,
                postalCodeEditText
            )
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

    @VisibleForTesting
    internal var shouldShowErrorIcon = false
        private set(value) {
            val isValueChange = field != value
            field = value

            if (isValueChange) {
                updateBrandUi()
            }
        }

    internal var expirationDatePlaceholderRes: Int? by Delegates.observable(
        R.string.expiry_date_hint
    ) { _, _, newValue ->
        expiryTextInputLayout.placeholderText = newValue?.let {
            resources.getString(it)
        }.orEmpty()
    }

    private var showCvcIconInCvcField: Boolean = false

    internal var cardBrandIconSupplier: CardBrandIconSupplier by Delegates.observable(
        DEFAULT_CARD_BRAND_ICON_SUPPLIER
    ) { _, _, _ ->
        updateBrandUi()
    }

    internal var cardNumberErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(cardNumberTextInputLayout)
    ) { _, _, newValue ->
        cardNumberEditText.setErrorMessageListener(newValue)
    }
    internal var expirationDateErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(expiryTextInputLayout)
    ) { _, _, newValue ->
        expiryDateEditText.setErrorMessageListener(newValue)
    }
    internal var cvcErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(cvcInputLayout)
    ) { _, _, newValue ->
        cvcEditText.setErrorMessageListener(newValue)
    }
    internal var postalCodeErrorListener: StripeEditText.ErrorMessageListener? by Delegates.observable(
        ErrorListener(postalInputLayout)
    ) { _, _, newValue ->
        postalCodeEditText.setErrorMessageListener(newValue)
    }

    init {
        orientation = VERTICAL

        tintColorInt = cardNumberEditText.hintTextColors.defaultColor

        textInputLayouts.forEach {
            it.placeholderTextColor = it.editText?.hintTextColors
        }

        // This sets the value of shouldShowPostalCode
        attrs?.let { checkAttributeSet(it) }

        initTextInputLayoutErrorHandlers()

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

        cvcEditText.setAfterTextChangedListener { text ->
            if (cardBrand.isMaxCvc(text)) {
                updateBrandUi()
                if (shouldShowPostalCode) {
                    postalCodeEditText.requestFocus()
                }
                cardInputListener?.onCvcComplete()
            } else if (!showCvcIconInCvcField) {
                flipToCvcIconIfNotFinished()
            }
            cvcEditText.shouldShowError = false
        }

        adjustViewForPostalCodeAttribute(shouldShowPostalCode)

        cardNumberEditText.updateLengthFilter()

        cardBrand = CardBrand.Unknown
        updateBrandUi()

        allFields.forEach { field ->
            field.doAfterTextChanged {
                shouldShowErrorIcon = false
            }
        }

        cardNumberEditText.isLoadingCallback = {
            cardNumberTextInputLayout.isLoading = it
        }

        isEnabled = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postalCodeEditText.config = PostalCodeEditText.Config.Global
        cvcEditText.hint = null
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

    override fun setCardValidCallback(callback: CardValidCallback?) {
        this.cardValidCallback = callback
        allFields.forEach { it.removeTextChangedListener(cardValidTextWatcher) }

        // only add the TextWatcher if it will be used
        if (callback != null) {
            allFields.forEach { it.addTextChangedListener(cardValidTextWatcher) }
        }

        // call immediately after setting
        cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
    }

    override fun setCardHint(cardHint: String) {
        cardNumberTextInputLayout.placeholderText = cardHint
    }

    /**
     * Validates all fields and shows error messages if appropriate.
     *
     * @return `true` if all shown fields are valid, `false` otherwise
     */
    fun validateAllFields(): Boolean {
        val cardNumberIsValid = validatedCardNumber != null
        val expiryIsValid = expirationDate != null
        val cvcIsValid = cvcEditText.cvc != null
        cardNumberEditText.shouldShowError = !cardNumberIsValid
        expiryDateEditText.shouldShowError = !expiryIsValid
        cvcEditText.shouldShowError = !cvcIsValid
        postalCodeEditText.shouldShowError =
            (postalCodeRequired || usZipCodeRequired) &&
            postalCodeEditText.postalCode.isNullOrBlank()

        allFields.firstOrNull { it.shouldShowError }?.requestFocus()

        return cardNumberIsValid && expiryIsValid && cvcIsValid && !postalCodeEditText.shouldShowError
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

    @JvmSynthetic
    internal fun setCvcIcon(resId: Int?) {
        if (resId != null) {
            cvcInputLayout.setEndIconDrawable(resId)
            cvcInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM
        } else {
            cvcInputLayout.setEndIconDrawable(0)
            cvcInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }
        showCvcIconInCvcField = resId != null
    }

    /**
     * The postal code field is enabled by default. Disabling the postal code field may impact
     * auth success rates, so it is discouraged to disable it unless you are collecting the postal
     * code outside of this form.
     */
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
        expiryDateEditText.setText(
            ExpirationDate.Unvalidated(month, year).getDisplayString()
        )
    }

    override fun setCvcCode(cvcCode: String?) {
        cvcEditText.setText(cvcCode)
    }

    /**
     * Checks whether the current card number is valid
     */
    fun validateCardNumber(): Boolean {
        val cardNumberIsValid = validatedCardNumber != null
        cardNumberEditText.shouldShowError = !cardNumberIsValid
        return cardNumberIsValid
    }

    /**
     * Set a `TextWatcher` to receive card number changes.
     */
    override fun setCardNumberTextWatcher(cardNumberTextWatcher: TextWatcher?) {
        cardNumberEditText.addTextChangedListener(cardNumberTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive expiration date changes.
     */
    override fun setExpiryDateTextWatcher(expiryDateTextWatcher: TextWatcher?) {
        expiryDateEditText.addTextChangedListener(expiryDateTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive CVC value changes.
     */
    override fun setCvcNumberTextWatcher(cvcNumberTextWatcher: TextWatcher?) {
        cvcEditText.addTextChangedListener(cvcNumberTextWatcher)
    }

    /**
     * Set a `TextWatcher` to receive postal code changes.
     */
    override fun setPostalCodeTextWatcher(postalCodeTextWatcher: TextWatcher?) {
        postalCodeEditText.addTextChangedListener(postalCodeTextWatcher)
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        textInputLayouts.forEach { it.isEnabled = enabled }
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

        cvcInputLayout.updateLayoutParams<LayoutParams> {
            marginEnd = if (shouldShowPostalCode) {
                resources.getDimensionPixelSize(R.dimen.stripe_add_card_expiry_middle_margin)
            } else {
                0
            }
        }
    }

    private fun checkAttributeSet(attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CardElement,
            0,
            0
        )

        try {
            shouldShowPostalCode = a.getBoolean(
                R.styleable.CardElement_shouldShowPostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_ENABLED
            )
            postalCodeRequired = a.getBoolean(
                R.styleable.CardElement_shouldRequirePostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_REQUIRED
            )
            usZipCodeRequired = a.getBoolean(
                R.styleable.CardElement_shouldRequireUsZipCode,
                CardWidget.DEFAULT_US_ZIP_CODE_REQUIRED
            )
        } finally {
            a.recycle()
        }
    }

    private fun flipToCvcIconIfNotFinished() {
        if (cardBrand.isMaxCvc(cvcEditText.fieldText)) {
            return
        }

        if (shouldShowErrorIcon) {
            updateCardNumberIcon(
                iconResourceId = cardBrand.errorIcon,
                shouldTint = false
            )
        } else {
            updateCardNumberIcon(
                iconResourceId = cardBrand.cvcIcon,
                shouldTint = true
            )
        }
    }

    private fun initDeleteEmptyListeners() {
        expiryDateEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(cardNumberEditText)
        )

        cvcEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(expiryDateEditText)
        )

        // It doesn't matter whether or not the postal code is shown;
        // we can still say where you go when you delete an empty field from it.
        postalCodeEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(cvcEditText)
        )
    }

    private fun initFocusChangeListeners() {
        cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                cardInputListener?.onFocusChange(CardInputListener.FocusField.CardNumber)
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                cardInputListener?.onFocusChange(CardInputListener.FocusField.ExpiryDate)
            }
        }

        cvcEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (!showCvcIconInCvcField) {
                    flipToCvcIconIfNotFinished()
                }
                cardInputListener?.onFocusChange(CardInputListener.FocusField.Cvc)
            } else {
                updateBrandUi()
            }
        }

        postalCodeEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (shouldShowPostalCode && hasFocus) {
                cardInputListener?.onFocusChange(CardInputListener.FocusField.PostalCode)
            }
        }
    }

    private fun initTextInputLayoutErrorHandlers() {
        cardNumberEditText.setErrorMessageListener(cardNumberErrorListener)
        expiryDateEditText.setErrorMessageListener(expirationDateErrorListener)
        cvcEditText.setErrorMessageListener(cvcErrorListener)
        postalCodeEditText.setErrorMessageListener(postalCodeErrorListener)
    }

    private fun updateBrandUi() {
        updateCvc()
        if (shouldShowErrorIcon) {
            updateCardNumberIcon(
                iconResourceId = cardBrand.errorIcon,
                shouldTint = false
            )
        } else {
            val cardBrandIcon = cardBrandIconSupplier.get(cardBrand)
            updateCardNumberIcon(
                iconResourceId = cardBrandIcon.iconResourceId,
                shouldTint = cardBrandIcon.shouldTint
            )
        }
    }

    private fun updateCvc() {
        cvcEditText.updateBrand(cardBrand, customCvcLabel, cvcInputLayout)
    }

    private fun updateCardNumberIcon(
        @DrawableRes iconResourceId: Int,
        shouldTint: Boolean
    ) {
        ContextCompat.getDrawable(context, iconResourceId)?.let { icon ->
            updateCompoundDrawable(
                if (shouldTint) {
                    DrawableCompat.wrap(icon).also {
                        it.setTint(tintColorInt)
                    }
                } else {
                    icon
                }
            )
        }
    }

    private fun updateCompoundDrawable(drawable: Drawable) {
        cardNumberEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }

    internal fun interface CardBrandIconSupplier {
        fun get(cardBrand: CardBrand): CardBrandIcon
    }

    internal data class CardBrandIcon(
        val iconResourceId: Int,
        val shouldTint: Boolean = false
    )

    private companion object {
        private const val CARD_MULTILINE_TOKEN = "CardMultilineView"

        private val DEFAULT_CARD_BRAND_ICON_SUPPLIER = CardBrandIconSupplier { cardBrand ->
            CardBrandIcon(
                cardBrand.icon,
                cardBrand == CardBrand.Unknown
            )
        }
    }
}
