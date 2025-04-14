package com.stripe.android.link.ui.signup

import androidx.compose.runtime.Immutable
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.signup.SignUpState.InputtingPrimaryField
import com.stripe.android.link.ui.signup.SignUpState.InputtingRemainingFields

@Immutable
internal data class SignUpScreenState(
    val merchantName: String,
    val signUpEnabled: Boolean,
    val requiresNameCollection: Boolean,
    val signUpState: SignUpState = InputtingPrimaryField,
    val isSubmitting: Boolean = false,
    val errorMessage: ResolvableString? = null,
) {
    val canEditForm: Boolean
        get() = !isSubmitting

    internal companion object {

        fun create(
            configuration: LinkConfiguration,
            customerInfo: LinkConfiguration.CustomerInfo?,
        ): SignUpScreenState {
            val showKeyboardOnOpen = customerInfo == null || customerInfo.email.isNullOrBlank()
            val signUpState = if (showKeyboardOnOpen) InputtingPrimaryField else InputtingRemainingFields
            val signupEnabled = customerInfo?.isComplete(configuration.requiresNameCollection) == true

            return SignUpScreenState(
                signUpEnabled = signupEnabled,
                merchantName = configuration.merchantName,
                requiresNameCollection = configuration.requiresNameCollection,
                signUpState = signUpState,
            )
        }
    }
}

internal val LinkConfiguration.requiresNameCollection: Boolean
    get() = stripeIntent.countryCode != CountryCode.US.value

private fun LinkConfiguration.CustomerInfo.isComplete(requiresNameCollection: Boolean): Boolean {
    return !email.isNullOrBlank() && !phone.isNullOrBlank() && (!requiresNameCollection || !name.isNullOrBlank())
}
