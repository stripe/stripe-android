package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter
import android.text.Layout
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stripe.android.R
import com.stripe.android.model.Card
import com.stripe.android.model.Card.CardBrand
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CARD
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_CVC
import com.stripe.android.view.CardInputListener.FocusField.Companion.FOCUS_EXPIRY

/**
 * A card input widget that handles all animation on its own.
 */
class CardInputWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), CardWidget {
    private val cardIconImageView: ImageView
    private val frameLayout: FrameLayout
    private val cardNumberEditText: CardNumberEditText
    private val cvcNumberEditText: StripeEditText
    private val expiryDateEditText: ExpiryDateEditText

    private var cardInputListener: CardInputListener? = null
    private var cardNumberIsViewed = true

    @ColorInt
    private var tintColorInt: Int = 0

    private var isAmEx: Boolean = false
    private var initFlag: Boolean = false

    private var totalLengthInPixels: Int = 0

    private var dimensionOverrides: DimensionOverrideSettings? = null
    internal val placementParameters: PlacementParameters = PlacementParameters()

    /**
     * Gets a [PaymentMethodCreateParams.Card] object from the user input, if all fields are
     * valid. If not, returns `null`.
     *
     * @return a valid [PaymentMethodCreateParams.Card] object based on user input, or
     * `null` if any field is invalid
     */
    override // CVC/CVV is the only field not validated by the entry control itself, so we check here.
    val paymentMethodCard: PaymentMethodCreateParams.Card?
        get() {
            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = expiryDateEditText.validDateFields
            val cvcValue = cvcNumberEditText.text.toString().trim()
            return if (cardNumber == null || cardDate == null || !isCvcLengthValid(cvcValue)) {
                null
            } else {
                PaymentMethodCreateParams.Card.Builder()
                    .setNumber(cardNumber)
                    .setCvc(cvcValue)
                    .setExpiryMonth(cardDate.first)
                    .setExpiryYear(cardDate.second)
                    .build()
            }
        }

    override val paymentMethodCreateParams: PaymentMethodCreateParams?
        get() {
            val card = paymentMethodCard ?: return null

            return PaymentMethodCreateParams.create(card)
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
            val builder = cardBuilder
            return builder?.build()
        }

    override // CVC/CVV is the only field not validated by the entry control itself, so we check here.
    val cardBuilder: Card.Builder?
        get() {
            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = expiryDateEditText.validDateFields
            return if (cardNumber == null || cardDate == null) {
                null
            } else {
                val cvcValue = cvcNumberEditText.text.toString().trim()
                if (!isCvcLengthValid(cvcValue)) {
                    null
                } else {
                    Card.Builder(cardNumber, cardDate.first, cardDate.second, cvcValue)
                        .loggingTokens(listOf(LOGGING_TOKEN))
                }
            }
        }

    private val frameWidth: Int
        get() = dimensionOverrides?.frameWidth ?: frameLayout.width

    init {
        View.inflate(getContext(), R.layout.card_input_widget, this)

        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (id == View.NO_ID) {
            id = DEFAULT_READER_ID
        }

        orientation = HORIZONTAL
        minimumWidth = resources.getDimensionPixelSize(R.dimen.stripe_card_widget_min_width)

        cardIconImageView = findViewById(R.id.iv_card_icon)
        cardNumberEditText = findViewById(R.id.et_card_number)
        expiryDateEditText = findViewById(R.id.et_expiry_date)
        cvcNumberEditText = findViewById(R.id.et_cvc_number)
        frameLayout = findViewById(R.id.frame_container)

        initView(attrs)
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
        setCardNumberIsViewed(!cardNumberEditText.isCardNumberValid)
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
        expiryDateEditText.setText(DateUtils.createDateStringFromIntegerInput(month, year))
    }

    /**
     * Set the CVC value for the card. Note that the maximum length is assumed to
     * be 3, unless the brand of the card has already been set (by setting the card number).
     *
     * @param cvcCode the CVC value to be set
     */
    override fun setCvcCode(cvcCode: String?) {
        cvcNumberEditText.setText(cvcCode)
    }

