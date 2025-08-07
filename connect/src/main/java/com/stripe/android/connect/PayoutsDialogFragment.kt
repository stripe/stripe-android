package com.stripe.android.connect

internal class PayoutsDialogFragment :
    StripeComponentDialogFragment<PayoutsView, PayoutsListener, PayoutsProps>() {

    override fun createComponentView(embeddedComponentManager: EmbeddedComponentManager): PayoutsView {
        return embeddedComponentManager.createPayoutsView(
            context = requireContext(),
            listener = listener,
            cacheKey = this.javaClass.simpleName
        )
    }
}
