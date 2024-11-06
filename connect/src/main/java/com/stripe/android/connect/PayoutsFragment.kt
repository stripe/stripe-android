package com.stripe.android.connect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PayoutsFragment internal constructor() : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stripe_payouts_fragment, container, false)
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(): PayoutsFragment {
            return PayoutsFragment()
        }
    }
}
