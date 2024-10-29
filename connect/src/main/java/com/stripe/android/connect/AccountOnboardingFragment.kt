package com.stripe.android.connect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AccountOnboardingFragment internal constructor() : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stripe_account_onboarding_fragment, container, false)
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @PrivateBetaConnectSDK
        fun create(): AccountOnboardingFragment {
            return AccountOnboardingFragment()
        }
    }
}
