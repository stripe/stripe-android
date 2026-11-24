package com.stripe.android.link.ui.wallet

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.link.ui.verification.VERIFICATION_HEADER_IMAGE_TAG
import com.stripe.android.link.ui.verification.VERIFICATION_SANDBOX_BANNER_TAG
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.model.LinkBrand
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun headerImage_hasDynamicBrandContentDescription() = runScenario {
        composeRule.onNodeWithTag(VERIFICATION_HEADER_IMAGE_TAG)
            .assertContentDescriptionContains("Onelink")
    }

    @Test
    fun `sandbox inline login shows test code instructions`() = runScenario(isTestMode = true) {
        composeRule.onNodeWithTag(VERIFICATION_SANDBOX_BANNER_TAG).assertIsDisplayed()
    }

    @Test
    fun `live inline login hides test code instructions`() = runScenario {
        composeRule.onNodeWithTag(VERIFICATION_SANDBOX_BANNER_TAG).assertDoesNotExist()
    }

    private fun runScenario(isTestMode: Boolean = false, block: () -> Unit) {
        composeRule.setContent {
            LinkInline2FASection(
                verificationState = VerificationViewState(
                    isTestMode = isTestMode,
                    authFlow = null,
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
                    linkBrand = LinkBrand.Onelink,
                ),
                otpElement = OTPElement(
                    identifier = FormFieldId.Generic("otp"),
                    controller = OTPController(),
                ),
                onResend = {},
            )
        }

        block()
    }
}
