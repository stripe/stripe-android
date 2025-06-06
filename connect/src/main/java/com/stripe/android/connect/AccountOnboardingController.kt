package com.stripe.android.connect

import androidx.fragment.app.FragmentActivity

/**
 * Controller for a full screen Account Onboarding component.
 */
class AccountOnboardingController internal constructor(
    activity: FragmentActivity,
    embeddedComponentManager: EmbeddedComponentManager,
    title: String? = null,
    props: AccountOnboardingProps? = null,
) : StripeComponentController<AccountOnboardingListener, AccountOnboardingProps>(
    dfClass = AccountOnboardingDialogFragment::class.java,
    activity = activity,
    embeddedComponentManager = embeddedComponentManager,
    title = title,
    props = props,
)
