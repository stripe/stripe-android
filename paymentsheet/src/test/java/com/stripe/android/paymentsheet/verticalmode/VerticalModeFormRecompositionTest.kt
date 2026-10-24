package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import android.view.Choreographer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.NoOpCardScanEventsReporter
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.assertHasErrorMessage
import com.stripe.paymentelementtestpages.assertHasNoErrorMessage
import dejavu.Dejavu
import dejavu.assertRecompositions
import dejavu.createRecompositionTrackingRule
import dejavu.getRecompositionCount
import dejavu.resetRecompositionCounts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowChoreographer

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.Q],
    shadows = [VerticalModeFormRecompositionTest.DelayedTrackingChoreographer::class],
)
internal class VerticalModeFormRecompositionTest {
    @get:Rule(order = 0)
    val trackingLifecycleRule = object : ExternalResource() {
        override fun after() {
            Dejavu.disable()
        }
    }

    @get:Rule(order = 1)
    val composeRule = createRecompositionTrackingRule()

    @get:Rule(order = 2)
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule(order = 3)
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @get:Rule(order = 4)
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    private val formPage = FormPage(composeRule)

    @Test
    fun `changing the header input is tracked`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        resetCounts()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                headerInformation = state.value.headerInformation!!.copy(
                    displayName = "Updated title".resolvableString,
                ),
            )
        }
        composeRule.waitForIdle()

        formPage.title.assertTextEquals("Updated title")
        formPage.title.assertRecompositions(atLeast = 1)
    }

    @Test
    fun `editing a focused card field keeps the header stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.performClick().performTextReplacement("424")
        composeRule.waitForIdle()
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FieldInteraction)
        resetCounts()

        formPage.cardNumber.performTextInput("2")
        composeRule.waitForIdle()

        formPage.cardNumber.assertTextContains("4242", substring = true)
        assertStableFormRecompositions()
    }

    @Test
    fun `entering processing disables fields with bounded recomposition`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.assertIsEnabled()
        formPage.expirationDate.assertIsEnabled()
        resetCounts()

        composeRule.runOnIdle { state.value = state.value.copy(isProcessing = true) }
        composeRule.waitForIdle()

        formPage.cardNumber.assertIsNotEnabled()
        formPage.expirationDate.assertIsNotEnabled()
        assertProcessingRecompositions()
    }

    @Test
    fun `leaving processing enables fields with bounded recomposition`() = runScenario(
        isProcessing = true,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.assertIsNotEnabled()
        formPage.expirationDate.assertIsNotEnabled()
        resetCounts()

        composeRule.runOnIdle { state.value = state.value.copy(isProcessing = false) }
        composeRule.waitForIdle()

        formPage.cardNumber.assertIsEnabled()
        formPage.expirationDate.assertIsEnabled()
        assertProcessingRecompositions()
    }

    @Test
    fun `requesting validation shows required errors and keeps the header stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.assertHasNoErrorMessage()
        formPage.expirationDate.assertHasNoErrorMessage()
        resetCounts()

        composeRule.runOnIdle { state.value = state.value.copy(isValidating = true) }
        composeRule.waitForIdle()

        formPage.cardNumber.assertHasErrorMessage("This field cannot be blank.")
        formPage.expirationDate.assertHasErrorMessage("This field cannot be blank.")
        formPage.title.assertTextEquals("Add new card")
        formPage.title.assertRecompositions(exactly = 0)
        // Validation updates controllers in place: only the screen observes the new state.
        // One screen pass is sufficient; the form container and header must remain skipped.
        composeRule.onNodeWithTag(FORM_TEST_TAG).assertRecompositions(atMost = 1)
        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertRecompositions(exactly = 0)
    }

    @Test
    fun `showing an incentive updates the header and keeps the form stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        composeRule.onNodeWithTag(TEST_TAG_HEADER_PROMO_BADGE).assertDoesNotExist()
        resetCounts()

        updatePromoBadge("5% back")

        assertPromoBadgeText("Get 5% back")
        assertHeaderUpdateRecompositions()
    }

    @Test
    fun `changing an incentive updates the header and keeps the form stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = "5% back",
    ) {
        assertPromoBadgeText("Get 5% back")
        resetCounts()

        updatePromoBadge("10% back")

        assertPromoBadgeText("Get 10% back")
        assertHeaderUpdateRecompositions()
    }

    @Test
    fun `removing an incentive updates the header and keeps the form stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = "5% back",
    ) {
        assertPromoBadgeText("Get 5% back")
        resetCounts()

        updatePromoBadge(null)

        composeRule.onNodeWithTag(TEST_TAG_HEADER_PROMO_BADGE).assertDoesNotExist()
        assertHeaderUpdateRecompositions()
    }

    @Test
    fun `correcting a field after validation clears its error and keeps the header stable`() = runScenario(
        isProcessing = false,
        isValidating = true,
        promoBadge = null,
    ) {
        formPage.cardNumber.performClick()
        composeRule.waitForIdle()
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FieldInteraction)
        formPage.cardNumber.assertHasErrorMessage("This field cannot be blank.")
        formPage.expirationDate.assertHasErrorMessage("This field cannot be blank.")
        resetCounts()

        formPage.cardNumber.performTextReplacement("4242424242424242")
        composeRule.waitForIdle()

        formPage.cardNumber.assertTextContains("4242424242424242")
        formPage.cardNumber.assertHasNoErrorMessage()
        formPage.expirationDate.assertHasErrorMessage("This field cannot be blank.")
        assertStableFormRecompositions()
    }

    @Test
    fun `completing the last required field reports valid values and keeps the header stable`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.performTextReplacement("4242424242424242")
        formPage.expirationDate.performTextReplacement("1234")
        composeRule.onNode(hasText("CVC")).performTextReplacement("123")
        val zipCode = composeRule.onNode(hasText("ZIP Code"))
        zipCode.performClick()
        composeRule.waitForIdle()
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FieldInteraction)
        assertThat(viewActionRecorder.viewActions).isEmpty()
        zipCode.assertIsFocused()
        resetCounts()

        zipCode.performTextReplacement("12345")
        composeRule.waitForIdle()

        zipCode.assertTextContains("12345")
        val action = viewActionRecorder.viewActions.single()
            as VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged
        val values = requireNotNull(action.formValues).fieldValuePairs
        assertThat(values[FormFieldId.CardNumber]?.value).isEqualTo("4242424242424242")
        assertThat(values[FormFieldId.CardExpMonth]?.value).isEqualTo("12")
        assertThat(values[FormFieldId.CardExpYear]?.value).isEqualTo("2034")
        assertThat(values[FormFieldId.CardCvc]?.value).isEqualTo("123")
        assertThat(values[FormFieldId.PostalCode]?.value).isEqualTo("12345")
        assertThat(values.values.all { it.isComplete }).isTrue()
        viewActionRecorder.consume(action)
        assertStableFormRecompositions()
    }

    @Test
    fun `entering processing with a focused field disables fields with bounded recomposition`() = runScenario(
        isProcessing = false,
        isValidating = false,
        promoBadge = null,
    ) {
        formPage.cardNumber.performClick().performTextReplacement("424")
        composeRule.waitForIdle()
        viewActionRecorder.consume(VerticalModeFormInteractor.ViewAction.FieldInteraction)
        formPage.cardNumber.assertIsFocused()
        formPage.cardNumber.assertIsEnabled()
        resetCounts()

        composeRule.runOnIdle { state.value = state.value.copy(isProcessing = true) }
        composeRule.waitForIdle()

        formPage.cardNumber.assertIsNotEnabled().assertTextContains("424")
        formPage.expirationDate.assertIsNotEnabled()
        assertProcessingRecompositions()
    }

    private fun assertPromoBadgeText(text: String) {
        composeRule.onNodeWithTag(TEST_TAG_HEADER_PROMO_BADGE)
            .onChildren().filter(hasText(text)).assertCountEquals(1)
    }

    private fun Scenario.updatePromoBadge(text: String?) {
        composeRule.runOnIdle {
            state.value = state.value.copy(
                headerInformation = state.value.headerInformation!!.copy(promoBadge = text),
            )
        }
        composeRule.waitForIdle()
    }

    private fun assertHeaderUpdateRecompositions() {
        // Each badge transition measured one screen/header pass. Updating header content
        // must not recompose the unchanged form, even when the badge is inserted or removed.
        formPage.title.assertTextEquals("Add new card")
        composeRule.onNodeWithTag(FORM_TEST_TAG).assertRecompositions(atLeast = 1, atMost = 1)
        formPage.title.assertRecompositions(atLeast = 1, atMost = 1)
        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertRecompositions(exactly = 0)
    }

    private fun assertStableFormRecompositions() {
        // Once initial focus settles, editing, clearing errors, and completing the form
        // measured zero passes here: those updates belong within the field components.
        formPage.title.assertTextEquals("Add new card")
        formPage.title.assertRecompositions(exactly = 0)
        composeRule.onNodeWithTag(FORM_TEST_TAG).assertRecompositions(exactly = 0)
        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertRecompositions(exactly = 0)
    }

    private fun assertProcessingRecompositions() {
        // Each processing transition measured one pass per region. These bounds prevent
        // extra screen/form work and repeated header alpha updates for a single state change.
        composeRule.onNodeWithTag(FORM_TEST_TAG).assertRecompositions(atLeast = 1, atMost = 1)
        composeRule.onNodeWithTag(FORM_ELEMENT_TEST_TAG).assertRecompositions(atLeast = 1, atMost = 1)
        formPage.title.assertRecompositions(atLeast = 1, atMost = 1)
    }

    private fun runScenario(
        isProcessing: Boolean,
        isValidating: Boolean,
        promoBadge: String?,
        block: Scenario.() -> Unit,
    ) {
        val initialState = createCardState()
        val state = MutableStateFlow(
            initialState.copy(
                isProcessing = isProcessing,
                isValidating = isValidating,
                headerInformation = initialState.headerInformation!!.copy(promoBadge = promoBadge),
            ),
        )
        val recorder = ViewActionRecorder<VerticalModeFormInteractor.ViewAction>()
        val interactor = object : VerticalModeFormInteractor {
            override val state: StateFlow<VerticalModeFormInteractor.State> = state

            override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
                recorder.record(viewAction)
            }

            override fun close() = Unit
        }
        try {
            composeRule.setContent {
                CompositionLocalProvider(
                    LocalCardScanEventsReporter provides NoOpCardScanEventsReporter,
                    LocalCardNumberCompletedEventReporter provides { },
                    LocalCardBrandDisallowedReporter provides { },
                ) {
                    VerticalModeFormUI(
                        interactor = interactor,
                        showsWalletHeader = false,
                        modifier = Modifier.testTag(FORM_TEST_TAG),
                    )
                }
            }
            formPage.waitUntilVisible()
            composeRule.waitForIdle()
            recorder.consume(VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged(null))
            assertTagMapping(FORM_TEST_TAG, "com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI")
            assertTagMapping(FORM_ELEMENT_TEST_TAG, "com.stripe.android.paymentsheet.ui.FormElement")
            assertTagMapping(
                TEST_TAG_HEADER_TITLE,
                "com.stripe.android.paymentsheet.verticalmode.VerticalModeFormHeaderUI",
            )
            Scenario(state, recorder).apply(block)
            assertThat(recorder.viewActions).isEmpty()
        } finally {
            // Also runs when setup or an assertion fails, before Compose disposes the content.
            Dejavu.disable()
        }
    }

    private fun resetCounts() {
        composeRule.waitForIdle()
        composeRule.resetRecompositionCounts()
    }

    private fun assertTagMapping(tag: String, composable: String) {
        composeRule.onNodeWithTag(tag).assertExists()
        // Dejavu exposes counts publicly but keeps its tag mapping internal. Check the mapping
        // explicitly so a zero count cannot silently refer to an unrelated composable.
        composeRule.getRecompositionCount(tag)
        val tracerClass = Class.forName("dejavu.internal.DejavuTracer")
        val tracer = tracerClass.getField("INSTANCE").get(null)
        val function = tracerClass.getMethod("getFunctionNameForTag", String::class.java).invoke(tracer, tag)
        assertThat(function).isEqualTo(composable)
    }

    private fun createCardState(): VerticalModeFormInteractor.State {
        val customUiDefinitionFactory = CardDefinition.uiDefinitionFactory(
            metadata = PaymentMethodMetadataFactory.create()
        ) as UiDefinitionFactory.Custom
        val headerInformation = customUiDefinitionFactory.createFormHeaderInformation(
            metadata = PaymentMethodMetadataFactory.create(),
            customerHasSavedPaymentMethods = true,
            incentive = null,
        )
        return VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code,
            isProcessing = false,
            usBankAccountFormArguments = mock(),
            formArguments = FormArguments(
                paymentMethodCode = PaymentMethod.Type.Card.code,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                merchantName = "Example, Inc.",
                amount = Amount(1000, "USD"),
                billingDetails = null,
                shippingDetails = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                hasIntentToSetup = false,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            ),
            formElements = CardDefinition.formElements(
                coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(Dispatchers.Unconfined)),
            ),
            isValidating = false,
            headerInformation = headerInformation,
        )
    }

    private data class Scenario(
        val state: MutableStateFlow<VerticalModeFormInteractor.State>,
        val viewActionRecorder: ViewActionRecorder<VerticalModeFormInteractor.ViewAction>,
    )

    // Robolectric dispatches frames immediately by default. Dejavu's self-posting callback
    // would keep ActivityScenario's main-looper drain running forever. Give only tracking
    // callbacks a frame delay, preserving the existing Compose/Robolectric synchronization.
    @Implements(Choreographer::class)
    class DelayedTrackingChoreographer : ShadowChoreographer() {
        @RealObject
        private lateinit var choreographer: Choreographer

        @Implementation
        fun postFrameCallback(callback: Choreographer.FrameCallback) {
            val delayMillis = if (callback.javaClass.name.startsWith("dejavu.")) 16L else 0L
            choreographer.postFrameCallbackDelayed(callback, delayMillis)
        }
    }

    private companion object {
        const val FORM_TEST_TAG = "vertical_mode_form_recomposition"
    }
}
