package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels

internal class PaymentOptionsListFragment() : BasePaymentMethodsListFragment(
    canClickSelectedItem = true
) {
    private val activityViewModel by activityViewModels<PaymentOptionsViewModel> {
        PaymentOptionsViewModel.Factory {
            requireNotNull(
                requireArguments().getParcelable(PaymentOptionsActivity.EXTRA_STARTER_ARGS)
            )
        }
    }

    override val sheetViewModel: PaymentOptionsViewModel by lazy { activityViewModel }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // We need to make sure the list fragment is attached before jumping, so the
        // list is properly added to the backstack.
        sheetViewModel.resolveTransitionTarget(config)
    }

    override fun onResume() {
        super.onResume()

        sheetViewModel.headerText.value =
            getString(R.string.stripe_paymentsheet_select_payment_method)
    }

    override fun transitionToAddPaymentMethod() {
        /**
         * Only the [PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull] will add
         * the previous fragment to the back stack creating the needed back button after jumping
         * through the the last unsaved card.
         */
        activityViewModel.transitionTo(
            PaymentOptionsViewModel.TransitionTarget.AddPaymentMethodFull(config)
        )
    }

    override fun onPaymentOptionsItemSelected(item: PaymentOptionsItem) {
        super.onPaymentOptionsItemSelected(item)
        sheetViewModel.onUserSelection()
    }
}
