package com.stripe.android.financialconnections.features.networkinglinksignup

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.LinkLoginPane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.PhoneNumberController

internal class NetworkingLinkSignupPreviewParameterProvider :
    PreviewParameterProvider<NetworkingLinkSignupState> {
    override val values = sequenceOf(
        default(),
        emailEntered(),
        invalidEmail(),
        instantDebits(),
    )

    private fun default() = NetworkingLinkSignupState(
        payload = Success(
            NetworkingLinkSignupState.Payload(
                merchantName = "Test",
                emailController = EmailConfig.createController(""),
                appVerificationEnabled = false,
                prefilledEmail = null,
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = "",
                    initiallySelectedCountryCode = null,
                ),
                isInstantDebits = false,
                content = networkingLinkSignupPane(),
                sessionId = "fcsess_1234",
            )
        ),
        validEmail = null,
        validPhone = null,
        lookupAccount = Uninitialized,
        saveAccountToLink = Uninitialized
    )

    private fun emailEntered() = NetworkingLinkSignupState(
        payload = Success(
            NetworkingLinkSignupState.Payload(
                merchantName = "Test",
                emailController = EmailConfig.createController("valid@email.com"),
                appVerificationEnabled = false,
                prefilledEmail = null,
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = "",
                    initiallySelectedCountryCode = null,
                ),
                isInstantDebits = false,
                content = networkingLinkSignupPane(),
                sessionId = "fcsess_1234",
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

    private fun invalidEmail() = NetworkingLinkSignupState(
        payload = Success(
            NetworkingLinkSignupState.Payload(
                merchantName = "Test",
                emailController = EmailConfig.createController("invalid_email.com"),
                appVerificationEnabled = false,
                prefilledEmail = null,
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = "",
                    initiallySelectedCountryCode = null,
                ),
                isInstantDebits = false,
                content = networkingLinkSignupPane(),
                sessionId = "fcsess_1234",
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

    private fun instantDebits() = NetworkingLinkSignupState(
        payload = Success(
            NetworkingLinkSignupState.Payload(
                merchantName = "Test",
                emailController = EmailConfig.createController(initialValue = null),
                appVerificationEnabled = false,
                prefilledEmail = null,
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = "",
                    initiallySelectedCountryCode = null,
                ),
                isInstantDebits = true,
                content = linkLoginPane(),
                sessionId = "fcsess_1234",
            )
        ),
        validEmail = null,
        validPhone = null,
        lookupAccount = Uninitialized,
        saveAccountToLink = Uninitialized,
        isInstantDebits = true,
    )

    private fun networkingLinkSignupPane() = NetworkingLinkSignupPane(
        aboveCta = "By saving your account to Link, you agree to Link’s Terms and Privacy Policy",
        body = NetworkingLinkSignupBody(
            listOf(
                Bullet(
                    title = "Connect your account faster everywhere Link is accepted.",
                ),
                Bullet(
                    title = "Link encrypts your data and never shares your login details.",
                ),
            )
        ),
        cta = "Save with Link",
        skipCta = "Not now",
        title = "Save account with Link"
    ).toContent()

    private fun linkLoginPane() = LinkLoginPane(
        title = "Sign up or log in",
        body = "Connect your account to RandomBusiness using Link.",
        aboveCta = "By using Link, you authorize debits under these Terms.",
        cta = "Continue with Link",
    ).toContent()
}
