package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.CardNumber
import com.stripe.android.databinding.StripeCardMultilineWidgetBinding
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.DelicateCardDetailsApi
import com.stripe.android.model.ExpirationDate
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlin.properties.Delegates

/**
 * A multiline card input widget that uses Material Components for Android.
 *
 * To enable 19-digit card support, [PaymentConfiguration.init] must be called before
 * [CardMultilineWidget] is instantiated.
 *
 * The card number, cvc, and expiry date will always be left to right regardless of locale.  Postal
 * code layout direction will be set according to the locale.
 */
class CardMultilineWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var shouldShowPostalCode: Boolean = CardWidget.DEFAULT_POSTAL_CODE_ENABLED
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val viewBinding = StripeCardMultilineWidgetBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val cardNumberEditText = viewBinding.etCardNumber

    internal val cardBrandView: CardBrandView = viewBinding.cardBrandView

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val expiryDateEditText = viewBinding.etExpiry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val cvcEditText = viewBinding.etCvc

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    internal val postalCodeEditText = viewBinding.etPostalCode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val secondRowLayout = viewBinding.secondRowLayout

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val cardNumberTextInputLayout = viewBinding.tlCardNumber

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val expiryTextInputLayout = viewBinding.tlExpiry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    val cvcInputLayout = viewBinding.tlCvc
    internal val postalInputLayout = viewBinding.tlPostalCode

    private val lifecycleDelegate = LifecycleOwnerDelegate()

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
    internal val invalidFields: Set<CardValidCallback.Fields>
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
                },
                CardValidCallback.Fields.Postal.takeIf {
                    isPostalRequired() && postalCodeEditText.postalCode.isNullOrBlank()
                }
            ).toSet()
        }

    private var isEnabled: Boolean = false
    private var customCvcLabel: String? = null
    private var customCvcPlaceholderText: String? = null

    /**
     * A [CardBrand] matching the current card number inputted by the user.
     */
    val brand: CardBrand
        @JvmSynthetic
        get() = cardBrandView.brand

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

    private fun isPostalRequired() =
        (postalCodeRequired || usZipCodeRequired) && shouldShowPostalCode

    internal var viewModelStoreOwner: ViewModelStoreOwner? = null

    /**
     * A [PaymentMethodCreateParams.Card] representing the card details if all fields are valid;
     * otherwise `null`
     */
    override val paymentMethodCard: PaymentMethodCreateParams.Card?
        @OptIn(DelicateCardDetailsApi::class)
        get() {
            return cardParams?.let {
                PaymentMethodCreateParams.Card(
                    number = it.number,
                    cvc = it.cvc,
                    expiryMonth = it.expMonth,
                    expiryYear = it.expYear,
                    attribution = it.attribution,
                    networks = cardBrandView.paymentMethodCreateParamsNetworks(),
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
     * The Stripe account ID (if any) which is the business of record.
     * See [use cases](https://docs.stripe.com/connect/charges#on_behalf_of) to determine if this option is relevant
     * for your integration. This should match the
     * [on_behalf_of](https://docs.stripe.com/api/payment_intents/create#create_payment_intent-on_behalf_of)
     * provided on the Intent used when confirming payment.
     */
    var onBehalfOf: String? = null
        set(value) {
            if (field != value) {
                if (isAttachedToWindow) {
                    doWithCardWidgetViewModel(viewModelStoreOwner) { viewModel ->
                        viewModel.setOnBehalfOf(value)
                    }
                }
                field = value
            }
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
                    .build(),
                networks = cardBrandView.cardParamsNetworks()
            )
        }

    internal val validatedCardNumber: CardNumber.Validated?
        get() {
            return cardNumberEditText.validatedCardNumber
        }

    private val expirationDate: ExpirationDate.Validated?
        get() = expiryDateEditText.validatedDate

    private val allFields: Collection<StripeEditText>
        get() {
            return setOf(
                cardNumberEditText,
                expiryDateEditText,
                cvcEditText,
                postalCodeEditText
            )
        }

    @VisibleForTesting
    internal var shouldShowErrorIcon = false
        set(value) {
            val isValueChange = field != value
            field = value

            if (isValueChange) {
                updateBrandUi()
            }
        }

    internal var expirationDatePlaceholderRes: Int? by Delegates.observable(
        R.string.stripe_expiry_date_hint
    ) { _, _, newValue ->
        expiryTextInputLayout.placeholderText = newValue?.let {
            resources.getString(it)
        }.orEmpty()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setExpirationDatePlaceholderRes(@StringRes resId: Int?) {
        expirationDatePlaceholderRes = resId
    }

    private var showCvcIconInCvcField: Boolean = false

    internal var cardNumberErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(cardNumberTextInputLayout)
    ) { _, _, newValue ->
        cardNumberEditText.setErrorMessageListener(newValue)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setCardNumberErrorListener(listener: StripeEditText.ErrorMessageListener) {
        cardNumberErrorListener = listener
    }

    internal var expirationDateErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(expiryTextInputLayout)
    ) { _, _, newValue ->
        expiryDateEditText.setErrorMessageListener(newValue)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setExpirationDateErrorListener(listener: StripeEditText.ErrorMessageListener) {
        expirationDateErrorListener = listener
    }

    internal var cvcErrorListener: StripeEditText.ErrorMessageListener by Delegates.observable(
        ErrorListener(cvcInputLayout)
    ) { _, _, newValue ->
        cvcEditText.setErrorMessageListener(newValue)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setCvcErrorListener(listener: StripeEditText.ErrorMessageListener) {
        cvcErrorListener = listener
    }

    internal var postalCodeErrorListener: StripeEditText.ErrorMessageListener? by Delegates.observable(
        ErrorListener(postalInputLayout)
    ) { _, _, newValue ->
        postalCodeEditText.setErrorMessageListener(newValue)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setPostalCodeErrorListener(listener: StripeEditText.ErrorMessageListener?) {
        postalCodeErrorListener = listener
    }

    /**
     * A list of preferred networks that should be used to process payments made with a co-branded
     * card if your user hasn't selected a network themselves.
     *
     * The first preferred network that matches any available network will be used. If no preferred
     * network is applicable, Stripe will select the network.
     */
    fun setPreferredNetworks(preferredNetworks: List<CardBrand>) {
        cardBrandView.merchantPreferredNetworks = preferredNetworks
    }

    init {
        orientation = VERTICAL

        textInputLayouts.forEach {
            it.placeholderTextColor = it.editText?.hintTextColors
        }

        // This sets the value of shouldShowPostalCode
        checkAttributeSet(attrs)

        initTextInputLayoutErrorHandlers()

        initFocusChangeListeners()
        initDeleteEmptyListeners()

        cardNumberEditText.completionCallback = {
            expiryDateEditText.requestFocus()
            cardInputListener?.onCardComplete()
        }

        cardNumberEditText.brandChangeCallback = { brand ->
            cardBrandView.brand = brand
            updateBrandUi()
        }

        cardNumberEditText.implicitCardBrandChangeCallback = { brand ->
            // With co-branded cards, a card number can belong to multiple brands. Since we still
            // need do validate based on the card's pan length and expected CVC length, we add this
            // callback to perform the validations, but don't update the current brand.
            updateCvc(brand)
        }

        cardNumberEditText.possibleCardBrandsCallback = { brands ->
            val currentBrand = cardBrandView.brand
            cardBrandView.possibleBrands = brands

            if (currentBrand !in brands) {
                cardBrandView.brand = CardBrand.Unknown
            }

            // We need to use a known card brand to set the correct expected CVC length. Since both
            // brands of a co-branded card have the same CVC length, we can just choose the first one.
            val brandForCvcLength = brands.firstOrNull() ?: CardBrand.Unknown
            updateCvc(brand = brandForCvcLength)
        }

        expiryDateEditText.completionCallback = {
            cvcEditText.requestFocus()
            cardInputListener?.onExpirationComplete()
        }

        cvcEditText.setAfterTextChangedListener { text ->
            val brand = cardNumberEditText.implicitCardBrandForCbc.takeUnless {
                it == CardBrand.Unknown
            } ?: cardNumberEditText.cardBrand

            // TODO
            if (brand.isMaxCvc(text)) {
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

        postalCodeEditText.setAfterTextChangedListener {
            if (isPostalRequired() && postalCodeEditText.hasValidPostal()) {
                cardInputListener?.onPostalCodeComplete()
            }
        }

        adjustViewForPostalCodeAttribute(shouldShowPostalCode)

        cardNumberEditText.updateLengthFilter()

        updateBrandUi()

        allFields.forEach { field ->
            field.doAfterTextChanged {
                shouldShowErrorIcon = false
            }
        }

        cardNumberEditText.isLoadingCallback = {
            cardNumberTextInputLayout.isLoading = it
        }

        postalCodeEditText.config = PostalCodeEditText.Config.Global
        isEnabled = true

        val cardBrandViewPadding = resources.getDimensionPixelSize(
            R.dimen.stripe_card_form_view_text_input_layout_padding_horizontal
        )

        cardBrandView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val inset = view.width + cardBrandViewPadding
            cardNumberEditText.updatePadding(right = inset)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // see https://github.com/stripe/stripe-android/pull/3154
        cvcEditText.hint = null

        lifecycleDelegate.initLifecycle(this)
        doWithCardWidgetViewModel(viewModelStoreOwner) { viewModel ->
            // Merchant could set onBehalfOf before view is attached to window.
            // Check and set if needed.
            if (onBehalfOf != null && viewModel.onBehalfOf != onBehalfOf) {
                viewModel.setOnBehalfOf(onBehalfOf)
            }
            viewModel.isCbcEligible.launchAndCollect { isCbcEligible ->
                cardBrandView.isCbcEligible = isCbcEligible
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleDelegate.destroyLifecycle(this)
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
        cardBrandView.shouldShowErrorIcon = false
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun populate(card: PaymentMethodCreateParams.Card?) {
        card?.let { createParamsCard ->
            // Keep track of currently focused view to return focus to it after populating
            val focusedView = findFocus()
            cardNumberEditText.setText(createParamsCard.number)
            cvcEditText.setText(createParamsCard.cvc)
            expiryDateEditText.setText(createParamsCard.expiryMonth, createParamsCard.expiryYear)
            focusedView?.requestFocus() ?: findFocus()?.clearFocus()
        }
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

    override fun onSaveInstanceState(): Parcelable {
        super.onSaveInstanceState()

        return bundleOf(
            STATE_REMAINING_STATE to super.onSaveInstanceState(),
            STATE_ON_BEHALF_OF to onBehalfOf
        )
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            onBehalfOf = state.getString(STATE_ON_BEHALF_OF)

            super.onRestoreInstanceState(state.getParcelable(STATE_REMAINING_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            updateBrandUi()
        }
    }

    /**
     * Set an optional CVC placeholder text to override defaults, or `null` to use defaults.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun setCvcPlaceholderText(cvcPlaceholderText: String?) {
        customCvcPlaceholderText = cvcPlaceholderText
        updateCvc()
    }

    /**
     * Set an optional CVC field label to override defaults, or `null` to use defaults.
     */
    fun setCvcLabel(cvcLabel: String?) {
        customCvcLabel = cvcLabel
        updateCvc()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // For paymentsheet
    @JvmSynthetic
    fun setCvcIcon(resId: Int?) {
        if (resId != null) {
            updateEndIcon(cvcEditText, resId)
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
            R.string.stripe_expiry_label_short
        } else {
            R.string.stripe_acc_label_expiry_date
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

    private fun checkAttributeSet(attrs: AttributeSet?) {
        context.withStyledAttributes(
            attrs,
            R.styleable.CardElement
        ) {
            shouldShowPostalCode = getBoolean(
                R.styleable.CardElement_shouldShowPostalCode,
                shouldShowPostalCode
            )
            postalCodeRequired = getBoolean(
                R.styleable.CardElement_shouldRequirePostalCode,
                postalCodeRequired
            )
            usZipCodeRequired = getBoolean(
                R.styleable.CardElement_shouldRequireUsZipCode,
                usZipCodeRequired
            )
        }
    }

    private fun flipToCvcIconIfNotFinished() {
        if (brand.isMaxCvc(cvcEditText.fieldText)) {
            return
        }

        cardBrandView.shouldShowErrorIcon = shouldShowErrorIcon
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
        cardNumberEditText.internalFocusChangeListeners.add { _, hasFocus ->
            if (hasFocus) {
                cardInputListener?.onFocusChange(CardInputListener.FocusField.CardNumber)
            }
        }

        expiryDateEditText.internalFocusChangeListeners.add { _, hasFocus ->
            if (hasFocus) {
                cardInputListener?.onFocusChange(CardInputListener.FocusField.ExpiryDate)
            }
        }

        cvcEditText.internalFocusChangeListeners.add { _, hasFocus ->
            if (hasFocus) {
                if (!showCvcIconInCvcField) {
                    flipToCvcIconIfNotFinished()
                }
                cardInputListener?.onFocusChange(CardInputListener.FocusField.Cvc)
            } else {
                cardBrandView.shouldShowErrorIcon = shouldShowErrorIcon
            }
        }

        postalCodeEditText.internalFocusChangeListeners.add { _, hasFocus ->
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
        cardBrandView.shouldShowErrorIcon = shouldShowErrorIcon
    }

    private fun updateCvc(brand: CardBrand = this.brand) {
        cvcEditText.updateBrand(brand, customCvcLabel, customCvcPlaceholderText, cvcInputLayout)
    }

    private fun updateEndIcon(editText: StripeEditText, @DrawableRes iconResourceId: Int) {
        ContextCompat.getDrawable(context, iconResourceId)?.let { icon ->
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                icon,
                null
            )
        }
    }

    private companion object {
        private const val CARD_MULTILINE_TOKEN = "CardMultilineView"
        private const val STATE_REMAINING_STATE = "state_remaining_state"
        private const val STATE_ON_BEHALF_OF = "state_on_behalf_of"
    }
}
