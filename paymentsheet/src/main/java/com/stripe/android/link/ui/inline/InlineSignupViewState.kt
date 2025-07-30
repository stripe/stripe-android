package com.stripe.android.link.ui.inline

import androidx.annotation.VisibleForTesting
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.requiresNameCollection

/**
 * The LinkInlineSignup view state.
 *
 * @param userInput The collected input from the user, always valid unless null.
 * @param isExpanded Whether the checkbox is selected and the view is expanded.
 * @param apiFailed Whether an API call has failed. In such cases, we want to continue the
 *                  payment flow without Link.
 * @param signUpState The stage of the sign in or sign up flow.
 */
internal data class InlineSignupViewState
@VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
constructor(
    val userInput: UserInput?,
    val merchantName: String,
    val signupMode: LinkSignupMode?,
    val fields: List<LinkSignupField>,
    val prefillEligibleFields: Set<LinkSignupField>,
    val allowsDefaultOptIn: Boolean,
    val linkSignUpOptInFeatureEnabled: Boolean,
    val didAskToChangeSignupDetails: Boolean = false,
    internal val isExpanded: Boolean = false,
    internal val apiFailed: Boolean = false,
    internal val signUpState: SignUpState = SignUpState.InputtingPrimaryField,
) {

    val isShowingPhoneFirst: Boolean
        get() = fields.first() == LinkSignupField.Phone

    val isShowingEmailFirst: Boolean
        get() = fields.first() == LinkSignupField.Email

    /**
     * Whether the view is active and the payment should be processed through Link.
     */
    val useLink: Boolean
        get() {
            if (apiFailed) {
                return false
            }

            return when (signupMode) {
                LinkSignupMode.AlongsideSaveForFutureUse -> userInput != null
                LinkSignupMode.InsteadOfSaveForFutureUse -> {
                    if (allowsDefaultOptIn) {
                        userInput != null
                    } else {
                        isExpanded
                    }
                }
                null -> false
            }
        }

    companion object {

        fun create(
            signupMode: LinkSignupMode,
            config: LinkConfiguration,
            initialEmail: String? = null,
            initialPhone: String? = null,
            isExpanded: Boolean = false,
        ): InlineSignupViewState {
            val isAlternativeFlow = signupMode == LinkSignupMode.AlongsideSaveForFutureUse
            val customer = config.customerInfo

            val fields = buildList {
                val hasPrefilledEmail = !customer.email.isNullOrBlank()

                if (isAlternativeFlow && hasPrefilledEmail) {
                    add(LinkSignupField.Phone)
                    add(LinkSignupField.Email)
                } else if (isAlternativeFlow) {
                    add(LinkSignupField.Email)
                    add(LinkSignupField.Phone)
                } else {
                    add(LinkSignupField.Email)
                    add(LinkSignupField.Phone)
                }

                if (config.requiresNameCollection) {
                    add(LinkSignupField.Name)
                }
            }

            val prefillEligibleFields = when (signupMode) {
                LinkSignupMode.InsteadOfSaveForFutureUse -> {
                    fields.toSet()
                }
                LinkSignupMode.AlongsideSaveForFutureUse -> {
                    // We can't prefill all fields, as this might lead to Link account creation without explicit
                    // user consent. We don't prefill the first field in this case.
                    fields.toSet() - fields.first()
                }
            }

            val allowsDefaultOptIn = allowsDefaultOptIn(config, signupMode)
            val missingDataForDefaultOptIn = initialEmail.isNullOrBlank() || initialPhone.isNullOrBlank()

            val signupState = if (allowsDefaultOptIn && missingDataForDefaultOptIn) {
                SignUpState.InputtingRemainingFields
            } else {
                SignUpState.InputtingPrimaryField
            }

            return InlineSignupViewState(
                userInput = null,
                merchantName = config.merchantName,
                signupMode = signupMode,
                fields = fields,
                prefillEligibleFields = prefillEligibleFields,
                isExpanded = isExpanded || allowsDefaultOptIn,
                allowsDefaultOptIn = allowsDefaultOptIn(config, signupMode),
                linkSignUpOptInFeatureEnabled = linkSignupOptInFeatureEnabled(config, signupMode),
                signUpState = signupState,
            )
        }

        private fun allowsDefaultOptIn(
            config: LinkConfiguration,
            signupMode: LinkSignupMode
        ): Boolean = config.allowDefaultOptIn &&
            config.stripeIntent.countryCode == "US" &&
            signupMode == LinkSignupMode.InsteadOfSaveForFutureUse

        private fun linkSignupOptInFeatureEnabled(
            config: LinkConfiguration,
            signupMode: LinkSignupMode
        ): Boolean = config.linkSignUpOptInFeatureEnabled &&
            config.stripeIntent.countryCode == "US" &&
            // We only allow the opt-in feature if the user has provided an email address.
            config.customerInfo.email.isNullOrBlank().not() &&
            signupMode == LinkSignupMode.InsteadOfSaveForFutureUse
    }
}

internal enum class LinkSignupMode {
    InsteadOfSaveForFutureUse,
    AlongsideSaveForFutureUse,
}

internal enum class LinkSignupField {
    Email,
    Phone,
    Name,
}
