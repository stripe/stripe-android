package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BasePaymentSheetActivity
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel

internal abstract class BasePaymentMethodsListFragment(
    private val canClickSelectedItem: Boolean,
    private val eventReporter: EventReporter
) : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
    abstract val sheetViewModel: SheetViewModel<*>

    protected lateinit var config: FragmentConfig

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nullableConfig = arguments?.getParcelable<FragmentConfig>(BasePaymentSheetActivity.EXTRA_FRAGMENT_CONFIG)
        if (nullableConfig == null) {
            sheetViewModel.onFatal(
                IllegalArgumentException("Failed to start existing payment options fragment.")
            )
            return
        }

        this.config = nullableConfig

        // reset the mode in case we're returning from the back stack
        sheetViewModel.updateMode(SheetMode.Wrapped)

        val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        viewBinding.recycler.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val adapter = PaymentOptionsAdapter(
            canClickSelectedItem,
            paymentOptionSelectedListener = ::onPaymentOptionSelected,
            addCardClickListener = {
                transitionToAddPaymentMethod()
            }
        ).also {
            viewBinding.recycler.adapter = it
        }

        adapter.update(
            config,
            sheetViewModel.newCard
        )

        eventReporter.onShowExistingPaymentOptions()

        viewBinding.header.text = createHeaderText()
    }

    abstract fun transitionToAddPaymentMethod()
    abstract fun createHeaderText(): String

    open fun onPaymentOptionSelected(
        paymentSelection: PaymentSelection,
        isClick: Boolean
    ) {
        sheetViewModel.updateSelection(paymentSelection)
    }
}
