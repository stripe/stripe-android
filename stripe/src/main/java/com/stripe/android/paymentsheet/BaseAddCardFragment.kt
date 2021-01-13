package com.stripe.android.paymentsheet

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
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
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BillingAddressView
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.CardInputListener
import com.stripe.android.view.CardMultilineWidget
import com.stripe.android.view.StripeEditText

/**
 * A `Fragment` for adding new card payment method.
 */
internal abstract class BaseAddCardFragment(
    private val eventReporter: EventReporter
) : Fragment() {
    abstract val sheetViewModel: SheetViewModel<*, *>

    private var _viewBinding: FragmentPaymentsheetAddCardBinding? = null
    protected val viewBinding get() = requireNotNull(_viewBinding)

    @VisibleForTesting
    internal val cardMultilineWidget: CardMultilineWidget by lazy {
        viewBinding.cardMultilineWidget
    }

    @VisibleForTesting
    internal val billingAddressView: BillingAddressView by lazy {
        viewBinding.billingAddress
    }

    /**
     * A [PaymentMethodCreateParams] instance of card and billing address details are valid;
     * otherwise, `null`.
     */
    @VisibleForTesting
    internal val paymentMethodParams: PaymentMethodCreateParams?
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

    @VisibleForTesting
    internal val googlePayButton: View by lazy { viewBinding.googlePayButton }

    @VisibleForTesting
    internal val saveCardCheckbox: CheckBox by lazy { viewBinding.saveCardCheckbox }

    @VisibleForTesting
    internal val addCardHeader: TextView by lazy { viewBinding.addCardHeader }

    private val addCardViewModel: AddCardViewModel by viewModels()

    abstract fun onGooglePaySelected()

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

        if (activity == null) {
            return
        }

        _viewBinding = FragmentPaymentsheetAddCardBinding.bind(view)

        billingAddressView.level = sheetViewModel.config?.billingAddressCollection
            ?: PaymentSheet.BillingAddressCollectionLevel.Automatic

        setupCardWidget()

        cardMultilineWidget.expiryDateEditText.includeSeparatorGaps = true

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
            override fun onFocusChange(focusField: CardInputListener.FocusField) {
                // If the user focuses any card field, expand to full screen
                sheetViewModel.updateMode(SheetMode.Full)
            }

            override fun onCardComplete() {}

            override fun onExpirationComplete() {}

            override fun onCvcComplete() {
                // move to postal code when CVC is complete
                billingAddressView.postalCodeLayout.requestFocus()
            }
        })

        // If we're launched in full expanded mode, focus the card number field
        // and show the keyboard automatically
        if (sheetViewModel.sheetMode.value == SheetMode.Full) {
            cardMultilineWidget.cardNumberEditText.requestFocus()
            getSystemService(requireContext(), InputMethodManager::class.java)?.apply {
                showSoftInput(cardMultilineWidget.cardNumberEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            saveCardCheckbox.isEnabled = !isProcessing
            cardMultilineWidget.isEnabled = !isProcessing
            billingAddressView.isEnabled = !isProcessing
        }

        setupSaveCardCheckbox(saveCardCheckbox)

        sheetViewModel.fetchAddPaymentMethodConfig().observe(viewLifecycleOwner) { config ->
            if (config != null) {
                onConfigReady(config)
            }
        }

        sheetViewModel.selection.observe(viewLifecycleOwner) { paymentSelection ->
            if (paymentSelection == PaymentSelection.GooglePay) {
                onGooglePaySelected()
            }
        }

        eventReporter.onShowNewPaymentOptionForm()
    }

    private fun updateSelection() {
        sheetViewModel.updateSelection(
            if (addCardViewModel.isCardValid) {
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

        cardMultilineWidget.expirationDateHintRes = R.string.stripe_paymentsheet_expiration_date_hint
        cardMultilineWidget.expiryTextInputLayout.hint = getString(cardMultilineWidget.expirationDateHintRes)
        cardMultilineWidget.cvcEditText.imeOptions = EditorInfo.IME_ACTION_NEXT
        cardMultilineWidget.setBackgroundResource(R.drawable.stripe_paymentsheet_form_states)

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

        val layoutMarginHorizontal = resources.getDimensionPixelSize(R.dimen.stripe_paymentsheet_cardwidget_margin_horizontal)
        val layoutMarginVertical = resources.getDimensionPixelSize(R.dimen.stripe_paymentsheet_cardwidget_margin_vertical)
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
                onCardError(errorMessage)
            }
        cardMultilineWidget.expirationDateErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onCardError(errorMessage)
            }
        cardMultilineWidget.cvcErrorListener =
            StripeEditText.ErrorMessageListener { errorMessage ->
                onCardError(errorMessage)
            }
        cardMultilineWidget.postalCodeErrorListener = null
    }

    private fun onCardError(errorMessage: String?) {
        // TODO(mshafrir-stripe): render error message
    }

    private fun setupSaveCardCheckbox(saveCardCheckbox: CheckBox) {
        val merchantDisplayName = sheetViewModel.config?.merchantDisplayName.takeUnless {
            it.isNullOrBlank()
        }
        saveCardCheckbox.text = merchantDisplayName?.let {
            getString(R.string.stripe_paymentsheet_save_this_card_with_merchant_name, it)
        } ?: getString(R.string.stripe_paymentsheet_save_this_card_with_merchant_name)

        saveCardCheckbox.isVisible = sheetViewModel.customerConfig != null

        saveCardCheckbox.setOnCheckedChangeListener { _, _ ->
            onSaveCardCheckboxChanged()
        }
    }

    private fun onSaveCardCheckboxChanged() {
        val selection = sheetViewModel.selection.value
        if (selection is PaymentSelection.New.Card) {
            sheetViewModel.updateSelection(
                selection.copy(shouldSavePaymentMethod = shouldSaveCard())
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    open fun onConfigReady(config: AddPaymentMethodConfig) {
        val shouldShowGooglePayButton = config.shouldShowGooglePayButton
        googlePayButton.setOnClickListener {
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        }
        googlePayButton.isVisible = shouldShowGooglePayButton
        viewBinding.googlePayDivider.isVisible = shouldShowGooglePayButton
        addCardHeader.isVisible = !shouldShowGooglePayButton
    }

    private fun shouldSaveCard() = saveCardCheckbox.isShown && saveCardCheckbox.isChecked

    internal class AddCardViewModel : ViewModel() {
        var isCardValid: Boolean = false
    }
}
