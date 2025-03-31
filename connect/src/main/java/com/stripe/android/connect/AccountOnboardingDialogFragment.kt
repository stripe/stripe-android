package com.stripe.android.connect

@OptIn(PrivateBetaConnectSDK::class)
internal class AccountOnboardingDialogFragment :
    StripeComponentDialogFragment<AccountOnboardingView, AccountOnboardingListener, AccountOnboardingProps>() {

    override fun createComponentView(embeddedComponentManager: EmbeddedComponentManager): AccountOnboardingView {
        return embeddedComponentManager.createAccountOnboardingView(
            context = requireContext(),
            listener = listener,
            props = props,
            cacheKey = this.javaClass.simpleName
        )
    }
}
