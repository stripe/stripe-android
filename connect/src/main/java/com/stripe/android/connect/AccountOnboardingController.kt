package com.stripe.android.connect

import androidx.fragment.app.FragmentActivity

@PrivateBetaConnectSDK
class AccountOnboardingController internal constructor(
    activity: FragmentActivity,
    embeddedComponentManager: EmbeddedComponentManager,
    props: AccountOnboardingProps? = null,
    cacheKey: String? = null,
) : StripeComponentController<AccountOnboardingListener> by StripeComponentControllerImpl(
    cls = AccountOnboardingDialogFragment::class.java,
    activity = activity,
    embeddedComponentManager = embeddedComponentManager,
    props = props,
    cacheKey = cacheKey
)
