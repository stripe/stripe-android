package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Layout
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.doAfterTextChanged
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.Cvc
import com.stripe.android.databinding.CardInputWidgetBinding
import com.stripe.android.model.Address
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ExpirationDate
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlin.properties.Delegates

/**
 * A single-line card input widget.
 *
 * To enable 19-digit card support, [PaymentConfiguration.init] must be called before
 * [CardInputWidget] is instantiated.
 *
 * The individual `EditText` views of this widget can be styled by defining a style
 * `Stripe.CardInputWidget.EditText` that extends `Stripe.Base.CardInputWidget.EditText`.
 */
class CardInputWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val viewBinding = CardInputWidgetBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    private val containerLayout = viewBinding.container

    @JvmSynthetic
    internal val cardBrandView = viewBinding.cardBrandView

    private val cardNumberTextInputLayout = viewBinding.cardNumberTextInputLayout
    private val expiryDateTextInputLayout = viewBinding.expiryDateTextInputLayout
    private val cvcNumberTextInputLayout = viewBinding.cvcTextInputLayout
    internal val postalCodeTextInputLayout = viewBinding.postalCodeTextInputLayout

    @JvmSynthetic
    internal val cardNumberEditText = viewBinding.cardNumberEditText

    @JvmSynthetic
    internal val expiryDateEditText = viewBinding.expiryDateEditText

    @JvmSynthetic
    internal val cvcEditText = viewBinding.cvcEditText

    @JvmSynthetic
    internal val postalCodeEditText = viewBinding.postalCodeEditText

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
                    cardNumberEditText.validatedCardNumber == null
                },
                CardValidCallback.Fields.Expiry.takeIf {
                    expiryDateEditText.validatedDate == null
                },
                CardValidCallback.Fields.Cvc.takeIf {
                    this.cvc == null
                }
            ).toSet()
        }

    @VisibleForTesting
    internal var shouldShowErrorIcon = false
        private set(value) {
            cardBrandView.shouldShowErrorIcon = value
            field = value
        }

    /**
     * If `true`, the full card number is being shown. This is the initial view.
     * If `false`, the peek card number is being shown.
     */
    @JvmSynthetic
    internal var isShowingFullCard = true

    private var isViewInitialized: Boolean = false

    @JvmSynthetic
    internal var layoutWidthCalculator: LayoutWidthCalculator = DefaultLayoutWidthCalculator()

    internal val placement = CardInputWidgetPlacement()

    private val postalCodeValue: String?
        get() {
            return if (postalCodeEnabled) {
                postalCodeEditText.postalCode
            } else {
                null
            }
        }

    private val cvc: Cvc.Validated?
        get() {
            return cvcEditText.cvc
        }

    private val brand: CardBrand
        get() {
            return cardNumberEditText.cardBrand
        }

    @VisibleForTesting
    @JvmSynthetic
    internal val requiredFields: List<StripeEditText>
    private val allFields: List<StripeEditText>

    /**
     * The [StripeEditText] fields that are currently enabled and active in the UI.
     */
    @VisibleForTesting
    internal val currentFields: List<StripeEditText>
        @JvmSynthetic
        get() {
            return requiredFields
                .plus(postalCodeEditText.takeIf { postalCodeEnabled })
                .filterNotNull()
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

    private val billingDetails: PaymentMethod.BillingDetails?
        get() {
            return postalCodeValue?.let {
                PaymentMethod.BillingDetails(
                    address = Address(
                        postalCode = it
                    )
                )
            }
        }

    /**
     * A [PaymentMethodCreateParams] representing the card details and postal code if all fields
     * are valid; otherwise `null`
     */
    override val paymentMethodCreateParams: PaymentMethodCreateParams?
        get() {
            return paymentMethodCard?.let { card ->
                PaymentMethodCreateParams.create(card, billingDetails)
            }
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
            val cardNumber = cardNumberEditText.validatedCardNumber
            val expirationDate = expiryDateEditText.validatedDate
            val cvc = this.cvc

            cardNumberEditText.shouldShowError = cardNumber == null
            expiryDateEditText.shouldShowError = expirationDate == null
            cvcEditText.shouldShowError = cvc == null
            postalCodeEditText.shouldShowError =
                (postalCodeRequired || usZipCodeRequired) &&
                postalCodeEditText.postalCode.isNullOrBlank()

            // Announce error messages for accessibility
            currentFields
                .filter { it.shouldShowError }
                .forEach { editText ->
                    editText.errorMessage?.let { errorMessage ->
                        editText.announceForAccessibility(errorMessage)
                    }
                }

            when {
                cardNumber == null -> {
                    cardNumberEditText.requestFocus()
                }
                expirationDate == null -> {
                    expiryDateEditText.requestFocus()
                }
                cvc == null -> {
                    cvcEditText.requestFocus()
                }
                postalCodeEditText.shouldShowError -> {
                    postalCodeEditText.requestFocus()
                }
                else -> {
                    shouldShowErrorIcon = false
                    return CardParams(
                        setOf(LOGGING_TOKEN),
                        number = cardNumber.value,
                        expMonth = expirationDate.month,
                        expYear = expirationDate.year,
                        cvc = cvc.value,
                        address = Address.Builder()
                            .setPostalCode(postalCodeValue.takeUnless { it.isNullOrBlank() })
                            .build()
                    )
                }
            }

            shouldShowErrorIcon = true

            return null
        }

    /**
     * A [Card.Builder] representing the card details and postal code if all fields are valid;
     * otherwise `null`
     */
    @Deprecated("Use cardParams", ReplaceWith("cardParams"))
    override val cardBuilder: Card.Builder?
        get() {
            val cardNumber = cardNumberEditText.validatedCardNumber
            val expirationDate = expiryDateEditText.validatedDate
            val cvc = this.cvc

            cardNumberEditText.shouldShowError = cardNumber == null
            expiryDateEditText.shouldShowError = expirationDate == null
            cvcEditText.shouldShowError = cvc == null
            postalCodeEditText.shouldShowError =
                (postalCodeRequired || usZipCodeRequired) &&
                postalCodeEditText.postalCode.isNullOrBlank()

            // Announce error messages for accessibility
            currentFields
                .filter { it.shouldShowError }
                .forEach { editText ->
                    editText.errorMessage?.let { errorMessage ->
                        editText.announceForAccessibility(errorMessage)
                    }
                }

            when {
                cardNumber == null -> {
                    cardNumberEditText.requestFocus()
                }
                expirationDate == null -> {
                    expiryDateEditText.requestFocus()
                }
                cvc == null -> {
                    cvcEditText.requestFocus()
                }
                postalCodeEditText.shouldShowError -> {
                    postalCodeEditText.requestFocus()
                }
                else -> {
                    shouldShowErrorIcon = false
                    return Card.Builder(
                        number = cardNumber.value,
                        expMonth = expirationDate.month,
                        expYear = expirationDate.year,
                        cvc = cvc.value
                    )
                        .addressZip(postalCodeValue)
                        .loggingTokens(setOf(LOGGING_TOKEN))
                }
            }

            shouldShowErrorIcon = true

            return null
        }

    private val frameWidth: Int
        get() = frameWidthSupplier()

    @JvmSynthetic
    internal var frameWidthSupplier: () -> Int

    /**
     * The postal code field is enabled by default. Disabling the postal code field may impact
     * auth success rates, so it is discouraged to disable it unless you are collecting the postal
     * code outside of this form.
     */
    var postalCodeEnabled: Boolean by Delegates.observable(
        CardWidget.DEFAULT_POSTAL_CODE_ENABLED
    ) { _, _, isEnabled ->
        if (isEnabled) {
            postalCodeEditText.isEnabled = true
            postalCodeTextInputLayout.visibility = View.VISIBLE

            cvcEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        } else {
            postalCodeEditText.isEnabled = false
            postalCodeTextInputLayout.visibility = View.GONE

            cvcEditText.imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

    /**
     * If [postalCodeEnabled] is true and [postalCodeRequired] is true, then postal code is a
     * required field.
     *
     * If [postalCodeEnabled] is false, this value is ignored.
     *
     * Note that some countries do not have postal codes, so requiring postal code will prevent
     * those users from submitting this form successfully.
     */
    var postalCodeRequired: Boolean = CardWidget.DEFAULT_POSTAL_CODE_REQUIRED

    /**
     * If [postalCodeEnabled] is true and [usZipCodeRequired] is true, then postal code is a
     * required field and must be a 5-digit US zip code.
     *
     * If [postalCodeEnabled] is false, this value is ignored.
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

    private val frameStart: Int
        get() {
            val isLtr = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
            return if (isLtr) {
                containerLayout.left
            } else {
                containerLayout.right
            }
        }

    init {
        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (id == View.NO_ID) {
            id = DEFAULT_READER_ID
        }

        orientation = HORIZONTAL
        minimumWidth = resources.getDimensionPixelSize(R.dimen.stripe_card_widget_min_width)

        frameWidthSupplier = { containerLayout.width }

        requiredFields = listOf(
            cardNumberEditText,
            cvcEditText,
            expiryDateEditText
        )
        allFields = requiredFields.plus(postalCodeEditText)

        initView(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        postalCodeEditText.config = PostalCodeEditText.Config.Global
    }

    override fun setCardValidCallback(callback: CardValidCallback?) {
        this.cardValidCallback = callback
        requiredFields.forEach { it.removeTextChangedListener(cardValidTextWatcher) }

        // only add the TextWatcher if it will be used
        if (callback != null) {
            requiredFields.forEach { it.addTextChangedListener(cardValidTextWatcher) }
        }

        // call immediately after setting
        cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
    }

    /**
     * Set a [CardInputListener] to be notified of card input events.
     *
     * @param listener the listener
     */
    override fun setCardInputListener(listener: CardInputListener?) {
        cardInputListener = listener
    }

    /**
     * Set the card number. Method does not change text field focus.
     *
     * @param cardNumber card number to be set
     */
    override fun setCardNumber(cardNumber: String?) {
        cardNumberEditText.setText(cardNumber)
        this.isShowingFullCard = !cardNumberEditText.isCardNumberValid
    }

    override fun setCardHint(cardHint: String) {
        cardNumberEditText.hint = cardHint
    }

    /**
     * Set the expiration date. Method invokes completion listener and changes focus
     * to the CVC field if a valid date is entered.
     *
     * Note that while a four-digit and two-digit year will both work, information
     * beyond the tens digit of a year will be truncated. Logic elsewhere in the SDK
     * makes assumptions about what century is implied by various two-digit years, and
     * will override any information provided here.
     *
     * @param month a month of the year, represented as a number between 1 and 12
     * @param year a year number, either in two-digit form or four-digit form
     */
    override fun setExpiryDate(
        @IntRange(from = 1, to = 12) month: Int,
        @IntRange(from = 0, to = 9999) year: Int
    ) {
        expiryDateEditText.setText(
            ExpirationDate.Unvalidated(month, year).getDisplayString()
        )
    }

    /**
     * Set the CVC value for the card. Note that the maximum length is assumed to
     * be 3, unless the brand of the card has already been set (by setting the card number).
     *
     * @param cvcCode the CVC value to be set
     */
    override fun setCvcCode(cvcCode: String?) {
        cvcEditText.setText(cvcCode)
    }

    @JvmSynthetic
    internal fun setPostalCode(postalCode: String?) {
        postalCodeEditText.setText(postalCode)
    }

    /**
     * Clear all text fields in the CardInputWidget.
     */
    override fun clear() {
        if (currentFields.any { it.hasFocus() } || this.hasFocus()) {
            cardNumberEditText.requestFocus()
        }

        currentFields.forEach { it.setText("") }
    }

    /**
     * Enable or disable text fields
     *
     * @param isEnabled boolean indicating whether fields should be enabled
     */
    override fun setEnabled(isEnabled: Boolean) {
        currentFields.forEach { it.isEnabled = isEnabled }
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

    /**
     * Override of [View.isEnabled] that returns `true` only
     * if all three sub-controls are enabled.
     *
     * @return `true` if the card number field, expiry field, and cvc field are enabled,
     * `false` otherwise
     */
    override fun isEnabled(): Boolean {
        return requiredFields.all { it.isEnabled }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action != MotionEvent.ACTION_DOWN) {
            return super.onInterceptTouchEvent(ev)
        }

        return getFocusField(
            ev.x.toInt(),
            frameStart
        )?.let { field ->
            when (field) {
                Field.Number -> cardNumberEditText
                Field.Expiry -> expiryDateEditText
                Field.Cvc -> cvcEditText
                Field.PostalCode -> postalCodeEditText
            }.requestFocus()
            true
        } ?: super.onInterceptTouchEvent(ev)
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            STATE_SUPER_STATE to super.onSaveInstanceState(),
            STATE_CARD_VIEWED to isShowingFullCard,
            STATE_POSTAL_CODE_ENABLED to postalCodeEnabled
        )
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            postalCodeEnabled = state.getBoolean(STATE_POSTAL_CODE_ENABLED, true)
            isShowingFullCard = state.getBoolean(STATE_CARD_VIEWED, true)
            updateSpaceSizes(isShowingFullCard)
            placement.totalLengthInPixels = frameWidth
            val cardStartMargin: Int
            val dateStartMargin: Int
            val cvcStartMargin: Int
            val postalCodeStartMargin: Int
            if (isShowingFullCard) {
                cardStartMargin = 0
                dateStartMargin = placement.getDateStartMargin(isFullCard = true)
                cvcStartMargin = placement.getCvcStartMargin(isFullCard = true)
                postalCodeStartMargin = placement.getPostalCodeStartMargin(isFullCard = true)
            } else {
                cardStartMargin = -1 * placement.hiddenCardWidth
                dateStartMargin = placement.getDateStartMargin(isFullCard = false)
                cvcStartMargin = placement.getCvcStartMargin(isFullCard = false)
                postalCodeStartMargin = if (postalCodeEnabled) {
                    placement.getPostalCodeStartMargin(isFullCard = false)
                } else {
                    placement.totalLengthInPixels
                }
            }

            updateFieldLayout(
                view = cardNumberTextInputLayout,
                width = placement.cardWidth,
                marginStart = cardStartMargin
            )
            updateFieldLayout(
                view = expiryDateTextInputLayout,
                width = placement.dateWidth,
                marginStart = dateStartMargin
            )
            updateFieldLayout(
                view = cvcNumberTextInputLayout,
                width = placement.cvcWidth,
                marginStart = cvcStartMargin
            )
            updateFieldLayout(
                view = postalCodeTextInputLayout,
                width = placement.postalCodeWidth,
                marginStart = postalCodeStartMargin
            )

            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun getFocusField(
        touchX: Int,
        frameStart: Int
    ) = placement.getFocusField(
        touchX,
        frameStart,
        isShowingFullCard,
        postalCodeEnabled
    )

    @VisibleForTesting
    internal fun updateSpaceSizes(
        isShowingFullCard: Boolean,
        frameWidth: Int = this.frameWidth,
        frameStart: Int = this.frameStart
    ) {
        if (frameWidth == 0) {
            // This is an invalid view state.
            return
        }

        placement.cardWidth = getDesiredWidthInPixels(
            FULL_SIZING_CARD_TEXT,
            cardNumberEditText
        )

        placement.dateWidth = getDesiredWidthInPixels(
            FULL_SIZING_DATE_TEXT,
            expiryDateEditText
        )

        placement.hiddenCardWidth = getDesiredWidthInPixels(
            hiddenCardText,
            cardNumberEditText
        )

        placement.cvcWidth = getDesiredWidthInPixels(
            cvcPlaceHolder,
            cvcEditText
        )

        placement.postalCodeWidth = getDesiredWidthInPixels(
            FULL_SIZING_POSTAL_CODE_TEXT,
            postalCodeEditText
        )

        placement.peekCardWidth = getDesiredWidthInPixels(
            peekCardText,
            cardNumberEditText
        )

        placement.updateSpacing(isShowingFullCard, postalCodeEnabled, frameStart, frameWidth)
    }

    private fun updateFieldLayout(
        view: View,
        width: Int,
        marginStart: Int
    ) {
        view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
            this.width = width
            this.marginStart = marginStart
        }
    }

    private fun getDesiredWidthInPixels(text: String, editText: StripeEditText): Int {
        return layoutWidthCalculator.calculate(text, editText.paint)
    }

    private fun initView(attrs: AttributeSet?) {
        attrs?.let { applyAttributes(it) }

        ViewCompat.setAccessibilityDelegate(
            cardNumberEditText,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)

                    // Avoid reading out "1234 1234 1234 1234"
                    info.hintText = null
                }
            }
        )

        isShowingFullCard = true

        @ColorInt var errorColorInt = cardNumberEditText.defaultErrorColorInt
        cardBrandView.tintColorInt = cardNumberEditText.hintTextColors.defaultColor
        var cardHintText: String? = null
        val shouldRequestFocus: Boolean
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CardInputView,
                0,
                0
            )

            try {
                cardBrandView.tintColorInt = a.getColor(
                    R.styleable.CardInputView_cardTint,
                    cardBrandView.tintColorInt
                )
                errorColorInt = a.getColor(R.styleable.CardInputView_cardTextErrorColor, errorColorInt)
                cardHintText = a.getString(R.styleable.CardInputView_cardHintText)
                shouldRequestFocus = a.getBoolean(R.styleable.CardInputView_android_focusedByDefault, true)
            } finally {
                a.recycle()
            }
        } else {
            shouldRequestFocus = true
        }

        cardHintText?.let {
            cardNumberEditText.hint = it
        }

        currentFields.forEach { it.setErrorColor(errorColorInt) }

        cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollStart()
                cardInputListener?.onFocusChange(CardInputListener.FocusField.CardNumber)
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollEnd()
                cardInputListener?.onFocusChange(CardInputListener.FocusField.ExpiryDate)
            }
        }

        expiryDateEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(cardNumberEditText))
        cvcEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(expiryDateEditText))
        postalCodeEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(cvcEditText))

        cvcEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            cardBrandView.shouldShowCvc = hasFocus

            if (hasFocus) {
                scrollEnd()
                cardInputListener?.onFocusChange(CardInputListener.FocusField.Cvc)
            }
        }

        cvcEditText.setAfterTextChangedListener { text ->
            if (brand.isMaxCvc(text)) {
                cardInputListener?.onCvcComplete()
            }
        }

        cardNumberEditText.completionCallback = {
            scrollEnd()
            cardInputListener?.onCardComplete()
        }

        cardNumberEditText.brandChangeCallback = { brand ->
            cardBrandView.brand = brand
            hiddenCardText = createHiddenCardText(cardNumberEditText.panLength)
            cvcEditText.updateBrand(brand)
        }

        expiryDateEditText.completionCallback = {
            cvcEditText.requestFocus()
            cardInputListener?.onExpirationComplete()
        }

        cvcEditText.completionCallback = {
            if (postalCodeEnabled) {
                postalCodeEditText.requestFocus()
            }
        }

        allFields.forEach { field ->
            field.doAfterTextChanged {
                shouldShowErrorIcon = false
            }
        }

        if (shouldRequestFocus) {
            cardNumberEditText.requestFocus()
        }

        cardNumberEditText.isLoadingCallback = {
            cardBrandView.isLoading = it
        }
    }

    /**
     * @return a [String] that is the length of a full formatted PAN for the given PAN length,
     * without the last group of digits. This is used for measuring the rendered width of the
     * hidden portion (i.e. when the card number is "peeking") and does not have to be a valid
     * card number.
     *
     * e.g. if [panLength] is `16`, this will generate `"0000 0000 0000 "` (including the
     * trailing space).
     *
     * This should only be called when [brand] changes.
     */
    @VisibleForTesting
    internal fun createHiddenCardText(
        panLength: Int
    ): String {
        val formattedNumber = CardNumber.Unvalidated(
            "0".repeat(panLength)
        ).getFormatted(panLength)

        return formattedNumber.take(
            formattedNumber.lastIndexOf(' ') + 1
        )
    }

    private fun applyAttributes(attrs: AttributeSet) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CardElement,
            0,
            0
        )

        try {
            postalCodeEnabled = typedArray.getBoolean(
                R.styleable.CardElement_shouldShowPostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_ENABLED
            )
            postalCodeRequired = typedArray.getBoolean(
                R.styleable.CardElement_shouldRequirePostalCode,
                CardWidget.DEFAULT_POSTAL_CODE_REQUIRED
            )
            usZipCodeRequired = typedArray.getBoolean(
                R.styleable.CardElement_shouldRequireUsZipCode,
                CardWidget.DEFAULT_US_ZIP_CODE_REQUIRED
            )
        } finally {
            typedArray.recycle()
        }
    }

    // reveal the full card number field
    private fun scrollStart() {
        if (isShowingFullCard || !isViewInitialized) {
            return
        }

        val dateStartPosition = placement.getDateStartMargin(isFullCard = false)
        val cvcStartPosition = placement.getCvcStartMargin(isFullCard = false)
        val postalCodeStartPosition = placement.getPostalCodeStartMargin(isFullCard = false)

        updateSpaceSizes(isShowingFullCard = true)

        val slideCardStartAnimation = CardNumberSlideStartAnimation(
            view = cardNumberTextInputLayout
        )

        val dateDestination = placement.getDateStartMargin(isFullCard = true)
        val slideDateStartAnimation = ExpiryDateSlideStartAnimation(
            view = expiryDateTextInputLayout,
            startPosition = dateStartPosition,
            destination = dateDestination
        )

        val cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition)
        val slideCvcStartAnimation = CvcSlideStartAnimation(
            view = cvcNumberTextInputLayout,
            startPosition = cvcStartPosition,
            destination = cvcDestination,
            newWidth = placement.cvcWidth
        )

        val postalCodeDestination = postalCodeStartPosition + (cvcDestination - cvcStartPosition)
        val slidePostalCodeStartAnimation = if (postalCodeEnabled) {
            PostalCodeSlideStartAnimation(
                view = postalCodeTextInputLayout,
                startPosition = postalCodeStartPosition,
                destination = postalCodeDestination,
                newWidth = placement.postalCodeWidth
            )
        } else {
            null
        }

        startSlideAnimation(
            listOfNotNull(
                slideCardStartAnimation,
                slideDateStartAnimation,
                slideCvcStartAnimation,
                slidePostalCodeStartAnimation
            )
        )

        isShowingFullCard = true
    }

    // reveal the secondary fields
    private fun scrollEnd() {
        if (!isShowingFullCard || !isViewInitialized) {
            return
        }

        val dateStartMargin = placement.getDateStartMargin(isFullCard = true)

        updateSpaceSizes(isShowingFullCard = false)

        val slideCardEndAnimation = CardNumberSlideEndAnimation(
            view = cardNumberTextInputLayout,
            hiddenCardWidth = placement.hiddenCardWidth,
            focusOnEndView = expiryDateEditText
        )

        val dateDestination = placement.getDateStartMargin(isFullCard = false)
        val slideDateEndAnimation = ExpiryDateSlideEndAnimation(
            view = expiryDateTextInputLayout,
            startMargin = dateStartMargin,
            destination = dateDestination
        )

        val cvcDestination = placement.getCvcStartMargin(isFullCard = false)
        val cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination)
        val slideCvcEndAnimation = CvcSlideEndAnimation(
            view = cvcNumberTextInputLayout,
            startMargin = cvcStartMargin,
            destination = cvcDestination,
            newWidth = placement.cvcWidth
        )

        val postalCodeDestination = placement.getPostalCodeStartMargin(isFullCard = false)
        val postalCodeStartMargin = postalCodeDestination + (cvcStartMargin - cvcDestination)
        val slidePostalCodeEndAnimation = if (postalCodeEnabled) {
            PostalCodeSlideEndAnimation(
                view = postalCodeTextInputLayout,
                startMargin = postalCodeStartMargin,
                destination = postalCodeDestination,
                newWidth = placement.postalCodeWidth
            )
        } else {
            null
        }

        startSlideAnimation(
            listOfNotNull(
                slideCardEndAnimation,
                slideDateEndAnimation,
                slideCvcEndAnimation,
                slidePostalCodeEndAnimation
            )
        )

        isShowingFullCard = false
    }

    private fun startSlideAnimation(animations: List<Animation>) {
        val animationSet = AnimationSet(true).apply {
            animations.forEach { addAnimation(it) }
        }
        containerLayout.startAnimation(animationSet)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!isViewInitialized && width != 0) {
            isViewInitialized = true
            placement.totalLengthInPixels = frameWidth

            updateSpaceSizes(isShowingFullCard)

            updateFieldLayout(
                view = cardNumberTextInputLayout,
                width = placement.cardWidth,
                marginStart = if (isShowingFullCard) {
                    0
                } else {
                    -1 * placement.hiddenCardWidth
                }
            )

            updateFieldLayout(
                view = expiryDateTextInputLayout,
                width = placement.dateWidth,
                marginStart = placement.getDateStartMargin(isShowingFullCard)
            )

            updateFieldLayout(
                view = cvcNumberTextInputLayout,
                width = placement.cvcWidth,
                marginStart = placement.getCvcStartMargin(isShowingFullCard)
            )

            updateFieldLayout(
                view = postalCodeTextInputLayout,
                width = placement.postalCodeWidth,
                marginStart = placement.getPostalCodeStartMargin(isShowingFullCard)
            )
        }
    }

    private var hiddenCardText: String = createHiddenCardText(cardNumberEditText.panLength)

    private val cvcPlaceHolder: String
        get() {
            return if (CardBrand.AmericanExpress == brand) {
                CVC_PLACEHOLDER_AMEX
            } else {
                CVC_PLACEHOLDER_COMMON
            }
        }

    private val peekCardText: String
        get() {
            return when (cardNumberEditText.panLength) {
                19 -> 3
                15 -> 5
                14 -> 2
                else -> 4
            }.let { peekSize ->
                "0".repeat(peekSize)
            }
        }

    private abstract class CardFieldAnimation : Animation() {
        init {
            duration = ANIMATION_LENGTH
        }

        private companion object {
            private const val ANIMATION_LENGTH = 150L
        }
    }

    private class CardNumberSlideStartAnimation(
        private val view: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(
                object : AnimationEndListener() {
                    override fun onAnimationEnd(animation: Animation) {
                        view.requestFocus()
                    }
                }
            )
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart = (marginStart * (1 - interpolatedTime)).toInt()
            }
        }
    }

    private class ExpiryDateSlideStartAnimation(
        private val view: View,
        private val startPosition: Int,
        private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
            }
        }
    }

    private class CvcSlideStartAnimation(
        private val view: View,
        private val startPosition: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.marginStart = (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    private class PostalCodeSlideStartAnimation(
        private val view: View,
        private val startPosition: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.marginStart =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    private class CardNumberSlideEndAnimation(
        private val view: View,
        private val hiddenCardWidth: Int,
        private val focusOnEndView: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(
                object : AnimationEndListener() {
                    override fun onAnimationEnd(animation: Animation) {
                        focusOnEndView.requestFocus()
                    }
                }
            )
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart = (-1f * hiddenCardWidth.toFloat() * interpolatedTime).toInt()
            }
        }
    }

    private class ExpiryDateSlideEndAnimation(
        private val view: View,
        private val startMargin: Int,
        private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
            }
        }
    }

    private class CvcSlideEndAnimation(
        private val view: View,
        private val startMargin: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                marginStart =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                marginEnd = 0
                width = newWidth
            }
        }
    }

    private class PostalCodeSlideEndAnimation(
        private val view: View,
        private val startMargin: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.marginStart =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                this.marginEnd = 0
                this.width = newWidth
            }
        }
    }

    /**
     * A convenience class for when we only want to listen for when an animation ends.
     */
    private abstract class AnimationEndListener : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            // Intentional No-op
        }

        override fun onAnimationRepeat(animation: Animation) {
            // Intentional No-op
        }
    }

    internal fun interface LayoutWidthCalculator {
        fun calculate(text: String, paint: TextPaint): Int
    }

    internal class DefaultLayoutWidthCalculator : LayoutWidthCalculator {
        override fun calculate(text: String, paint: TextPaint): Int {
            return Layout.getDesiredWidth(text, paint).toInt()
        }
    }

    internal enum class Field {
        Number,
        Expiry,
        Cvc,
        PostalCode
    }

    internal companion object {
        internal const val LOGGING_TOKEN = "CardInputView"

        private const val CVC_PLACEHOLDER_COMMON = "CVC"
        private const val CVC_PLACEHOLDER_AMEX = "2345"

        private const val FULL_SIZING_CARD_TEXT = "4242 4242 4242 4242 424"
        private const val FULL_SIZING_DATE_TEXT = "MM/MM"
        private const val FULL_SIZING_POSTAL_CODE_TEXT = "1234567890"

        private const val STATE_CARD_VIEWED = "state_card_viewed"
        private const val STATE_SUPER_STATE = "state_super_state"
        private const val STATE_POSTAL_CODE_ENABLED = "state_postal_code_enabled"

        // This value is used to ensure that onSaveInstanceState is called
        // in the event that the user doesn't give this control an ID.
        @IdRes
        private val DEFAULT_READER_ID = R.id.stripe_default_reader_id

        /**
         * Determines whether or not the icon should show the card brand instead of the
         * CVC helper icon.
         *
         * @param brand the [CardBrand] of the card number
         * @param cvcHasFocus `true` if the CVC entry field has focus, `false` otherwise
         * @param cvcText the current content of [cvcEditText]
         *
         * @return `true` if we should show the brand of the card, or `false` if we
         * should show the CVC helper icon instead
         */
        @VisibleForTesting
        internal fun shouldIconShowBrand(
            brand: CardBrand,
            cvcHasFocus: Boolean,
            cvcText: String?
        ): Boolean {
            return !cvcHasFocus || brand.isMaxCvc(cvcText)
        }
    }
}
