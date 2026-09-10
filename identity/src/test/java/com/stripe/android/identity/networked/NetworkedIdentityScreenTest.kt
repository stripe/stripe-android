package com.stripe.android.identity.networked

import android.os.Build
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.TestApplication
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalCoroutinesApi::class)
internal class NetworkedIdentityScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `blank email disables Continue`() = runScenario {
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(NI_EMAIL_TAG).assertIsEnabled()
    }

    @Test
    fun `invalid email disables Continue`() = runScenario {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performTextReplacement("jane@example")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsNotEnabled()
    }

    @Test
    fun `valid email enables Continue and submits typed email`() = runScenario {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performTextReplacement("jane@example.com")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsEnabled().performClick()
        assertThat(emailSubmissions.awaitItem()).isEqualTo("jane@example.com")
    }

    @Test
    fun `lookup disables fields and shows pending feedback`() = runScenario {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performTextReplacement("jane@example.com")
        updateState(NetworkedIdentityState.LookupPending)
        composeRule.onNodeWithTag(NI_EMAIL_TAG).assertIsNotEnabled().assertTextContains("jane@example.com")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(NI_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `OTP displays redacted destination`() = runScenario(initialState = awaitingOtp()) {
        composeRule.onNodeWithTag(NI_BODY_TAG).assertTextEquals("Enter the code sent to (***) *** **34.")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertDoesNotExist()
    }

    @Test
    fun `six digits submit once while partial code does not submit`() = runScenario(initialState = awaitingOtp()) {
        composeRule.onNodeWithTag("OTP-0").performTextInput("12a34")
        composeRule.onNodeWithTag("OTP-3").assertTextEquals("4")
        otpSubmissions.expectNoEvents()
        composeRule.onNodeWithTag("OTP-4").performTextInput("56")
        composeRule.onNodeWithTag("OTP-5").assertTextEquals("6")
        composeRule.waitForIdle()
        assertThat(otpSubmissions.awaitItem()).isEqualTo("123456")
        composeRule.waitForIdle()
        otpSubmissions.expectNoEvents()
    }

    @Test
    fun `invalid code is announced and accepts a fresh retry`() = runScenario(initialState = awaitingOtp()) {
        composeRule.onNodeWithTag("OTP-0").performTextInput("123456")
        composeRule.onNodeWithTag("OTP-5").assertTextEquals("6")
        composeRule.waitForIdle()
        assertThat(otpSubmissions.awaitItem()).isEqualTo("123456")
        updateState(NetworkedIdentityState.OtpConfirmPending("(***) *** **34", otpGeneration = 1))
        composeRule.onNodeWithTag("OTP-0").assertIsNotEnabled().assertTextEquals("1")
        updateState(awaitingOtp(invalidCode = true))
        otpSubmissions.expectNoEvents()
        composeRule.onNodeWithTag(NI_ERROR_TAG).assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)
        )
        composeRule.onNodeWithTag("OTP-0").assertIsEnabled().assertTextEquals("").performTextInput("654321")
        composeRule.onNodeWithTag("OTP-5").assertTextEquals("1")
        composeRule.waitForIdle()
        assertThat(otpSubmissions.awaitItem()).isEqualTo("654321")
    }

    @Test
    fun `OTP confirm disables digits with loading feedback`() = runScenario(
        initialState = NetworkedIdentityState.OtpConfirmPending("(***) *** **34", otpGeneration = 1),
    ) {
        composeRule.onNodeWithTag("OTP-0").assertIsNotEnabled()
        composeRule.onNodeWithTag(NI_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `reauthentication clears email and fresh OTP discards previous digits`() = runScenario {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performTextReplacement("jane@example.com")
        updateState(awaitingOtp())
        composeRule.onNodeWithTag("OTP-0").performTextInput("123")
        updateState(NetworkedIdentityState.ReauthenticationRequired)
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertTextEquals("Sign in again")
        composeRule.onNodeWithTag(NI_EMAIL_TAG).assertTextEquals("Email", "")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsNotEnabled()
        updateState(awaitingOtp(generation = 2))
        composeRule.onNodeWithTag("OTP-0").assertTextEquals("")
        composeRule.onNodeWithTag("OTP-1").assertTextEquals("")
        composeRule.onNodeWithTag("OTP-2").assertTextEquals("")
        otpSubmissions.expectNoEvents()
    }

    @Test
    fun `expiry restart generation clears partial OTP`() = runScenario(initialState = awaitingOtp()) {
        composeRule.onNodeWithTag("OTP-0").performTextInput("123")
        updateState(awaitingOtp(generation = 2))
        composeRule.onNodeWithTag("OTP-0").assertTextEquals("")
        composeRule.onNodeWithTag("OTP-1").assertTextEquals("")
    }

    @Test
    fun `document loading is visible`() = runScenario(initialState = NetworkedIdentityState.DocumentsPending) {
        composeRule.onNodeWithTag(NI_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NI_BODY_TAG).assertTextEquals("Loading your saved IDs…")
    }

    @Test
    fun `document selection can change and exposes selected semantics`() = runScenario(
        initialState = NetworkedIdentityState.SelectDocument(DOCUMENTS, selectedDocumentId = "license"),
    ) {
        composeRule.onNodeWithTag(NI_DOCUMENT_TAG_PREFIX + "license").assertIsSelected()
        composeRule.onNodeWithTag(NI_DOCUMENT_TAG_PREFIX + "passport")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performClick()
        assertThat(documentSelections.awaitItem()).isEqualTo("passport")
        updateState(NetworkedIdentityState.SelectDocument(DOCUMENTS, selectedDocumentId = "passport"))
        composeRule.onNodeWithTag(NI_DOCUMENT_TAG_PREFIX + "license").assertIsNotSelected()
        composeRule.onNodeWithTag(NI_DOCUMENT_TAG_PREFIX + "passport").assertIsSelected().assertIsFocused()
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertDoesNotExist()
    }

    @Test
    fun `manual capture remains reachable in a short viewport`() = runScenario(height = 360.dp) {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performScrollTo().performTextReplacement("jane@example.com")
        composeRule.onNodeWithTag(NI_MANUAL_TAG).assertIsDisplayed().performClick()
        manualCapture.awaitItem()
    }

    @Test
    fun `email IME Done clears focus without submitting`() = runScenario {
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performTextReplacement("jane@example.com")
        composeRule.onNodeWithTag(NI_EMAIL_TAG).performImeAction()
        emailSubmissions.expectNoEvents()
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertIsEnabled()
    }

    @Test
    fun `close delegates cancellation`() = runScenario {
        composeRule.onNodeWithTag(NI_CLOSE_TAG).performClick()
        cancellations.awaitItem()
    }

    @Test
    fun `system back delegates cancellation`() = runScenario {
        composeRule.runOnIdle { backDispatcher.onBackPressed() }
        cancellations.awaitItem()
    }

    @Test
    fun `leaving composition is not permanent cancellation`() = runScenario(initialState = awaitingOtp()) {
        composeRule.onNodeWithTag("OTP-0").performTextInput("123")
        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()
        cancellations.expectNoEvents()
        composeRule.runOnIdle { visible = true }
        composeRule.onNodeWithTag("OTP-0").assertTextEquals("")
        cancellations.expectNoEvents()
    }

    @Test
    fun `terminal state removes input and actions`() = runScenario(initialState = awaitingOtp()) {
        updateState(NetworkedIdentityState.Cancelled)
        composeRule.onNodeWithTag(NI_OTP_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(NI_CLOSE_TAG).assertDoesNotExist()
    }

    private fun runScenario(
        initialState: NetworkedIdentityState = NetworkedIdentityState.CollectEmail,
        height: Dp = 700.dp,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val scenario = Scenario(initialState)
        composeRule.setContent {
            scenario.backDispatcher = requireNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            StripeTheme {
                // StripeTheme marks Robolectric as inspection mode. Exercise runtime focus behavior
                // here; visual tests retain inspection mode so previews never open the keyboard.
                CompositionLocalProvider(LocalInspectionMode provides false) {
                    Box(Modifier.height(height)) {
                        if (!scenario.visible) return@Box
                        NetworkedIdentityScreen(
                            state = scenario.state,
                            onSubmitEmail = scenario.emailSubmissions::add,
                            onSubmitOtp = scenario.otpSubmissions::add,
                            onSelectDocument = scenario.documentSelections::add,
                            onManualCapture = { scenario.manualCapture.add(Unit) },
                            onCancel = { scenario.cancellations.add(Unit) },
                        )
                    }
                }
            }
        }
        scenario.block()
        scenario.ensureAllEventsConsumed()
    }

    private inner class Scenario(initialState: NetworkedIdentityState) {
        var state by mutableStateOf(initialState)
        var visible by mutableStateOf(true)
        lateinit var backDispatcher: OnBackPressedDispatcher
        val emailSubmissions = Turbine<String>()
        val otpSubmissions = Turbine<String>()
        val documentSelections = Turbine<String>()
        val manualCapture = Turbine<Unit>()
        val cancellations = Turbine<Unit>()

        fun updateState(newState: NetworkedIdentityState) {
            composeRule.runOnIdle { state = newState }
            composeRule.waitForIdle()
        }

        fun ensureAllEventsConsumed() {
            emailSubmissions.ensureAllEventsConsumed()
            otpSubmissions.ensureAllEventsConsumed()
            documentSelections.ensureAllEventsConsumed()
            manualCapture.ensureAllEventsConsumed()
            cancellations.ensureAllEventsConsumed()
        }
    }

    private companion object {
        fun awaitingOtp(invalidCode: Boolean = false, generation: Int = 1) = NetworkedIdentityState.AwaitingOtp(
            redactedPhoneNumber = "(***) *** **34",
            invalidCode = invalidCode,
            otpGeneration = generation,
        )

        val DOCUMENTS = listOf(
            NetworkedIdentityDocument(
                id = "license",
                documentType = NetworkedIdentityDocumentType.DRIVING_LICENSE,
                created = 1,
                country = "US",
                region = "CA",
                redactedDocumentNumber = "•••• 4242",
                expirationDate = null,
                liveCaptured = true,
            ),
            NetworkedIdentityDocument(
                id = "passport",
                documentType = NetworkedIdentityDocumentType.PASSPORT,
                created = 1,
                country = "US",
                region = null,
                redactedDocumentNumber = "•••• 6789",
                expirationDate = null,
                liveCaptured = true,
            ),
        )
    }
}
