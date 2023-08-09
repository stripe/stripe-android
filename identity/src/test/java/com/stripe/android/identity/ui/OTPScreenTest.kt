package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.OTPDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.PhoneParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageStaticContentOTPPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.OTPViewModel
import com.stripe.android.identity.viewmodel.OTPViewState
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class OTPScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val verificationPageOTP = mock<VerificationPage>().also {
        whenever(it.phoneOtp).thenReturn(
            VerificationPageStaticContentOTPPage(
                title = TITLE,
                body = BODY,
                redactedPhoneNumber = REDACTED_PHONE_NUMBER,
                errorOtpMessage = ERROR_OTP_MESSAGE,
                resendButtonText = RESEND_BUTTON_TEXT,
                cannotVerifyButtonText = CANNOT_VERIFY_BUTTON_TEXT
            )
        )
    }

    private val verificationPageOTPWithoutPhoneNumber = mock<VerificationPage>().also {
        whenever(it.phoneOtp).thenReturn(
            VerificationPageStaticContentOTPPage(
                title = TITLE,
                body = BODY,
                errorOtpMessage = ERROR_OTP_MESSAGE,
                redactedPhoneNumber = null,
                resendButtonText = RESEND_BUTTON_TEXT,
                cannotVerifyButtonText = CANNOT_VERIFY_BUTTON_TEXT
            )
        )
    }

    private val verificationPageData = MutableLiveData(
        Resource.success(verificationPageOTP)
    )

    private val mockErrorCause = mock<MutableLiveData<Throwable>>()

    private val collectedDataFlow = MutableStateFlow(CollectedDataParam())
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageData
        on { collectedData } doReturn collectedDataFlow
        on { errorCause } doReturn mockErrorCause
    }

    private val otpViewState = MutableStateFlow<OTPViewState>(OTPViewState.InputtingOTP)
    private val mockOtpViewModel = mock<OTPViewModel> {
        on { viewState } doReturn otpViewState
        on { otpElement } doReturn OTPElement(
            identifier = IdentifierSpec.Generic(OTPViewModel.OTP),
            controller = OTPController()
        )
    }
    private val mockNavController = mock<NavController>()

    @Test
    fun verifyUIWithRedactedPhoneNumber() {
        setComposeTestRuleWith {
            onNodeWithTag(OTP_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(OTP_BODY_TAG).assertTextEquals(
                BODY.replace(
                    PHONE_NUMBER_PATTERN,
                    REDACTED_PHONE_NUMBER
                )
            )
            onNodeWithTag(OTP_ELEMENT_TAG).assertExists()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(RESEND_BUTTON_TEXT.uppercase())
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(CANNOT_VERIFY_BUTTON_TEXT.uppercase())
        }
    }

    @Test
    fun verifyUIWithoutRedactedPhoneNumber() {
        verificationPageData.postValue(
            Resource.success(verificationPageOTPWithoutPhoneNumber)
        )
        collectedDataFlow.update {
            it.copy(
                phone = PhoneParam(
                    phoneNumber = LOCALLY_COLLECTED_PHONE
                )
            )
        }
        setComposeTestRuleWith {
            onNodeWithTag(OTP_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(OTP_BODY_TAG).assertTextEquals(
                BODY.replace(
                    PHONE_NUMBER_PATTERN,
                    LOCALLY_COLLECTED_PHONE.takeLast(4)
                )
            )
            onNodeWithTag(OTP_ELEMENT_TAG).assertExists()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(RESEND_BUTTON_TEXT.uppercase())
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0)
                .assertTextEquals(CANNOT_VERIFY_BUTTON_TEXT.uppercase())
        }
    }

    @Test
    fun verifyInputtingOTPState() {
        otpViewState.update { OTPViewState.InputtingOTP }

        setComposeTestRuleWith {
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsEnabled()
        }
    }

    @Test
    fun verifySubmittingOTPState() {
        otpViewState.update { OTPViewState.SubmittingOTP(OTP) }

        setComposeTestRuleWith {
            verify(mockIdentityViewModel).postVerificationPageDataForOTP(
                otp = eq(OTP),
                navController = same(mockNavController),
                onMissingOtp = any()
            )
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyErrorOTPState() {
        otpViewState.update { OTPViewState.ErrorOTP }
        setComposeTestRuleWith {
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertExists()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsEnabled()
        }
    }

    @Test
    fun verifyRequestingOTPState() {
        otpViewState.update { OTPViewState.RequestingOTP }
        setComposeTestRuleWith {
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyRequestingCannotVerifyState() {
        otpViewState.update { OTPViewState.RequestingCannotVerify }
        setComposeTestRuleWith {
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyRequestingCannotVerifySuccessState() {
        val verificationPageData = mock<VerificationPageData>()
        otpViewState.update { OTPViewState.RequestingCannotVerifySuccess(verificationPageData) }
        setComposeTestRuleWith {
            verify(mockIdentityViewModel).updateStatesWithVerificationPageData(
                eq(OTPDestination.ROUTE.route),
                same(verificationPageData),
                same(mockNavController),
                any()
            )
            onNodeWithTag(OTP_ELEMENT_TAG).onChildAt(0).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_ERROR_TAG).assertDoesNotExist()
            onNodeWithTag(OTP_RESEND_BUTTON_TAG).onChildAt(0).assertIsEnabled()
            onNodeWithTag(OTP_CANNOT_VERIFY_BUTTON_TAG).onChildAt(0).assertIsEnabled()
        }
    }

    @Test
    fun verifyRequestingErrorState() {
        val cause = Throwable()
        otpViewState.update { OTPViewState.RequestingError(cause) }
        setComposeTestRuleWith {
            verify(mockErrorCause).postValue(same(cause))
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(ErrorDestination.ERROR)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun setComposeTestRuleWith(
        testBlock: suspend ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            OTPScreen(
                mockNavController,
                mockIdentityViewModel,
                viewModelFactoryFor(mockOtpViewModel)
            )
        }

        runBlocking {
            composeTestRule.testBlock()
        }
    }

    private companion object {
        private const val OTP = "123456"
        private const val TITLE = "Title"
        private const val BODY = "body with $PHONE_NUMBER_PATTERN"
        private const val REDACTED_PHONE_NUMBER = "1234"
        private const val ERROR_OTP_MESSAGE = "Error OTP!"
        private const val RESEND_BUTTON_TEXT = "Resend"
        private const val CANNOT_VERIFY_BUTTON_TEXT = "Cannot verify"
        private const val LOCALLY_COLLECTED_PHONE = "4151234567"
    }
}
