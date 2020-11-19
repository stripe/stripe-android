package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddCardBinding
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import com.stripe.android.view.CardInputListener

/**
 * A `Fragment` for adding new card payment method.
 */
internal abstract class BaseAddCardFragment : Fragment(R.layout.fragment_paymentsheet_add_card) {
    abstract val sheetViewModel: SheetViewModel<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity == null) {
            return
        }

        val binding = FragmentPaymentsheetAddCardBinding.bind(view)
        val cardMultilineWidget = binding.cardMultilineWidget
        val saveCardCheckbox = binding.saveCardCheckbox

        cardMultilineWidget.setCardValidCallback { isValid, _ ->
            val selection = if (isValid) {
                cardMultilineWidget.paymentMethodCreateParams?.let {
                    PaymentSelection.New(it)
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
    }

    private fun setupSaveCardCheckbox(saveCardCheckbox: CheckBox) {
        saveCardCheckbox.visibility = when (sheetViewModel.isGuestMode) {
            true -> View.GONE
            false -> View.VISIBLE
        }

        sheetViewModel.shouldSavePaymentMethod = saveCardCheckbox.isShown && saveCardCheckbox.isChecked
        saveCardCheckbox.setOnCheckedChangeListener { _, isChecked ->
            sheetViewModel.shouldSavePaymentMethod = isChecked
        }
    }
}
