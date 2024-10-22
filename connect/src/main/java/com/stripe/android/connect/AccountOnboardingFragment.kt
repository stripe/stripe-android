package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.RestrictTo

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AccountOnboardingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.stripe_account_onboarding_fragment, this)
    }

    fun registerListener(listener: AccountOnboardingFragmentListener) {

    }
}

/**
 * Interface used to communicate events from the Account Onboarding Component to the application.
 */
interface AccountOnboardingFragmentListener {
    /**
     * Triggered when an error occurs loading the payouts component.
     * [error] may be of type [EmbeddedComponentException] but is not guaranteed to be.
     * Developers are encouraged to check this type with as? or is to gain additional
     * context for the loading failure.
     * @param error The error that occurred when loading the component
     */
    fun onLoadFailure(error: Throwable)

    /**
     * The connected account has exited the onboarding process
     */
    fun onExit()
}

