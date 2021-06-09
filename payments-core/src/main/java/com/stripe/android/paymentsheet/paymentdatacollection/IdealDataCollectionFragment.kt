package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddIdealBinding

internal class IdealDataCollectionFragment : BasePaymentDataCollectionFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val themedInflater = inflater.cloneInContext(
            ContextThemeWrapper(requireActivity(), R.style.StripePaymentSheetAddPaymentMethodTheme)
        )
        return themedInflater.inflate(
            R.layout.fragment_paymentsheet_add_ideal,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = FragmentPaymentsheetAddIdealBinding.bind(view)

        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf(
                "Select", "Bank 1", "Bank 2", "Bank 3"
            )
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            viewBinding.spinner.adapter = adapter
        }
    }
}
