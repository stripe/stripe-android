package com.stripe.android.connect

import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsFragment internal constructor() : Fragment() {

    companion object {
        @PrivateBetaConnectSDK
        fun create(): PayoutsFragment {
            return PayoutsFragment()
        }
    }
}