    /**
     * Clear all text fields in the CardInputWidget.
     */
    override fun clear() {
        if (cardNumberEditText.hasFocus() ||
            expiryDateEditText.hasFocus() ||
            cvcNumberEditText.hasFocus() ||
            this.hasFocus()) {
            cardNumberEditText.requestFocus()
        }
        cvcNumberEditText.setText("")
        expiryDateEditText.setText("")
        cardNumberEditText.setText("")
    }

    /**
     * Enable or disable text fields
     *
     * @param isEnabled boolean indicating whether fields should be enabled
     */
    override fun setEnabled(isEnabled: Boolean) {
        cardNumberEditText.isEnabled = isEnabled
        expiryDateEditText.isEnabled = isEnabled
        cvcNumberEditText.isEnabled = isEnabled
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
        cvcNumberEditText.addTextChangedListener(cvcNumberTextWatcher)
    }

    /**
     * Override of [View.isEnabled] that returns `true` only
     * if all three sub-controls are enabled.
     *
     * @return `true` if the card number field, expiry field, and cvc field are enabled,
     * `false` otherwise
     */
    override fun isEnabled(): Boolean {
        return cardNumberEditText.isEnabled &&
            expiryDateEditText.isEnabled &&
            cvcNumberEditText.isEnabled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action != MotionEvent.ACTION_DOWN) {
            return super.onInterceptTouchEvent(ev)
        }

