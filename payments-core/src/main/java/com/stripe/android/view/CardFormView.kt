package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.R
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.databinding.StripeCardFormViewBinding
import com.stripe.android.databinding.StripeHorizontalDividerBinding
import com.stripe.android.databinding.StripeVerticalDividerBinding
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.DelicateCardDetailsApi
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardFormView.Style
import com.stripe.android.view.CardValidCallback.Fields
import com.stripe.android.uicore.R as UiCoreR
import com.stripe.payments.model.R as PaymentsModelR

// TODO: should this be removed?
/**
 * A view to collect credit card information and provide [CardParams] for API invocation.
 * The postal code field adjust its form accordingly based on currently selected country.
 *
 * Use [R.styleable.StripeCardFormView_cardFormStyle] to toggle style between [Style.Standard] and [Style.Borderless],
 * Use [R.styleable.StripeCardFormView_backgroundColorStateList] to change the card form's background color in enable and disabled state.
 *
 * To access the [CardParams], see details in [cardParams] property.
 * To get notified if the current card params are valid, set a [CardValidCallback] object with [setCardValidCallback].
 */
class CardFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val layoutInflater = LayoutInflater.from(context)
    private val viewBinding = StripeCardFormViewBinding.inflate(layoutInflater, this)

    private val cardContainer = viewBinding.cardMultilineWidgetContainer

    private val cardMultilineWidget = viewBinding.cardMultilineWidget

    private val countryPostalDivider = viewBinding.countryPostalDivider

    private val postalCodeContainer = viewBinding.postalCodeContainer

    private val errors = viewBinding.errors

    private val postalCodeView = viewBinding.postalCode

    private val countryLayout = viewBinding.countryLayout

    private val postalCodeValidator = PostalCodeValidator()

    private var style: Style = Style.Standard

    private val errorsMap = mutableMapOf<Fields, String?>()

    private var cardValidCallback: CardValidCallback? = null

    private val lifecycleOwnerDelegate = LifecycleOwnerDelegate()

    private val allEditTextFields: Collection<StripeEditText>
        get() {
            return listOf(
                cardMultilineWidget.cardNumberEditText,
                cardMultilineWidget.expiryDateEditText,
                cardMultilineWidget.cvcEditText,
                postalCodeView
            )
        }

    private val invalidFields: Set<Fields>
        get() {
            return (
                cardMultilineWidget.invalidFields.toList() +
                    listOfNotNull(Fields.Postal.takeIf { !isPostalValid() })
                ).toSet()
        }

    private val cardValidTextWatcher = object : StripeTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            super.afterTextChanged(s)
            cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
        }
    }

    internal var viewModelStoreOwner: ViewModelStoreOwner? = null

    private enum class Style(
        internal val attrValue: Int
    ) {
        Standard(0),
        Borderless(1)
    }

    /**
     * A [CardBrand] matching the current card number inputted by the user.
     */
    val brand: CardBrand
        get() {
            return cardMultilineWidget.brand
        }

    /**
     * Retrieve a [CardParams] representing the card details if all these fields are valid:
     * card number, expiration date, CVC, postal, country.
     * Returns null otherwise and display corresponding errors.
     */
    val cardParams: CardParams?
        get() {
            // first validate card fields
            if (!cardMultilineWidget.validateAllFields()) {
                cardMultilineWidget.shouldShowErrorIcon = true
                return null
            }
            cardMultilineWidget.shouldShowErrorIcon = false

            // then validate postal and country
            if (!isPostalValid()) {
                showPostalError()
                return null
            }

            // validation OK, clear any error view
            updateErrorsView(null)

            val expirationDate =
                requireNotNull(cardMultilineWidget.expiryDateEditText.validatedDate)

            return CardParams(
                brand = brand,
                loggingTokens = setOf(CARD_FORM_VIEW),
                number = cardMultilineWidget.validatedCardNumber?.value.orEmpty(),
                expMonth = expirationDate.month,
                expYear = expirationDate.year,
                cvc = cardMultilineWidget.cvcEditText.text?.toString(),
                address = Address.Builder()
                    .setCountryCode(countryLayout.selectedCountryCode)
                    .setPostalCode(postalCodeView.text?.toString())
                    .build(),
                networks = cardMultilineWidget.cardBrandView.cardParamsNetworks()
            )
        }

    /**
     * A [PaymentMethodCreateParams.Card] representing the card details if all fields are valid;
     * otherwise `null`
     */
    private val paymentMethodCard: PaymentMethodCreateParams.Card?
        @OptIn(DelicateCardDetailsApi::class)
        get() {
            return cardParams?.let {
                PaymentMethodCreateParams.Card(
                    number = it.number,
                    cvc = it.cvc,
                    expiryMonth = it.expMonth,
                    expiryYear = it.expYear,
                    attribution = it.attribution,
                    networks = cardMultilineWidget.cardBrandView.paymentMethodCreateParamsNetworks(),
                )
            }
        }

    /**
     * A [PaymentMethodCreateParams] representing the card details and postal code if all fields
     * are valid; otherwise `null`
     */
    val paymentMethodCreateParams: PaymentMethodCreateParams?
        get() = paymentMethodCard?.let { PaymentMethodCreateParams.create(it) }

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

    init {
        orientation = VERTICAL

        setupCountryAndPostal()
        setupCardWidget()

        var backgroundColorStateList: ColorStateList? = null

        context.withStyledAttributes(
            attrs,
            R.styleable.StripeCardFormView
        ) {
            backgroundColorStateList =
                getColorStateList(R.styleable.StripeCardFormView_backgroundColorStateList)
            style = Style.entries[getInt(R.styleable.StripeCardFormView_cardFormStyle, 0)]
        }

        backgroundColorStateList?.let {
            cardContainer.setCardBackgroundColor(it)
            cardMultilineWidget.setBackgroundColor(Color.TRANSPARENT)
            countryLayout.setBackgroundColor(Color.TRANSPARENT)
            postalCodeContainer.setBackgroundColor(Color.TRANSPARENT)
        }

        when (style) {
            Style.Standard -> applyStandardStyle()
            Style.Borderless -> applyBorderlessStyle()
        }
    }

    fun setCardValidCallback(callback: CardValidCallback?) {
        this.cardValidCallback = callback
        allEditTextFields.forEach { it.removeTextChangedListener(cardValidTextWatcher) }

        // only add the TextWatcher if it will be used
        if (callback != null) {
            allEditTextFields.forEach { it.addTextChangedListener(cardValidTextWatcher) }
        }
        // call immediately after setting
        cardValidCallback?.onInputChanged(invalidFields.isEmpty(), invalidFields)
    }

    private fun setupCountryAndPostal() {
        // wire up postal code and country
        updatePostalCodeViewLocale(countryLayout.selectedCountryCode)

        // color in sync with CardMultilineWidget
        postalCodeView.setErrorColor(
            ContextCompat.getColor(context, R.color.stripe_card_form_view_form_error)
        )

        postalCodeView.internalFocusChangeListeners.add { _, hasFocus ->
            if (!hasFocus) {
                postalCodeView.shouldShowError =
                    postalCodeView.fieldText.isNotBlank() && !isPostalValid()

                if (postalCodeView.shouldShowError) {
                    showPostalError()
                } else {
                    onFieldError(Fields.Postal, null)
                }
            }
        }

        postalCodeView.doAfterTextChanged {
            onFieldError(Fields.Postal, null)
        }

        postalCodeView.setErrorMessageListener { errorMessage ->
            onFieldError(
                Fields.Postal,
                errorMessage
            )
        }

        countryLayout.countryCodeChangeCallback = { countryCode ->
            updatePostalCodeViewLocale(countryCode)
            postalCodeContainer.isVisible = CountryUtils.doesCountryUsePostalCode(countryCode)
            postalCodeView.shouldShowError = false
            postalCodeView.text = null
        }
    }

    private fun updatePostalCodeViewLocale(countryCode: CountryCode?) {
        if (CountryCode.isUS(countryCode)) {
            postalCodeView.config = PostalCodeEditText.Config.US
            postalCodeView.setErrorMessage(resources.getString(UiCoreR.string.stripe_address_zip_invalid))
        } else {
            postalCodeView.config = PostalCodeEditText.Config.Global
            postalCodeView.setErrorMessage(resources.getString(R.string.stripe_address_postal_code_invalid))
        }
    }

    private fun isPostalValid() =
        countryLayout.selectedCountryCode?.let { countryCode ->
            postalCodeValidator.isValid(
                postalCode = postalCodeView.postalCode.orEmpty(),
                countryCode = countryCode.value
            )
        } ?: false

    private fun showPostalError() {
        onFieldError(
            Fields.Postal,
            postalCodeView.errorMessage
        )
    }

    private fun setupCardWidget() {
        setOf(
            cardMultilineWidget.cardNumberEditText,
            cardMultilineWidget.expiryDateEditText,
            cardMultilineWidget.cvcEditText
        ).forEach { editText ->
            editText.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.stripe_card_form_view_textsize)
            )
            editText.setTextColor(
                ContextCompat.getColorStateList(
                    context,
                    R.color.stripe_card_form_view_text_color
                )
            )

            editText.setBackgroundResource(android.R.color.transparent)

            editText.setErrorColor(
                ContextCompat.getColor(context, R.color.stripe_card_form_view_form_error)
            )
        }

        cardMultilineWidget.expiryDateEditText.includeSeparatorGaps = true
        cardMultilineWidget.expirationDatePlaceholderRes = null
        cardMultilineWidget.expiryTextInputLayout.hint = context.getString(UiCoreR.string.stripe_expiration_date_hint)
        cardMultilineWidget.cardNumberTextInputLayout.placeholderText = null
        cardMultilineWidget.setCvcPlaceholderText("")

        cardMultilineWidget.viewModelStoreOwner = viewModelStoreOwner
        cardMultilineWidget.cardNumberEditText.viewModelStoreOwner = viewModelStoreOwner

        cardMultilineWidget.cvcEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        cardMultilineWidget.setBackgroundResource(R.drawable.stripe_card_form_view_text_input_layout_background)
        cardMultilineWidget.cvcEditText.doAfterTextChanged { cvcText ->
            if (postalCodeContainer.isVisible && cardMultilineWidget.brand.isMaxCvc(cvcText.toString())) {
                postalCodeView.requestFocus()
            }
        }

        val layoutMarginHorizontal =
            resources.getDimensionPixelSize(R.dimen.stripe_card_form_view_text_margin_horizontal)
        val layoutMarginVertical =
            resources.getDimensionPixelSize(R.dimen.stripe_card_form_view_text_margin_vertical)

        cardMultilineWidget.cardNumberTextInputLayout.updateLayoutParams<FrameLayout.LayoutParams> {
            marginStart = layoutMarginHorizontal
            marginEnd = layoutMarginHorizontal
            topMargin = layoutMarginVertical
            bottomMargin = layoutMarginVertical
        }

        setOf(
            cardMultilineWidget.expiryTextInputLayout,
            cardMultilineWidget.cvcInputLayout
        ).forEach { frameLayout ->
            frameLayout.updateLayoutParams<LayoutParams> {
                marginStart = layoutMarginHorizontal
                marginEnd = layoutMarginHorizontal
                topMargin = layoutMarginVertical
                bottomMargin = layoutMarginVertical
            }
            frameLayout.isErrorEnabled = false
            frameLayout.error = null
        }

        cardMultilineWidget.setCvcIcon(PaymentsModelR.drawable.stripe_ic_cvc)

        cardMultilineWidget.cardNumberErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onFieldError(
                    Fields.Number,
                    errorMessage
                )
            }
        cardMultilineWidget.expirationDateErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onFieldError(
                    Fields.Expiry,
                    errorMessage
                )
            }
        cardMultilineWidget.cvcErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onFieldError(
                    Fields.Cvc,
                    errorMessage
                )
            }
        cardMultilineWidget.postalCodeErrorListener = null
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        cardContainer.isEnabled = enabled
        cardMultilineWidget.isEnabled = enabled
        countryLayout.isEnabled = enabled
        postalCodeContainer.isEnabled = enabled
        errors.isEnabled = enabled
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            STATE_SUPER_STATE to super.onSaveInstanceState(),
            STATE_ENABLED to isEnabled,
            STATE_ON_BEHALF_OF to onBehalfOf,
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER_STATE))
            isEnabled = state.getBoolean(STATE_ENABLED)
            onBehalfOf = state.getString(STATE_ON_BEHALF_OF)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwnerDelegate.initLifecycle(this)
        // Merchant could set onBehalfOf before view is attached to window.
        // Check and set if needed.
        doWithCardWidgetViewModel(viewModelStoreOwner) { viewModel ->
            if (onBehalfOf != null && viewModel.onBehalfOf != onBehalfOf) {
                viewModel.setOnBehalfOf(onBehalfOf)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleOwnerDelegate.destroyLifecycle(this)
    }

    /**
     * A list of preferred networks that should be used to process payments made with a co-branded
     * card if your user hasn't selected a network themselves.
     *
     * The first preferred network that matches any available network will be used. If no preferred
     * network is applicable, Stripe will select the network.
     */
    fun setPreferredNetworks(preferredNetworks: List<CardBrand>) {
        cardMultilineWidget.cardBrandView.merchantPreferredNetworks = preferredNetworks
    }

    private fun applyStandardStyle() {
        // add horizontal divider between card number and other fields
        cardMultilineWidget.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            1
        )

        // add vertical divider between expiry date and CVC
        cardMultilineWidget.secondRowLayout.addView(
            StripeVerticalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget.secondRowLayout,
                false
            ).root,
            1
        )
        // add horizontal divider below expiry date and CVC
        cardMultilineWidget.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            cardMultilineWidget.childCount
        )

        // make cardElevation non zero to show border
        cardContainer.cardElevation =
            resources.getDimension(R.dimen.stripe_card_form_view_card_elevation)
    }

    private fun applyBorderlessStyle() {
        // add horizontal divider below cardNumberTextInputLayout
        cardMultilineWidget.cardNumberTextInputLayout.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            1
        )

        // add a separate horizontal dividers below expiry date and CVC respectively
        cardMultilineWidget.expiryTextInputLayout.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            1
        )

        cardMultilineWidget.cvcInputLayout.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            1
        )

        // add horizontal divider below countryLayout and hide countryPostalDivider
        countryLayout.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                countryLayout,
                false
            ).root
        )
        countryPostalDivider.isVisible = false

        // hide border
        cardContainer.cardElevation = 0f
    }

    private fun onFieldError(
        field: Fields,
        errorMessage: String?
    ) {
        errorsMap[field] = errorMessage

        val error = Fields.entries
            .map { errorsMap[it] }
            .firstOrNull { !it.isNullOrBlank() }

        updateErrorsView(error)
    }

    private fun updateErrorsView(errorMessage: String?) {
        errors.text = errorMessage
        errors.isVisible = errorMessage != null
    }

    internal companion object {
        const val CARD_FORM_VIEW = "CardFormView"
        private const val STATE_ENABLED = "state_enabled"
        private const val STATE_SUPER_STATE = "state_super_state"
        private const val STATE_ON_BEHALF_OF = "state_on_behalf_of"
    }
}
