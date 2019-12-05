package com.stripe.android.view

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Layout
import android.text.TextPaint
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Transformation
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
import com.stripe.android.model.Address
import com.stripe.android.model.Card
import com.stripe.android.model.Card.CardBrand
import com.stripe.android.model.PaymentMethod
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
    private val expiryDateEditText: ExpiryDateEditText
    private val cvcNumberEditText: CvcEditText

    private var cardInputListener: CardInputListener? = null

    @JvmSynthetic
    internal var cardNumberIsViewed = true

    @ColorInt
    private var tintColorInt: Int = 0

    private var isAmEx: Boolean = false
    private var initFlag: Boolean = false

    @JvmSynthetic
    internal var layoutWidthCalculator: LayoutWidthCalculator = DefaultLayoutWidthCalculator()

    internal val placementParameters: PlacementParameters = PlacementParameters()

    private val postalCode: String? = null

    private val cvcValue: String?
        get() {
            return cvcNumberEditText.cvcValue
        }

    private val standardFields: List<StripeEditText>

    /**
     * Gets a [PaymentMethodCreateParams.Card] object from the user input, if all fields are
     * valid. If not, returns `null`.
     *
     * @return a valid [PaymentMethodCreateParams.Card] object based on user input, or
     * `null` if any field is invalid
     */
    override val paymentMethodCard: PaymentMethodCreateParams.Card?
        get() {
            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = expiryDateEditText.validDateFields
            val cvcValue = cvcValue

            return if (cardNumber == null || cardDate == null || cvcValue == null) {
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

    private val billingDetails: PaymentMethod.BillingDetails?
        get() {
            return postalCode?.let {
                PaymentMethod.BillingDetails(
                    address = Address(
                        postalCode = it
                    )
                )
            }
        }

    override val paymentMethodCreateParams: PaymentMethodCreateParams?
        get() {
            return paymentMethodCard?.let { card ->
                PaymentMethodCreateParams.create(card, billingDetails)
            }
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

    override val cardBuilder: Card.Builder?
        get() {
            val cardNumber = cardNumberEditText.cardNumber
            val cardDate = expiryDateEditText.validDateFields
            return if (cardNumber == null || cardDate == null) {
                null
            } else {
                cvcValue?.let {
                    Card.Builder(cardNumber, cardDate.first, cardDate.second, it)
                        .addressZip(postalCode)
                        .loggingTokens(listOf(LOGGING_TOKEN))
                }
            }
        }

    private val frameWidth: Int
        get() = frameWidthSupplier()

    @JvmSynthetic
    internal var frameWidthSupplier: () -> Int

    init {
        View.inflate(getContext(), R.layout.card_input_widget, this)

        // This ensures that onRestoreInstanceState is called
        // during rotations.
        if (id == View.NO_ID) {
            id = DEFAULT_READER_ID
        }

        orientation = HORIZONTAL
        minimumWidth = resources.getDimensionPixelSize(R.dimen.stripe_card_widget_min_width)

        frameLayout = findViewById(R.id.frame_container)
        cardNumberEditText = frameLayout.findViewById(R.id.et_card_number)
        expiryDateEditText = frameLayout.findViewById(R.id.et_expiry_date)
        cvcNumberEditText = frameLayout.findViewById(R.id.et_cvc)

        frameWidthSupplier = { frameLayout.width }

        cardIconImageView = findViewById(R.id.iv_card_icon)

        standardFields = listOf(
            cardNumberEditText, cvcNumberEditText, expiryDateEditText
        )

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
        this.cardNumberIsViewed = !cardNumberEditText.isCardNumberValid
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

        standardFields.forEach { it.setText("") }
    }

    /**
     * Enable or disable text fields
     *
     * @param isEnabled boolean indicating whether fields should be enabled
     */
    override fun setEnabled(isEnabled: Boolean) {
        standardFields.forEach { it.isEnabled = isEnabled }
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
        return standardFields.all { it.isEnabled }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action != MotionEvent.ACTION_DOWN) {
            return super.onInterceptTouchEvent(ev)
        }

        return getFocusRequestOnTouch(ev.x.toInt())?.let {
            it.requestFocus()
            true
        } ?: super.onInterceptTouchEvent(ev)
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(STATE_SUPER_STATE, super.onSaveInstanceState())
            putBoolean(STATE_CARD_VIEWED, cardNumberIsViewed)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            cardNumberIsViewed = state.getBoolean(STATE_CARD_VIEWED, true)
            updateSpaceSizes(cardNumberIsViewed)
            placementParameters.totalLengthInPixels = frameWidth
            val cardMargin: Int
            val dateMargin: Int
            val cvcMargin: Int
            if (cardNumberIsViewed) {
                cardMargin = 0
                dateMargin = placementParameters.cardWidth + placementParameters.cardDateSeparation
                cvcMargin = placementParameters.totalLengthInPixels
            } else {
                cardMargin = -1 * placementParameters.hiddenCardWidth
                dateMargin = placementParameters.peekCardWidth + placementParameters.cardDateSeparation
                cvcMargin = dateMargin + placementParameters.dateWidth + placementParameters.dateCvcSeparation
            }

            updateFieldLayout(
                view = cardNumberEditText,
                width = placementParameters.cardWidth,
                margin = cardMargin
            )
            updateFieldLayout(
                view = expiryDateEditText,
                width = placementParameters.dateWidth,
                margin = dateMargin
            )
            updateFieldLayout(
                view = cvcNumberEditText,
                width = placementParameters.cvcWidth,
                margin = cvcMargin
            )

            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER_STATE))
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
    internal fun updateSpaceSizes(isCardViewed: Boolean) {
        val frameWidth = frameWidth
        val frameStart = frameLayout.left
        if (frameWidth == 0) {
            // This is an invalid view state.
            return
        }

        placementParameters.cardWidth = getDesiredWidthInPixels(
            FULL_SIZING_CARD_TEXT, cardNumberEditText
        )

        placementParameters.dateWidth = getDesiredWidthInPixels(
            FULL_SIZING_DATE_TEXT, expiryDateEditText
        )

        @Card.CardBrand val brand = cardNumberEditText.cardBrand
        placementParameters.hiddenCardWidth = getDesiredWidthInPixels(
            getHiddenTextForBrand(brand), cardNumberEditText
        )

        placementParameters.cvcWidth = getDesiredWidthInPixels(
            getCvcPlaceHolderForBrand(brand), cvcNumberEditText
        )

        placementParameters.peekCardWidth = getDesiredWidthInPixels(
            getPeekCardTextForBrand(brand), cardNumberEditText
        )

        placementParameters.updateSpacing(isCardViewed, frameStart, frameWidth)
    }

    private fun updateFieldLayout(view: View, width: Int, margin: Int) {
        view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
            this.width = width
            this.leftMargin = margin
        }
    }

    private fun getDesiredWidthInPixels(text: String, editText: StripeEditText): Int {
        return layoutWidthCalculator.calculate(text, editText.paint)
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
                    info.text = resources.getString(
                        R.string.acc_label_cvc_node,
                        cvcNumberEditText.text
                    )
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

        standardFields.forEach { it.setErrorColor(errorColorInt) }

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

        expiryDateEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(cardNumberEditText))
        cvcNumberEditText.setDeleteEmptyListener(BackUpFieldDeleteListener(expiryDateEditText))

        cvcNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollRight()
                cardInputListener?.onFocusChange(FOCUS_CVC)
            }
            updateIconCvc(
                cardNumberEditText.cardBrand,
                hasFocus,
                cvcValue
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

        cardNumberEditText.completionCallback = {
            scrollRight()
            cardInputListener?.onCardComplete()
        }

        cardNumberEditText.brandChangeCallback = { brand ->
            isAmEx = CardBrand.AMERICAN_EXPRESS == brand
            updateIcon(brand)
            cvcNumberEditText.updateBrand(brand)
        }

        expiryDateEditText.completionCallback = {
            cvcNumberEditText.requestFocus()
            cardInputListener?.onExpirationComplete()
        }

        cardNumberEditText.requestFocus()
    }

    // reveal the full card number field
    private fun scrollLeft() {
        if (cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartPosition = placementParameters.dateDestination
        val cvcStartPosition = dateStartPosition + placementParameters.dateWidth + placementParameters.dateCvcSeparation

        updateSpaceSizes(isCardViewed = true)

        val slideCardLeftAnimation = CardNumberSlideLeftAnimation(
            view = cardNumberEditText
        )

        val dateDestination = placementParameters.cardWidth + placementParameters.cardDateSeparation
        val slideDateLeftAnimation = ExpiryDateSlideLeftAnimation(
            view = expiryDateEditText,
            startPosition = dateStartPosition,
            destination = dateDestination
        )

        val cvcDestination = cvcStartPosition + (dateDestination - dateStartPosition)
        val slideCvcLeftAnimation = CvcSlideLeftAnimation(
            view = cvcNumberEditText,
            startPosition = cvcStartPosition,
            destination = cvcDestination,
            newWidth = placementParameters.cvcWidth
        )

        startSlideAnimation(listOf(
            slideCardLeftAnimation,
            slideDateLeftAnimation,
            slideCvcLeftAnimation
        ))

        cardNumberIsViewed = true
    }

    // reveal the secondary fields
    private fun scrollRight() {
        if (!cardNumberIsViewed || !initFlag) {
            return
        }

        val dateStartMargin = placementParameters.cardWidth + placementParameters.cardDateSeparation

        updateSpaceSizes(isCardViewed = false)

        val slideCardRightAnimation = CardNumberSlideRightAnimation(
            view = cardNumberEditText,
            hiddenCardWidth = placementParameters.hiddenCardWidth,
            focusOnEndView = expiryDateEditText
        )

        val dateDestination = placementParameters.dateDestination
        val slideDateRightAnimation = ExpiryDateSlideRightAnimation(
            view = expiryDateEditText,
            startMargin = dateStartMargin,
            destination = dateDestination
        )

        val cvcDestination = placementParameters.cvcDestination
        val slideCvcRightAnimation = CvcSlideRightAnimation(
            view = cvcNumberEditText,
            startMargin = cvcDestination + (dateStartMargin - dateDestination),
            destination = cvcDestination,
            newWidth = placementParameters.cvcWidth
        )

        startSlideAnimation(listOf(
            slideCardRightAnimation,
            slideDateRightAnimation,
            slideCvcRightAnimation
        ))

        cardNumberIsViewed = false
    }

    private fun startSlideAnimation(animations: List<Animation>) {
        val animationSet = AnimationSet(true).apply {
            animations.forEach { addAnimation(it) }
        }
        frameLayout.startAnimation(animationSet)
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
            placementParameters.totalLengthInPixels = frameWidth

            updateSpaceSizes(cardNumberIsViewed)

            updateFieldLayout(
                view = cardNumberEditText,
                width = placementParameters.cardWidth,
                margin = if (cardNumberIsViewed) {
                    0
                } else {
                    -1 * placementParameters.hiddenCardWidth
                }
            )

            updateFieldLayout(
                view = expiryDateEditText,
                width = placementParameters.dateWidth,
                margin = placementParameters.getDateMargin(cardNumberIsViewed)
            )

            updateFieldLayout(
                view = cvcNumberEditText,
                width = placementParameters.cvcWidth,
                margin = placementParameters.getCvcMargin(cardNumberIsViewed)
            )
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

    private fun updateIcon(@Card.CardBrand brand: String) {
        if (CardBrand.UNKNOWN == brand) {
            val icon = ContextCompat.getDrawable(context, R.drawable.stripe_ic_unknown)
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
        cardIconImageView.setImageResource(if (isAmEx) {
            R.drawable.stripe_ic_cvc_amex
        } else {
            R.drawable.stripe_ic_cvc
        })
        applyTint(true)
    }

    /**
     * A class for tracking the placement and layout of fields
     */
    internal class PlacementParameters {
        internal var totalLengthInPixels: Int = 0

        internal var cardWidth: Int = 0
        internal var hiddenCardWidth: Int = 0
        internal var peekCardWidth: Int = 0
        internal var cardDateSeparation: Int = 0
        internal var dateWidth: Int = 0
        internal var dateCvcSeparation: Int = 0
        internal var cvcWidth: Int = 0

        internal var cardTouchBufferLimit: Int = 0
        internal var dateStartPosition: Int = 0
        internal var dateRightTouchBufferLimit: Int = 0
        internal var cvcStartPosition: Int = 0

        internal val dateDestination: Int
            @JvmSynthetic
            get() {
                return peekCardWidth + cardDateSeparation
            }

        internal val cvcDestination: Int
            @JvmSynthetic
            get() {
                return dateDestination + dateWidth + dateCvcSeparation
            }

        @JvmSynthetic
        internal fun getDateMargin(cardNumberIsViewed: Boolean): Int {
            return if (cardNumberIsViewed) {
                cardWidth + cardDateSeparation
            } else {
                dateDestination
            }
        }

        @JvmSynthetic
        internal fun getCvcMargin(cardNumberIsViewed: Boolean): Int {
            return if (cardNumberIsViewed) {
                totalLengthInPixels
            } else {
                cvcDestination
            }
        }

        @JvmSynthetic
        internal fun updateSpacing(
            isCardViewed: Boolean,
            frameStart: Int,
            frameWidth: Int
        ) {
            if (isCardViewed) {
                cardDateSeparation = frameWidth - cardWidth - dateWidth
                cardTouchBufferLimit = frameStart + cardWidth + cardDateSeparation / 2
                dateStartPosition = frameStart + cardWidth + cardDateSeparation
            } else {
                cardDateSeparation = frameWidth / 2 - peekCardWidth - dateWidth / 2
                dateCvcSeparation = frameWidth - peekCardWidth - cardDateSeparation - dateWidth - cvcWidth

                cardTouchBufferLimit = frameStart + peekCardWidth + cardDateSeparation / 2
                dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
                dateRightTouchBufferLimit = dateStartPosition + dateWidth + dateCvcSeparation / 2
                cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
            }
        }

        override fun toString(): String {
            val touchBufferData = """
                Touch Buffer Data:
                "CardTouchBufferLimit = $cardTouchBufferLimit
                "DateStartPosition = $dateStartPosition
                "DateRightTouchBufferLimit = $dateRightTouchBufferLimit
                "CvcStartPosition = $cvcStartPosition"
                """

            val elementSizeData = """
                CardWidth = $cardWidth
                HiddenCardWidth = $hiddenCardWidth
                PeekCardWidth = $peekCardWidth
                CardDateSeparation = $cardDateSeparation
                DateWidth = $dateWidth
                DateCvcSeparation = $dateCvcSeparation
                CvcWidth = $cvcWidth
                """

            return elementSizeData + touchBufferData
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

    private class CardNumberSlideLeftAnimation(
        private val view: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(object : AnimationEndListener() {
                override fun onAnimationEnd(animation: Animation) {
                    view.requestFocus()
                }
            })
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = (leftMargin * (1 - interpolatedTime)).toInt()
            }
        }
    }

    private class ExpiryDateSlideLeftAnimation(
        private val view: View,
        private val startPosition: Int,
        private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
            }
        }
    }

    private class CvcSlideLeftAnimation(
        private val view: View,
        private val startPosition: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                this.leftMargin = (interpolatedTime * destination + (1 - interpolatedTime) * startPosition).toInt()
                this.rightMargin = 0
                this.width = newWidth
            }
        }
    }

    private class CardNumberSlideRightAnimation(
        private val view: View,
        private val hiddenCardWidth: Int,
        private val focusOnEndView: View
    ) : CardFieldAnimation() {
        init {
            setAnimationListener(object : AnimationEndListener() {
                override fun onAnimationEnd(animation: Animation) {
                    focusOnEndView.requestFocus()
                }
            })
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = (-1f * hiddenCardWidth.toFloat() * interpolatedTime).toInt()
            }
        }
    }

    private class ExpiryDateSlideRightAnimation(
        private val view: View,
        private val startMargin: Int,
        private val destination: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
            }
        }
    }

    private class CvcSlideRightAnimation(
        private val view: View,
        private val startMargin: Int,
        private val destination: Int,
        private val newWidth: Int
    ) : CardFieldAnimation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin =
                    (interpolatedTime * destination + (1 - interpolatedTime) * startMargin).toInt()
                rightMargin = 0
                width = newWidth
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

    internal interface LayoutWidthCalculator {
        fun calculate(text: String, paint: TextPaint): Int
    }

    internal class DefaultLayoutWidthCalculator : LayoutWidthCalculator {
        override fun calculate(text: String, paint: TextPaint): Int {
            return Layout.getDesiredWidth(text, paint).toInt()
        }
    }

    internal companion object {
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

        private const val STATE_CARD_VIEWED = "state_card_viewed"
        private const val STATE_SUPER_STATE = "state_super_state"

        // This value is used to ensure that onSaveInstanceState is called
        // in the event that the user doesn't give this control an ID.
        @IdRes
        private val DEFAULT_READER_ID = R.id.stripe_default_reader_id

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
            return !cvcHasFocus || ViewUtils.isCvcMaximalLength(brand, cvcText)
        }
    }
}
