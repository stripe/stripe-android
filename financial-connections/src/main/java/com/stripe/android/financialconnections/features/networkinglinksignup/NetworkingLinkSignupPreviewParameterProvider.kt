package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.PhoneNumberController

internal class NetworkingLinkSignupPreviewParameterProvider :
    PreviewParameterProvider<NetworkingLinkSignupState> {
    override val values = sequenceOf(
        default(),
        emailEntered()
    )

    internal fun default(): NetworkingLinkSignupState {
        return NetworkingLinkSignupState(
            payload = Success(
                NetworkingLinkSignupState.Payload(
                    merchantName = "Test",
                    emailController = EmailConfig.createController(""),
                    phoneController = PhoneNumberController.createPhoneNumberController(
                        initialValue = "",
                        initiallySelectedCountryCode = null,
                    ),
                    content = networkingLinkSignupPane()
                )
            ),
            validEmail = null,
            validPhone = null,
            lookupAccount = Uninitialized,
            saveAccountToLink = Uninitialized
        )
    }

    internal fun emailEntered(): NetworkingLinkSignupState {
        return NetworkingLinkSignupState(
            payload = Success(
                NetworkingLinkSignupState.Payload(
                    merchantName = "Test",
                    emailController = EmailConfig.createController("email"),
                    phoneController = PhoneNumberController.createPhoneNumberController(
                        initialValue = "",
                        initiallySelectedCountryCode = null,
                    ),
                    content = networkingLinkSignupPane()
                )
            ),
            validEmail = "test@test.com",
            validPhone = null,
            lookupAccount = Success(
                ConsumerSessionLookup(
                    exists = false,
                    consumerSession = null,
                    errorMessage = null
                )
            ),
            saveAccountToLink = Uninitialized
        )
    }

    private fun networkingLinkSignupPane() = NetworkingLinkSignupPane(
        aboveCta = "By saving your account to Link, you agree to Link’s Terms and Privacy Policy",
        body = NetworkingLinkSignupBody(
            listOf(
                Bullet(
                    title = "Connect your account faster on RandomBusiness and everywhere Link is accepted.",
                ),
                Bullet(
                    title = "Link encrypts your data and never shares your login details.",
                ),
            )
        ),
        cta = "Save to Link",
        skipCta = "Not now",
        title = "Save your account to Link"
    )
}
