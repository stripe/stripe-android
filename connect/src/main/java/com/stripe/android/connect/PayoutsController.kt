package com.stripe.android.connect

import androidx.fragment.app.FragmentActivity

/**
 * Controller for a full screen Payouts component.
 */
class PayoutsController internal constructor(
    activity: FragmentActivity,
    embeddedComponentManager: EmbeddedComponentManager,
    title: String? = null,
    props: PayoutsProps? = null,
) : StripeComponentController<PayoutsListener, PayoutsProps>(
    dfClass = PayoutsDialogFragment::class.java,
    activity = activity,
    embeddedComponentManager = embeddedComponentManager,
    title = title,
    props = props,
)