        val focusEditText = getFocusRequestOnTouch(ev.x.toInt())
        if (focusEditText != null) {
            focusEditText.requestFocus()
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(EXTRA_SUPER_STATE, super.onSaveInstanceState())
        bundle.putBoolean(EXTRA_CARD_VIEWED, cardNumberIsViewed)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            cardNumberIsViewed = state.getBoolean(EXTRA_CARD_VIEWED, true)
            updateSpaceSizes(cardNumberIsViewed)
            totalLengthInPixels = frameWidth
            val cardMargin: Int
            val dateMargin: Int
            val cvcMargin: Int
            if (cardNumberIsViewed) {
                cardMargin = 0
                dateMargin = placementParameters.cardWidth + placementParameters.cardDateSeparation
                cvcMargin = totalLengthInPixels
            } else {
                cardMargin = -1 * placementParameters.hiddenCardWidth
                dateMargin = placementParameters.peekCardWidth + placementParameters.cardDateSeparation
                cvcMargin = (dateMargin +
                    placementParameters.dateWidth +
                    placementParameters.dateCvcSeparation)
            }

            setLayoutValues(placementParameters.cardWidth, cardMargin, cardNumberEditText)
            setLayoutValues(placementParameters.dateWidth, dateMargin, expiryDateEditText)
            setLayoutValues(placementParameters.cvcWidth, cvcMargin, cvcNumberEditText)

            super.onRestoreInstanceState(state.getParcelable(EXTRA_SUPER_STATE))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Checks on the horizontal position of a touch event to see if
     * that event needs to be associated with one of the controls even
     * without having actually touched it. This essentially gives a larger
     * touch surface to the controls. We return `null` if the user touches
     * actually inside the widget because no interception is necessary - the touch will
     * naturally give focus to that control, and we don't want to interfere with what
     * Android will naturally do in response to that touch.
     *
     * @param touchX distance in pixels from the left side of this control
     * @return a [StripeEditText] that needs to request focus, or `null`
     * if no such request is necessary.
     */
    @VisibleForTesting
    internal fun getFocusRequestOnTouch(touchX: Int): StripeEditText? {
        val frameStart = frameLayout.left

        return if (cardNumberIsViewed) {
            // Then our view is
            // |CARDVIEW||space||DATEVIEW|

            when {
                touchX < frameStart + placementParameters.cardWidth -> // Then the card edit view will already handle this touch.
                    null
                touchX < placementParameters.cardTouchBufferLimit -> // Then we want to act like this was a touch on the card view
                    cardNumberEditText
                touchX < placementParameters.dateStartPosition -> // Then we act like this was a touch on the date editor.
                    expiryDateEditText
                else -> // Then the date editor will already handle this touch.
                    null
            }
        } else {
            // Our view is
            // |PEEK||space||DATE||space||CVC|
            when {
                touchX < frameStart + placementParameters.peekCardWidth -> // This was a touch on the card number editor, so we don't need to handle it.
                    null
                touchX < placementParameters.cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                    cardNumberEditText
                touchX < placementParameters.dateStartPosition -> // Then we need to act like this was a touch on the date editor
                    expiryDateEditText
                touchX < placementParameters.dateStartPosition + placementParameters.dateWidth -> // Just a regular touch on the date editor.
                    null
                touchX < placementParameters.dateRightTouchBufferLimit -> // We need to act like this was a touch on the date editor
                    expiryDateEditText
                touchX < placementParameters.cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                    cvcNumberEditText
                else -> null
            }
        }
    }

    @VisibleForTesting
    internal fun setDimensionOverrideSettings(dimensonOverrides: DimensionOverrideSettings?) {
        dimensionOverrides = dimensonOverrides
    }

    @VisibleForTesting
    internal fun setCardNumberIsViewed(cardNumberIsViewed: Boolean) {
        this.cardNumberIsViewed = cardNumberIsViewed
    }

    @VisibleForTesting
    internal fun updateSpaceSizes(isCardViewed: Boolean) {
        val frameWidth = frameWidth
        val frameStart = frameLayout.left
        if (frameWidth == 0) {
            // This is an invalid view state.
            return
        }

        placementParameters.cardWidth = getDesiredWidthInPixels(FULL_SIZING_CARD_TEXT, cardNumberEditText)

        placementParameters.dateWidth = getDesiredWidthInPixels(FULL_SIZING_DATE_TEXT, expiryDateEditText)

        @Card.CardBrand val brand = cardNumberEditText.cardBrand
        placementParameters.hiddenCardWidth = getDesiredWidthInPixels(getHiddenTextForBrand(brand), cardNumberEditText)

        placementParameters.cvcWidth = getDesiredWidthInPixels(getCvcPlaceHolderForBrand(brand), cvcNumberEditText)

        placementParameters.peekCardWidth = getDesiredWidthInPixels(getPeekCardTextForBrand(brand), cardNumberEditText)

        if (isCardViewed) {
            placementParameters.cardDateSeparation = (frameWidth -
                placementParameters.cardWidth - placementParameters.dateWidth)
            placementParameters.cardTouchBufferLimit = (frameStart +
                placementParameters.cardWidth + placementParameters.cardDateSeparation / 2)
            placementParameters.dateStartPosition = (frameStart +
                placementParameters.cardWidth + placementParameters.cardDateSeparation)
        } else {
            placementParameters.cardDateSeparation = (frameWidth / 2 -
                placementParameters.peekCardWidth -
                placementParameters.dateWidth / 2)
            placementParameters.dateCvcSeparation = (frameWidth -
                placementParameters.peekCardWidth -
                placementParameters.cardDateSeparation -
                placementParameters.dateWidth -
                placementParameters.cvcWidth)

            placementParameters.cardTouchBufferLimit = (frameStart +
                placementParameters.peekCardWidth +
                placementParameters.cardDateSeparation / 2)
            placementParameters.dateStartPosition = (frameStart +
                placementParameters.peekCardWidth +
                placementParameters.cardDateSeparation)
            placementParameters.dateRightTouchBufferLimit = (placementParameters.dateStartPosition +
                placementParameters.dateWidth +
                placementParameters.dateCvcSeparation / 2)
            placementParameters.cvcStartPosition = (placementParameters.dateStartPosition +
                placementParameters.dateWidth +
                placementParameters.dateCvcSeparation)
        }
    }

    private fun isCvcLengthValid(cvcValue: String): Boolean {
        val cvcLength = cvcValue.length
        return if (isAmEx && cvcLength == Card.CVC_LENGTH_AMERICAN_EXPRESS) {
            true
        } else cvcLength == Card.CVC_LENGTH_COMMON
    }

    private fun setLayoutValues(width: Int, margin: Int, editText: StripeEditText) {
        val layoutParams = editText.layoutParams as FrameLayout.LayoutParams
        layoutParams.width = width
        layoutParams.leftMargin = margin
        editText.layoutParams = layoutParams
    }

    private fun getDesiredWidthInPixels(text: String, editText: StripeEditText): Int {
        return dimensionOverrides?.getPixelWidth(text, editText)
            ?: Layout.getDesiredWidth(text, editText.paint).toInt()
    }

    private fun initView(attrs: AttributeSet?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cardNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER)
            expiryDateEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE)
            cvcNumberEditText.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE)
        }

        ViewCompat.setAccessibilityDelegate(
            cvcNumberEditText,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val accLabel = resources.getString(
                        R.string.acc_label_cvc_node,
                        cvcNumberEditText.text
                    )
                    info.text = accLabel
                }
            })

        cardNumberIsViewed = true

        @ColorInt var errorColorInt = cardNumberEditText.defaultErrorColorInt
        tintColorInt = cardNumberEditText.hintTextColors.defaultColor
        var cardHintText: String? = null
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.CardInputView,
                0, 0)

            try {
                tintColorInt = a.getColor(R.styleable.CardInputView_cardTint, tintColorInt)
                errorColorInt = a.getColor(R.styleable.CardInputView_cardTextErrorColor, errorColorInt)
                cardHintText = a.getString(R.styleable.CardInputView_cardHintText)
            } finally {
                a.recycle()
            }
        }

        cardHintText?.let {
            cardNumberEditText.hint = it
        }
        cardNumberEditText.setErrorColor(errorColorInt)
        expiryDateEditText.setErrorColor(errorColorInt)
        cvcNumberEditText.setErrorColor(errorColorInt)

        cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollLeft()
                cardInputListener?.onFocusChange(FOCUS_CARD)
            }
        }

        expiryDateEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRight()
                cardInputListener?.onFocusChange(FOCUS_EXPIRY)
            }
        }

        expiryDateEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(cardNumberEditText))

        cvcNumberEditText.setDeleteEmptyListener(
            BackUpFieldDeleteListener(expiryDateEditText))

        cvcNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRight()
                cardInputListener?.onFocusChange(FOCUS_CVC)
            }
            updateIconCvc(
                cardNumberEditText.cardBrand,
                hasFocus,
                cvcNumberEditText.text.toString()
            )
        }

        cvcNumberEditText.setAfterTextChangedListener(
            object : StripeEditText.AfterTextChangedListener {
                override fun onTextChanged(text: String) {
                    if (ViewUtils.isCvcMaximalLength(cardNumberEditText.cardBrand, text)) {
                        cardInputListener?.onCvcComplete()
                    }
                    updateIconCvc(cardNumberEditText.cardBrand,
                        cvcNumberEditText.hasFocus(),
                        text)
                }
            }
        )

        cardNumberEditText.setCardNumberCompleteListener(
            object : CardNumberEditText.CardNumberCompleteListener {
                override fun onCardNumberComplete() {
                    scrollRight()
                    cardInputListener?.onCardComplete()
                }
            }
        )

        cardNumberEditText.setCardBrandChangeListener(
            object : CardNumberEditText.CardBrandChangeListener {
                override fun onCardBrandChanged(brand: String) {
                    isAmEx = CardBrand.AMERICAN_EXPRESS == brand
                    updateIcon(brand)
                    updateCvc(brand)
                }
            }
        )

        expiryDateEditText.setExpiryDateEditListener(
            object : ExpiryDateEditText.ExpiryDateEditListener {
                override fun onExpiryDateComplete() {
                    cvcNumberEditText.requestFocus()
                    cardInputListener?.onExpirationComplete()
                }
            }
        )

        cardNumberEditText.requestFocus()
    }

    private fun scrollLeft() {
        if (cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartPosition = placementParameters.peekCardWidth +
            placementParameters.cardDateSeparation
        val cvcStartPosition = (dateStartPosition +
            placementParameters.dateWidth + placementParameters.dateCvcSeparation)

        updateSpaceSizes(true)

        val startPoint = (cardNumberEditText.layoutParams as FrameLayout.LayoutParams).leftMargin
        val slideCardLeftAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val params = cardNumberEditText.layoutParams as FrameLayout.LayoutParams
                params.leftMargin = (startPoint * (1 - interpolatedTime)).toInt()
                cardNumberEditText.layoutParams = params
            }
        }

        val dateDestination = placementParameters.cardWidth + placementParameters.cardDateSeparation
        val slideDateLeftAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val tempValue = (interpolatedTime * dateDestination + (1 - interpolatedTime) * dateStartPosition).toInt()
                val params = expiryDateEditText.layoutParams as FrameLayout.LayoutParams
                params.leftMargin = tempValue
                expiryDateEditText.layoutParams = params
            }
        }

        val cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition)
        val slideCvcLeftAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val tempValue = (interpolatedTime * cvcDestination + (1 - interpolatedTime) * cvcStartPosition).toInt()
                val params = cvcNumberEditText.layoutParams as FrameLayout.LayoutParams
                params.leftMargin = tempValue
                params.rightMargin = 0
                params.width = placementParameters.cvcWidth
                cvcNumberEditText.layoutParams = params
            }
        }

        slideCardLeftAnimation.setAnimationListener(object : AnimationEndListener() {
            override fun onAnimationEnd(animation: Animation) {
                cardNumberEditText.requestFocus()
            }
        })

        slideCardLeftAnimation.duration = ANIMATION_LENGTH
        slideDateLeftAnimation.duration = ANIMATION_LENGTH
        slideCvcLeftAnimation.duration = ANIMATION_LENGTH

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideCardLeftAnimation)
        animationSet.addAnimation(slideDateLeftAnimation)
        animationSet.addAnimation(slideCvcLeftAnimation)
        frameLayout.startAnimation(animationSet)
        cardNumberIsViewed = true
    }

    private fun scrollRight() {
        if (!cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartMargin = placementParameters.cardWidth + placementParameters.cardDateSeparation

        updateSpaceSizes(false)

        val slideCardRightAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val cardParams = cardNumberEditText.layoutParams as FrameLayout.LayoutParams
                cardParams.leftMargin = (-1f * placementParameters.hiddenCardWidth.toFloat() * interpolatedTime).toInt()
                cardNumberEditText.layoutParams = cardParams
            }
        }

        val dateDestination = placementParameters.peekCardWidth + placementParameters.cardDateSeparation

        val slideDateRightAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val tempValue = (interpolatedTime * dateDestination + (1 - interpolatedTime) * dateStartMargin).toInt()
                val dateParams = expiryDateEditText.layoutParams as FrameLayout.LayoutParams
                dateParams.leftMargin = tempValue
                expiryDateEditText.layoutParams = dateParams
            }
        }

        val cvcDestination = (placementParameters.peekCardWidth +
            placementParameters.cardDateSeparation +
            placementParameters.dateWidth +
            placementParameters.dateCvcSeparation)
        val cvcStartMargin = cvcDestination + (dateStartMargin - dateDestination)

        val slideCvcRightAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                super.applyTransformation(interpolatedTime, t)
                val tempValue = (interpolatedTime * cvcDestination + (1 - interpolatedTime) * cvcStartMargin).toInt()
                val cardParams = cvcNumberEditText.layoutParams as FrameLayout.LayoutParams
                cardParams.leftMargin = tempValue
                cardParams.rightMargin = 0
                cardParams.width = placementParameters.cvcWidth
                cvcNumberEditText.layoutParams = cardParams
            }
        }

        slideCardRightAnimation.duration = ANIMATION_LENGTH
        slideDateRightAnimation.duration = ANIMATION_LENGTH
        slideCvcRightAnimation.duration = ANIMATION_LENGTH

        slideCardRightAnimation.setAnimationListener(object : AnimationEndListener() {
            override fun onAnimationEnd(animation: Animation) {
                expiryDateEditText.requestFocus()
            }
        })

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideCardRightAnimation)
        animationSet.addAnimation(slideDateRightAnimation)
        animationSet.addAnimation(slideCvcRightAnimation)

        frameLayout.startAnimation(animationSet)
        cardNumberIsViewed = false
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            applyTint(false)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!initFlag && width != 0) {
            initFlag = true
            totalLengthInPixels = frameWidth

            updateSpaceSizes(cardNumberIsViewed)

            val cardLeftMargin = if (cardNumberIsViewed) {
                0
            } else {
                -1 * placementParameters.hiddenCardWidth
            }
            setLayoutValues(placementParameters.cardWidth, cardLeftMargin, cardNumberEditText)

            val dateMargin = if (cardNumberIsViewed)
                placementParameters.cardWidth + placementParameters.cardDateSeparation
            else
                placementParameters.peekCardWidth + placementParameters.cardDateSeparation
            setLayoutValues(placementParameters.dateWidth, dateMargin, expiryDateEditText)

            val cvcMargin = if (cardNumberIsViewed) {
                totalLengthInPixels
            } else {
                placementParameters.peekCardWidth + placementParameters.cardDateSeparation +
                    placementParameters.dateWidth + placementParameters.dateCvcSeparation
            }
            setLayoutValues(placementParameters.cvcWidth, cvcMargin, cvcNumberEditText)
        }
    }

    private fun getHiddenTextForBrand(@CardBrand brand: String): String {
        return if (CardBrand.AMERICAN_EXPRESS == brand) {
            HIDDEN_TEXT_AMEX
        } else {
            HIDDEN_TEXT_COMMON
        }
    }

    private fun getCvcPlaceHolderForBrand(@Card.CardBrand brand: String): String {
        return if (CardBrand.AMERICAN_EXPRESS == brand) {
            CVC_PLACEHOLDER_AMEX
        } else {
            CVC_PLACEHOLDER_COMMON
        }
    }

    private fun getPeekCardTextForBrand(@Card.CardBrand brand: String): String {
        return when (brand) {
            CardBrand.AMERICAN_EXPRESS -> {
                PEEK_TEXT_AMEX
            }
            CardBrand.DINERS_CLUB -> {
                PEEK_TEXT_DINERS
            }
            else -> {
                PEEK_TEXT_COMMON
            }
        }
    }

    private fun applyTint(isCvc: Boolean) {
        if (isCvc || CardBrand.UNKNOWN == cardNumberEditText.cardBrand) {
            val icon = cardIconImageView.drawable
            val compatIcon = DrawableCompat.wrap(icon)
            DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
            cardIconImageView.setImageDrawable(DrawableCompat.unwrap(compatIcon))
        }
    }

    private fun updateCvc(@Card.CardBrand brand: String) {
        if (CardBrand.AMERICAN_EXPRESS == brand) {
            cvcNumberEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Card.CVC_LENGTH_AMERICAN_EXPRESS))
            cvcNumberEditText.setHint(R.string.cvc_amex_hint)
        } else {
            cvcNumberEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Card.CVC_LENGTH_COMMON))
            cvcNumberEditText.setHint(R.string.cvc_number_hint)
        }
    }

    private fun updateIcon(@Card.CardBrand brand: String) {
        if (CardBrand.UNKNOWN == brand) {
            val icon = ContextCompat.getDrawable(context, R.drawable.ic_unknown)
            cardIconImageView.setImageDrawable(icon)
            applyTint(false)
        } else {
            cardIconImageView.setImageResource(Card.getBrandIcon(brand))
        }
    }

    private fun updateIconCvc(
        @CardBrand brand: String,
        hasFocus: Boolean,
        cvcText: String?
    ) {
        if (shouldIconShowBrand(brand, hasFocus, cvcText)) {
            updateIcon(brand)
        } else {
            updateIconForCvcEntry(CardBrand.AMERICAN_EXPRESS == brand)
        }
    }

    private fun updateIconForCvcEntry(isAmEx: Boolean) {
        if (isAmEx) {
            cardIconImageView.setImageResource(R.drawable.ic_cvc_amex)
        } else {
            cardIconImageView.setImageResource(R.drawable.ic_cvc)
        }
        applyTint(true)
    }

    /**
     * Interface useful for testing calculations without generating real views.
     */
    @VisibleForTesting
    internal interface DimensionOverrideSettings {

        val frameWidth: Int
        fun getPixelWidth(text: String, editText: EditText): Int
    }

    /**
     * A data-dump class.
     */
    internal class PlacementParameters {
        var cardWidth: Int = 0
        var hiddenCardWidth: Int = 0
        var peekCardWidth: Int = 0
        var cardDateSeparation: Int = 0
        var dateWidth: Int = 0
        var dateCvcSeparation: Int = 0
        var cvcWidth: Int = 0

        var cardTouchBufferLimit: Int = 0
        var dateStartPosition: Int = 0
        var dateRightTouchBufferLimit: Int = 0
        var cvcStartPosition: Int = 0

        override fun toString(): String {
            val touchBufferData = "Touch Buffer Data:\n" +
                "CardTouchBufferLimit = $cardTouchBufferLimit\n" +
                "DateStartPosition = $dateStartPosition\n" +
                "DateRightTouchBufferLimit = $dateRightTouchBufferLimit\n" +
                "CvcStartPosition = $cvcStartPosition"
            val elementSizeData = "CardWidth = $cardWidth\n" +
                "HiddenCardWidth = $hiddenCardWidth\n" +
                "PeekCardWidth = $peekCardWidth\n" +
                "CardDateSeparation = $cardDateSeparation\n" +
                "DateWidth = $dateWidth\n" +
                "DateCvcSeparation = $dateCvcSeparation\n" +
                "CvcWidth = $cvcWidth\n"
            return elementSizeData + touchBufferData
        }
    }

    /**
     * A convenience class for when we only want to listen for when an animation ends.
     */
    private abstract inner class AnimationEndListener : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            // Intentional No-op
        }

        override fun onAnimationRepeat(animation: Animation) {
            // Intentional No-op
        }
    }

    companion object {
        internal const val LOGGING_TOKEN = "CardInputView"

        private const val PEEK_TEXT_COMMON = "4242"
        private const val PEEK_TEXT_DINERS = "88"
        private const val PEEK_TEXT_AMEX = "34343"

        private const val CVC_PLACEHOLDER_COMMON = "CVC"
        private const val CVC_PLACEHOLDER_AMEX = "2345"

        // These intentionally include a space at the end.
        private const val HIDDEN_TEXT_AMEX = "3434 343434 "
        private const val HIDDEN_TEXT_COMMON = "4242 4242 4242 "

        private const val FULL_SIZING_CARD_TEXT = "4242 4242 4242 4242"
        private const val FULL_SIZING_DATE_TEXT = "MM/MM"

        private const val EXTRA_CARD_VIEWED = "extra_card_viewed"
        private const val EXTRA_SUPER_STATE = "extra_super_state"

        // This value is used to ensure that onSaveInstanceState is called
        // in the event that the user doesn't give this control an ID.
        @IdRes
        private val DEFAULT_READER_ID = R.id.stripe_default_reader_id

        private const val ANIMATION_LENGTH = 150L

        /**
         * Determines whether or not the icon should show the card brand instead of the
         * CVC helper icon.
         *
         * @param brand the [Card.CardBrand] in question, used for determining max length
         * @param cvcHasFocus `true` if the CVC entry field has focus, `false` otherwise
         * @param cvcText the current content of [cvcNumberEditText]
         * @return `true` if we should show the brand of the card, or `false` if we
         * should show the CVC helper icon instead
         */
        @VisibleForTesting
        internal fun shouldIconShowBrand(
            @Card.CardBrand brand: String,
            cvcHasFocus: Boolean,
            cvcText: String?
        ): Boolean {
            return if (!cvcHasFocus) {
                true
            } else ViewUtils.isCvcMaximalLength(brand, cvcText)
        }
    }
}
