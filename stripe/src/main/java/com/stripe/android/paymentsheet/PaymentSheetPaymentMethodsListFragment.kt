package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.activityViewModels
import com.stripe.android.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import java.util.Currency
import java.util.Locale

internal class PaymentSheetPaymentMethodsListFragment(
    eventReporter: EventReporter
) : BasePaymentMethodsListFragment(eventReporter) {
    private val activityViewModel by activityViewModels<PaymentSheetViewModel> {
        PaymentSheetViewModel.Factory(
            { requireActivity().application },
            {
                requireNotNull(
                    requireArguments().getParcelable(PaymentSheetActivity.EXTRA_STARTER_ARGS)
                )
            }
        )
    }

    override val sheetViewModel: PaymentSheetViewModel by lazy { activityViewModel }

    private val currencyFormatter = CurrencyFormatter()

    internal val header: TextView by lazy { viewBinding.header }

    override fun transitionToAddPaymentMethod() {
        activityViewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Only fetch the payment methods list if we haven't already
        if (activityViewModel.paymentMethods.value == null) {
            activityViewModel.updatePaymentMethods()
        }

        viewBinding.header.setText(R.string.stripe_paymentsheet_pay_using)
        activityViewModel.paymentIntent
            .observe(viewLifecycleOwner) { paymentIntent ->
                if (paymentIntent == null) {
                    // ignore null
                } else if (paymentIntent.currency != null && paymentIntent.amount != null) {
                    updateHeader(paymentIntent.amount, paymentIntent.currency)
                }
            }
    }

    @VisibleForTesting
    internal fun updateHeader(
        amount: Long,
        currencyCode: String
    ) {
        val currency = Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT))
        header.text = getString(
            R.string.stripe_paymentsheet_pay_using_with_amount,
            currencyFormatter.format(amount, currency)
        )
    }
}
