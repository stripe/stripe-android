package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BillingAddressView
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.CardInputListener
import com.stripe.android.view.CardMultilineWidget

/**
 * A `Fragment` for adding new card payment method.
 */
internal abstract class BaseAddCardFragment : Fragment() {
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

    @VisibleForTesting
    internal val paymentMethodParams: PaymentMethodCreateParams?
        get() {
            val cardParams = cardMultilineWidget.cardParams?.also { cardParams ->
                cardParams.address = billingAddressView.address
            }

            return cardParams?.let {
                PaymentMethodCreateParams.createCard(it)
            }
        }

    @VisibleForTesting
    internal val googlePayButton: View by lazy { viewBinding.googlePayButton }

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

        val saveCardCheckbox = viewBinding.saveCardCheckbox

        cardMultilineWidget.setCardValidCallback { isValid, _ ->
            val selection = if (isValid) {
                paymentMethodParams?.let { params ->
                    PaymentSelection.New.Card(
                        params,
                        cardMultilineWidget.brand
                    )
                }
            } else {
                null
            }
            sheetViewModel.updateSelection(selection)
        }

        cardMultilineWidget.setCardInputListener(object : CardInputListener {
            override fun onFocusChange(focusField: CardInputListener.FocusField) {
                // If the user focuses any card field, expand to full screen
                sheetViewModel.updateMode(SheetMode.Full)
            }

            override fun onCardComplete() {}

            override fun onExpirationComplete() {}

            override fun onCvcComplete() {}
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
    }

    private fun setupSaveCardCheckbox(saveCardCheckbox: CheckBox) {
        saveCardCheckbox.isVisible = sheetViewModel.customerConfig != null

        sheetViewModel.shouldSavePaymentMethod = saveCardCheckbox.isShown && saveCardCheckbox.isChecked
        saveCardCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sheetViewModel.shouldSavePaymentMethod = isChecked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    @VisibleForTesting
    fun onConfigReady(config: AddPaymentMethodConfig) {
        val shouldShowGooglePayButton = config.shouldShowGooglePayButton
        googlePayButton.setOnClickListener {
            sheetViewModel.updateSelection(PaymentSelection.GooglePay)
        }
        googlePayButton.isVisible = shouldShowGooglePayButton
        viewBinding.googlePayDivider.isVisible = shouldShowGooglePayButton
        viewBinding.addCardHeader.isVisible = !shouldShowGooglePayButton
    }
}
