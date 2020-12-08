package com.stripe.android.view

import android.content.Context
import android.graphics.Rect
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
import java.math.BigDecimal
import java.math.RoundingMode
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

    private val cardNumberTextInputLayout = viewBinding.tlCardNumber
    private val expiryTextInputLayout = viewBinding.tlExpiry
    private val cvcInputLayout = viewBinding.tlCvc
    internal val postalInputLayout = viewBinding.tlPostalCode

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
    private var hasAdjustedDrawable: Boolean = false
    private var customCvcLabel: String? = null

    private var cardBrand: CardBrand = CardBrand.Unknown

    internal val brand: CardBrand
        @JvmSynthetic
        get() = cardBrand

    @ColorInt
    private val tintColorInt: Int

    private var cardHintText: String = resources.getString(R.string.card_number_hint)

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
                setOf(CARD_MULTILINE_TOKEN),
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

    private val pixelsToAdjust: Double =
        resources
            .getDimension(R.dimen.stripe_card_icon_multiline_padding_bottom)
            .toDouble()

    private val dynamicBufferInPixels: Int = BigDecimal(pixelsToAdjust)
        .setScale(0, RoundingMode.HALF_DOWN)
        .toInt()

    @VisibleForTesting
    internal var shouldShowErrorIcon = false
        private set(value) {
            val isValueChange = field != value
            field = value

            if (isValueChange) {
                updateBrandUi()
            }
        }

    init {
        orientation = VERTICAL

        tintColorInt = cardNumberEditText.hintTextColors.defaultColor

        // This sets the value of shouldShowPostalCode
        attrs?.let { checkAttributeSet(it) }

        initTextInputLayoutErrorHandlers(
            cardNumberTextInputLayout,
            expiryTextInputLayout,
            cvcInputLayout,
            postalInputLayout
        )

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
            } else {
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
        cardHintText = cardHint
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
        expiryTextInputLayout.isEnabled = enabled
        cardNumberTextInputLayout.isEnabled = enabled
        cvcInputLayout.isEnabled = enabled
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
        val linearParams = cvcInputLayout.layoutParams as LayoutParams
        linearParams.setMargins(0, 0, marginPixels, 0)
        linearParams.marginEnd = marginPixels

        cvcInputLayout.layoutParams = linearParams
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
            updateDrawable(
                iconResourceId = cardBrand.errorIcon,
                shouldTint = false
            )
        } else {
            updateDrawable(
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
                cardNumberEditText.setHintDelayed(cardHintText, CARD_NUMBER_HINT_DELAY)
                cardInputListener?.onFocusChange(CardInputListener.FocusField.CardNumber)
            } else {
                cardNumberEditText.hint = ""
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                expiryDateEditText.setHintDelayed(R.string.expiry_date_hint, COMMON_HINT_DELAY)
                cardInputListener?.onFocusChange(CardInputListener.FocusField.ExpiryDate)
            } else {
                expiryDateEditText.hint = ""
            }
        }

        cvcEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                flipToCvcIconIfNotFinished()
                cvcEditText.setHintDelayed(cvcHelperText, COMMON_HINT_DELAY)
                cardInputListener?.onFocusChange(CardInputListener.FocusField.Cvc)
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
                cardInputListener?.onFocusChange(CardInputListener.FocusField.PostalCode)
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
        if (shouldShowErrorIcon) {
            updateDrawable(
                iconResourceId = cardBrand.errorIcon,
                shouldTint = false
            )
        } else {
            updateDrawable(
                iconResourceId = cardBrand.icon,
                shouldTint = CardBrand.Unknown == cardBrand
            )
        }
    }

    private fun updateCvc() {
        cvcEditText.updateBrand(cardBrand, customCvcLabel, cvcInputLayout)
    }

    private fun updateDrawable(@DrawableRes iconResourceId: Int, shouldTint: Boolean) {
        val icon = ContextCompat.getDrawable(context, iconResourceId) ?: return
        val original = cardNumberEditText.compoundDrawablesRelative[0] ?: return
        val iconPadding = cardNumberEditText.compoundDrawablePadding
        icon.bounds = createDrawableBounds(original)

        val compatIcon = DrawableCompat.wrap(icon)
        if (shouldTint) {
            DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
        }

        cardNumberEditText.compoundDrawablePadding = iconPadding
        cardNumberEditText.setCompoundDrawablesRelative(
            compatIcon,
            null,
            null,
            null
        )
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

    private companion object {
        private const val CARD_MULTILINE_TOKEN = "CardMultilineView"
        private const val CARD_NUMBER_HINT_DELAY = 120L
        private const val COMMON_HINT_DELAY = 90L
    }
}
