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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    fun `save mode hides manual capture`() = runScenario(
        initialState = NetworkedIdentityState.SavePending,
        mode = NetworkedIdentityMode.Save,
    ) {
        composeRule.onNodeWithTag(NI_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(NI_MANUAL_TAG).assertDoesNotExist()
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
    fun `share is disabled until a document is selected`() = runScenario(
        initialState = NetworkedIdentityState.SelectDocument(DOCUMENTS, selectedDocumentId = null),
    ) {
        composeRule.onNodeWithTag(NI_SHARE_TAG).assertIsNotEnabled()
    }

    @Test
    fun `share for a selected document needs a tap`() = runScenario(
        initialState = NetworkedIdentityState.SelectDocument(DOCUMENTS, selectedDocumentId = "license"),
    ) {
        shares.expectNoEvents()
        composeRule.onNodeWithTag(NI_SHARE_TAG).assertIsEnabled().performClick()
        shares.awaitItem()
    }

    @Test
    fun `shared document continues on tap without manual capture`() = runScenario(
        initialState = NetworkedIdentityState.DocumentShared(DOCUMENTS.first()),
    ) {
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertTextEquals("You shared your ID")
        composeRule.onNodeWithTag(NI_MANUAL_TAG).assertDoesNotExist()
        continues.expectNoEvents()
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).performClick()
        continues.awaitItem()
    }

    @Test
    fun `a prepared save says so and finishes on Done`() = runScenario(
        initialState = NetworkedIdentityState.SavePrepared,
        mode = NetworkedIdentityMode.Save,
    ) {
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertTextEquals("ID saved")
        composeRule.onNodeWithTag(NI_BODY_TAG)
            .assertTextEquals("It's encrypted and ready to reuse with your Link wallet.")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).assertTextEquals("Done")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).performClick()
        continues.awaitItem()
    }

    @Test
    fun `a prepared save with a selfie names both`() = runScenario(
        initialState = NetworkedIdentityState.SavePrepared,
        mode = NetworkedIdentityMode.Save,
        includesSelfie = true,
    ) {
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertTextEquals("ID and selfie saved")
        composeRule.onNodeWithTag(NI_BODY_TAG)
            .assertTextEquals("They are encrypted and ready to reuse with your Link wallet.")
    }

    @Test
    fun `skipping shows progress without offering manual capture again`() = runScenario(
        initialState = NetworkedIdentityState.SkipPending,
    ) {
        composeRule.onNodeWithTag(NI_LOADING_TAG).assertExists()
        composeRule.onNodeWithTag(NI_MANUAL_TAG).assertDoesNotExist()
    }

    @Test
    fun `manual capture remains reachable in a short viewport`() = runScenario(height = 360.dp) {
        composeRule.onNodeWithTag(NI_MANUAL_TAG).assertIsDisplayed().performClick()
        manualCapture.awaitItem()
    }

    @Test
    fun `save failure shows the details and closes on tap`() = runScenario(
        initialState = NetworkedIdentityState.SaveFailed(details = "Save failed. (resource_missing)"),
        mode = NetworkedIdentityMode.Save,
    ) {
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertTextEquals("Couldn't save your ID")
        composeRule.onNodeWithTag(NI_ERROR_TAG).assertTextEquals("Save failed. (resource_missing)")
        composeRule.onNodeWithTag(NI_CONTINUE_TAG).performClick()
        cancellations.awaitItem()
    }

    @Test
    fun `system back delegates cancellation`() = runScenario {
        composeRule.runOnIdle { backDispatcher.onBackPressed() }
        cancellations.awaitItem()
    }

    @Test
    fun `terminal state removes content and actions`() = runScenario {
        updateState(NetworkedIdentityState.Cancelled)
        composeRule.onNodeWithTag(NI_SHARE_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(NI_TITLE_TAG).assertDoesNotExist()
    }

    private fun runScenario(
        initialState: NetworkedIdentityState =
            NetworkedIdentityState.SelectDocument(DOCUMENTS, selectedDocumentId = "license"),
        mode: NetworkedIdentityMode = NetworkedIdentityMode.Reuse,
        includesSelfie: Boolean = false,
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
                        NetworkedIdentityScreen(
                            state = scenario.state,
                            mode = mode,
                            includesSelfie = includesSelfie,
                            actions = scenario.actions,
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
        lateinit var backDispatcher: OnBackPressedDispatcher
        val documentSelections = Turbine<String>()
        val shares = Turbine<Unit>()
        val continues = Turbine<Unit>()
        val manualCapture = Turbine<Unit>()
        val cancellations = Turbine<Unit>()

        val actions = NetworkedIdentityScreenActions(
            onSelectDocument = documentSelections::add,
            onShareDocument = { shares.add(Unit) },
            onContinue = { continues.add(Unit) },
            onManualCapture = { manualCapture.add(Unit) },
            onCancel = { cancellations.add(Unit) },
        )

        fun updateState(newState: NetworkedIdentityState) {
            composeRule.runOnIdle { state = newState }
            composeRule.waitForIdle()
        }

        fun ensureAllEventsConsumed() {
            documentSelections.ensureAllEventsConsumed()
            shares.ensureAllEventsConsumed()
            continues.ensureAllEventsConsumed()
            manualCapture.ensureAllEventsConsumed()
            cancellations.ensureAllEventsConsumed()
        }
    }

    private companion object {
        val DOCUMENTS = listOf(
            NetworkedIdentityDocument(
                id = "license",
                documentType = NetworkedIdentityDocumentType.DRIVING_LICENSE,
                redactedDocumentNumber = "•••• 4242",
                expirationDate = null,
                liveCaptured = true,
            ),
            NetworkedIdentityDocument(
                id = "passport",
                documentType = NetworkedIdentityDocumentType.PASSPORT,
                redactedDocumentNumber = "•••• 6789",
                expirationDate = null,
                liveCaptured = true,
            ),
        )
    }
}
