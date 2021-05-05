package com.stripe.android.paymentsheet

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.databinding.StripeHorizontalDividerBinding
import com.stripe.android.databinding.StripeVerticalDividerBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CountryCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.BillingAddressView
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.view.CardInputListener
import com.stripe.android.view.CardMultilineWidget
import com.stripe.android.view.Country
import com.stripe.android.view.StripeEditText

/**
 * A `Fragment` for adding new card payment method.
 */
internal abstract class BaseAddCardFragment(
    private val eventReporter: EventReporter
) : Fragment() {
    abstract val sheetViewModel: BaseSheetViewModel<*>

    private lateinit var cardMultilineWidget: CardMultilineWidget
    private lateinit var billingAddressView: BillingAddressView
    private lateinit var cardErrors: TextView
    private lateinit var billingErrors: TextView
    private lateinit var saveCardCheckbox: CheckBox
    private lateinit var bottomSpace: Space
    private lateinit var addCardHeader: TextView

    /**
     * A [PaymentMethodCreateParams] instance of card and billing address details are valid;
     * otherwise, `null`.
     */
    private val paymentMethodParams: PaymentMethodCreateParams?
        get() {
            val cardParams = billingAddressView.address.value?.let { billingAddress ->
                cardMultilineWidget.cardParams?.also { cardParams ->
                    cardParams.address = billingAddress
                }
            }

            return cardParams?.let {
                PaymentMethodCreateParams.createCard(it)
            }
        }

    private val addCardViewModel: AddCardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val themedInflater = inflater.cloneInContext(
            ContextThemeWrapper(requireActivity(), R.style.StripePaymentSheetAddCardTheme)
        )
        return themedInflater.inflate(
            R.layout.fragment_paymentsheet_add_card,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val config = arguments?.getParcelable<FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        if (activity == null || config == null) {
            sheetViewModel.onFatal(
                IllegalArgumentException("Failed to start add payment option fragment.")
            )
            return
        }

        val viewBinding = FragmentPaymentsheetAddCardBinding.bind(view)
        cardMultilineWidget = viewBinding.cardMultilineWidget
        billingAddressView = viewBinding.billingAddress
        cardErrors = viewBinding.cardErrors
        billingErrors = viewBinding.billingErrors
        saveCardCheckbox = viewBinding.saveCardCheckbox
        bottomSpace = viewBinding.bottomSpace
        addCardHeader = viewBinding.addCardHeader

        // This must be done prior to setting up the card widget or the save card checkbox won't
        // populate correctly.
        populateFieldsFromNewCard()
        setupCardWidget()

        billingAddressView.address.observe(viewLifecycleOwner) {
            // update selection whenever billing address changes
            updateSelection()
        }

        cardMultilineWidget.setCardValidCallback { isValid, _ ->
            // update selection whenever card details changes
            addCardViewModel.isCardValid = isValid
            updateSelection()
        }

        cardMultilineWidget.setCardInputListener(object : CardInputListener {
            override fun onFocusChange(focusField: CardInputListener.FocusField) {}

            override fun onCardComplete() {}

            override fun onExpirationComplete() {}

            override fun onCvcComplete() {
                // move to first field when CVC is complete
                billingAddressView.focusFirstField()
            }
        })

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            saveCardCheckbox.isEnabled = !isProcessing
            cardMultilineWidget.isEnabled = !isProcessing
            billingAddressView.isEnabled = !isProcessing
        }

        setupSaveCardCheckbox()

        eventReporter.onShowNewPaymentOptionForm()
    }

    protected fun updateSelection() {
        val validCard = if (addCardViewModel.isCardValid) {
            paymentMethodParams?.let { params ->
                PaymentSelection.New.Card(
                    params,
                    cardMultilineWidget.brand,
                    shouldSavePaymentMethod = shouldSaveCard()
                )
            }
        } else {
            null
        }

        // If you open a new unsaved card, edit it, go to the list view and come back the edited
        // card should be shown, this means that the new card must be updated
        validCard?.let {
            sheetViewModel.newCard = validCard
        }
        sheetViewModel.updateSelection(validCard)
    }

    private fun setupCardWidget() {
        setOf(
            cardMultilineWidget.cardNumberEditText,
            cardMultilineWidget.expiryDateEditText,
            cardMultilineWidget.cvcEditText
        ).forEach { editText ->
            editText.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.stripe_paymentsheet_form_textsize)
            )
            editText.setTextColor(
                ContextCompat.getColor(
                    requireActivity(),
                    R.color.stripe_paymentsheet_textinput_color
                )
            )

            editText.setBackgroundResource(android.R.color.transparent)

            editText.setErrorColor(
                ContextCompat.getColor(requireActivity(), R.color.stripe_paymentsheet_form_error)
            )
        }

        cardMultilineWidget.expiryDateEditText.includeSeparatorGaps = true
        cardMultilineWidget.expirationDatePlaceholderRes = null
        cardMultilineWidget.expiryTextInputLayout.hint =
            getString(R.string.stripe_paymentsheet_expiration_date_hint)
        cardMultilineWidget.cardNumberTextInputLayout.placeholderText = null
        cardMultilineWidget.setCvcPlaceholderText("")

        cardMultilineWidget.cvcEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        cardMultilineWidget.setBackgroundResource(R.color.stripe_paymentsheet_elements_background_states)

        // add vertical divider between expiry date and CVC
        cardMultilineWidget.secondRowLayout.addView(
            StripeVerticalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget.secondRowLayout,
                false
            ).root,
            1
        )

        // add horizontal divider between card number and other fields
        cardMultilineWidget.addView(
            StripeHorizontalDividerBinding.inflate(
                layoutInflater,
                cardMultilineWidget,
                false
            ).root,
            1
        )

        val layoutMarginHorizontal =
            resources.getDimensionPixelSize(R.dimen.stripe_paymentsheet_cardwidget_margin_horizontal)
        val layoutMarginVertical =
            resources.getDimensionPixelSize(R.dimen.stripe_paymentsheet_cardwidget_margin_vertical)
        setOf(
            cardMultilineWidget.cardNumberTextInputLayout,
            cardMultilineWidget.expiryTextInputLayout,
            cardMultilineWidget.cvcInputLayout
        ).forEach { layout ->
            layout.updateLayoutParams<LinearLayout.LayoutParams> {
                marginStart = layoutMarginHorizontal
                marginEnd = layoutMarginHorizontal
                topMargin = layoutMarginVertical
                bottomMargin = layoutMarginVertical
            }
            layout.isErrorEnabled = false
            layout.error = null
        }

        cardMultilineWidget.setCvcIcon(R.drawable.stripe_ic_paymentsheet_cvc)

        cardMultilineWidget.cardBrandIconSupplier =
            CardMultilineWidget.CardBrandIconSupplier { cardBrand ->
                CardMultilineWidget.CardBrandIcon(
                    when (cardBrand) {
                        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
                        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
                        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
                        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
                        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
                        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
                        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
                        CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
                    }
                )
            }

        cardMultilineWidget.cardNumberErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onCardError(
                    AddCardViewModel.Field.Number,
                    errorMessage
                )
            }
        cardMultilineWidget.expirationDateErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onCardError(
                    AddCardViewModel.Field.Date,
                    errorMessage
                )
            }
        cardMultilineWidget.cvcErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onCardError(
                    AddCardViewModel.Field.Cvc,
                    errorMessage
                )
            }
        cardMultilineWidget.postalCodeErrorListener = null

        billingAddressView.postalCodeViewListener =
            object : BillingAddressView.PostalCodeViewListener {
                override fun onLosingFocus(country: Country?, isPostalValid: Boolean) {
                    val shouldToggleBillingError =
                        !isPostalValid && !billingAddressView.postalCodeView.text.isNullOrEmpty()
                    billingErrors.text = if (shouldToggleBillingError) {
                        if (country == null || CountryCode.isUS(country.code)) {
                            getString(R.string.address_zip_invalid)
                        } else {
                            getString(R.string.address_postal_code_invalid)
                        }
                    } else {
                        null
                    }
                    billingErrors.isVisible = !billingErrors.text.isNullOrEmpty()
                }

                override fun onGainingFocus(country: Country?, isPostalValid: Boolean) {
                    // Always hide error field when user starts editing postal code
                    billingErrors.isVisible = false
                }

                override fun onCountryChanged(country: Country?, isPostalValid: Boolean) {
                    billingErrors.text = null
                    billingErrors.isVisible = false
                }
            }
    }

    private fun populateFieldsFromNewCard() {
        val paymentMethodCreateParams = sheetViewModel.newCard?.paymentMethodCreateParams
        saveCardCheckbox.isChecked = sheetViewModel.newCard?.shouldSavePaymentMethod ?: true
        cardMultilineWidget.populate(paymentMethodCreateParams?.card)
        billingAddressView.populate(paymentMethodCreateParams?.billingDetails?.address)
    }

    private fun onCardError(
        field: AddCardViewModel.Field,
        errorMessage: String?
    ) {
        addCardViewModel.cardErrors[field] = errorMessage

        val error = AddCardViewModel.Field.values()
            .map { addCardViewModel.cardErrors[it] }
            .firstOrNull { !it.isNullOrBlank() }

        cardErrors.text = error
        cardErrors.isVisible = error != null
    }

    private fun setupSaveCardCheckbox() {
        val merchantDisplayName = sheetViewModel.config?.merchantDisplayName.takeUnless {
            it.isNullOrBlank()
        }
        saveCardCheckbox.text = merchantDisplayName?.let {
            getString(R.string.stripe_paymentsheet_save_this_card_with_merchant_name, it)
        } ?: getString(R.string.stripe_paymentsheet_save_this_card)

        saveCardCheckbox.isVisible = sheetViewModel.customerConfig != null
        bottomSpace.isVisible = !saveCardCheckbox.isVisible

        saveCardCheckbox.setOnCheckedChangeListener { _, _ ->
            onSaveCardCheckboxChanged()
        }
    }

    private fun onSaveCardCheckboxChanged() {
        val selection = sheetViewModel.selection.value
        if (selection is PaymentSelection.New.Card) {
            val newCardSelection = selection.copy(shouldSavePaymentMethod = shouldSaveCard())
            sheetViewModel.updateSelection(newCardSelection)
            sheetViewModel.newCard = newCardSelection
        }
    }

    private fun shouldSaveCard() = saveCardCheckbox.isShown && saveCardCheckbox.isChecked

    internal class AddCardViewModel : ViewModel() {
        var isCardValid: Boolean = false

        val cardErrors = mutableMapOf<Field, String?>()

        enum class Field {
            Number,
            Date,
            Cvc
        }
    }
}
