package com.stripe.android.link.ui.wallet

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.link.ui.verification.VERIFICATION_HEADER_IMAGE_TAG
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.model.LinkBrand
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class LinkInline2FASectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun headerImage_hasDynamicBrandContentDescription() {
        composeRule.setContent {
            LinkInline2FASection(
                verificationState = VerificationViewState(
                    isProcessing = false,
                    requestFocus = true,
                    errorMessage = null,
                    isSendingNewCode = false,
                    didSendNewCode = false,
                    redactedPhoneNumber = "***-***-1234",
                    email = "user@example.com",
                    defaultPayment = null,
                    isDialog = false,
                    allowLogout = true,
                    linkBrand = LinkBrand.Notlink,
                ),
                otpElement = OTPElement(
                    identifier = IdentifierSpec.Generic("otp"),
                    controller = OTPController(),
                ),
                onResend = {},
            )
        }

        composeRule
            .onNodeWithTag(VERIFICATION_HEADER_IMAGE_TAG)
            .assertContentDescriptionContains("Notlink")
    }
}
