package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import com.stripe.android.paymentelement.embedded.PreviousNewSelections
import com.stripe.android.paymentelement.embedded.toArgs
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityCoordinatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `construction creates the presentation`() = runScenario {
        assertThat(createCall.activity).isSameInstanceAs(activity)
        assertThat(createCall.state).isEqualTo(state)
        assertThat(createCall.activityResultCaller).isSameInstanceAs(activity)
    }

    @Test
    fun `register delegates to presentation`() = runScenario {
        coordinator.register()

        assertThat(initialPresentation.registerCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `canDismiss delegates to presentation`() = runScenario {
        initialPresentation.canDismissResult = false

        assertThat(coordinator.canDismiss()).isFalse()
        assertThat(initialPresentation.canDismissCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `onDismissed delegates to presentation`() = runScenario {
        coordinator.onDismissed()

        assertThat(initialPresentation.onDismissedCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `Content delegates to presentation`() = runScenario {
        composeRule.setContent {
            coordinator.Content()
        }

        composeRule.onNodeWithTag(CONTENT_TEST_TAG).assertExists()
    }

    @Test
    fun `onDestroy delegates to presentation`() = runScenario {
        coordinator.onDestroy()

        assertThat(initialPresentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `valid loading to ready transition replaces and registers presentation`() = runScenario {
        val readyState = createReadyState(
            callbackIdentifier = "updated_callback_identifier",
        )
        val readyIntent = createIntent(readyState)

        coordinator.handleNewIntent(readyIntent)

        assertThat(initialPresentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
        val createCall = presentationFactory.createCalls.awaitItem()
        assertThat(createCall.activity).isSameInstanceAs(activity)
        assertThat(createCall.state).isEqualTo(readyState)
        assertThat(createCall.activityResultCaller).isNotSameInstanceAs(activity)
        assertThat(readyPresentation.registerCalls.awaitItem()).isEqualTo(Unit)
        assertThat(activity.intent).isSameInstanceAs(readyIntent)

        coordinator.onDestroy()
        assertThat(readyPresentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `intent without args is ignored`() = runScenario {
        assertTransitionIgnored(Intent())
    }

    @Test
    fun `loading intent is ignored`() = runScenario {
        assertTransitionIgnored(
            createIntent(createLoadingState())
        )
    }

    @Test
    fun `ready manage intent is ignored`() = runScenario {
        assertTransitionIgnored(
            createIntent(
                createManageState()
            )
        )
    }

    @Test
    fun `ready coordinator ignores later ready intent`() = runScenario(
        initialState = createReadyState(),
    ) {
        assertTransitionIgnored(
            createIntent(createReadyState())
        )
    }

    @Test
    fun `finishing activity ignores ready intent`() = runScenario {
        activity.finish()

        assertTransitionIgnored(
            createIntent(createReadyState())
        )
    }

    private fun runScenario(
        initialState: EmbeddedActivityState = createLoadingState(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val activity = Robolectric.buildActivity(EmbeddedSheetActivity::class.java).get()
        val initialPresentation = FakeEmbeddedSheetPresentation()
        val readyPresentation = FakeEmbeddedSheetPresentation()
        val presentationFactory = FakeEmbeddedSheetPresentationFactory(
            initialPresentation = initialPresentation,
            readyPresentation = readyPresentation,
        )
        val coordinator = EmbeddedSheetActivityCoordinator(
            activity = activity,
            initialState = initialState,
            presentationFactory = presentationFactory,
        )
        val createCall = presentationFactory.createCalls.awaitItem()

        Scenario(
            activity = activity,
            state = initialState,
            coordinator = coordinator,
            initialPresentation = initialPresentation,
            readyPresentation = readyPresentation,
            presentationFactory = presentationFactory,
            createCall = createCall,
        ).block()

        presentationFactory.ensureAllEventsConsumed()
        initialPresentation.ensureAllEventsConsumed()
        readyPresentation.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val activity: EmbeddedSheetActivity,
        val state: EmbeddedActivityState,
        val coordinator: EmbeddedSheetActivityCoordinator,
        val initialPresentation: FakeEmbeddedSheetPresentation,
        val readyPresentation: FakeEmbeddedSheetPresentation,
        val presentationFactory: FakeEmbeddedSheetPresentationFactory,
        val createCall: FakeEmbeddedSheetPresentationFactory.CreateCall,
    ) {
        suspend fun assertTransitionIgnored(intent: Intent) {
            val originalIntent = activity.intent

            coordinator.handleNewIntent(intent)

            assertThat(activity.intent).isSameInstanceAs(originalIntent)
            presentationFactory.createCalls.expectNoEvents()
            initialPresentation.registerCalls.expectNoEvents()
            initialPresentation.onDestroyCalls.expectNoEvents()
            readyPresentation.registerCalls.expectNoEvents()
            readyPresentation.onDestroyCalls.expectNoEvents()
        }
    }

    private class FakeEmbeddedSheetPresentationFactory(
        private val initialPresentation: EmbeddedSheetPresentation,
        private val readyPresentation: EmbeddedSheetPresentation,
    ) : EmbeddedSheetPresentationFactory {
        val createCalls = Turbine<CreateCall>()

        override fun create(
            activity: EmbeddedSheetActivity,
            state: EmbeddedActivityState,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            createCalls.add(CreateCall(activity, state, activityResultCaller))
            return when (state) {
                is EmbeddedActivityState.LoadingPaymentOptions -> initialPresentation
                is EmbeddedActivityState.Ready -> readyPresentation
            }
        }

        fun ensureAllEventsConsumed() {
            createCalls.ensureAllEventsConsumed()
        }

        data class CreateCall(
            val activity: EmbeddedSheetActivity,
            val state: EmbeddedActivityState,
            val activityResultCaller: ActivityResultCaller,
        )
    }

    private class FakeEmbeddedSheetPresentation : EmbeddedSheetPresentation {
        var canDismissResult = true

        val registerCalls = Turbine<Unit>()
        val canDismissCalls = Turbine<Unit>()
        val onDismissedCalls = Turbine<Unit>()
        val onDestroyCalls = Turbine<Unit>()

        override fun register() {
            registerCalls.add(Unit)
        }

        override fun canDismiss(): Boolean {
            canDismissCalls.add(Unit)
            return canDismissResult
        }

        override fun onDismissed() {
            onDismissedCalls.add(Unit)
        }

        @Composable
        override fun Content() {
            Box(modifier = Modifier.testTag(CONTENT_TEST_TAG))
        }

        override fun onDestroy() {
            onDestroyCalls.add(Unit)
        }

        fun ensureAllEventsConsumed() {
            registerCalls.ensureAllEventsConsumed()
            canDismissCalls.ensureAllEventsConsumed()
            onDismissedCalls.ensureAllEventsConsumed()
            onDestroyCalls.ensureAllEventsConsumed()
        }
    }

    private companion object {
        const val CONTENT_TEST_TAG = "coordinator_content"

        fun createReadyState(
            callbackIdentifier: String = "callback_identifier",
        ): EmbeddedActivityState.Ready.PaymentOptions {
            return EmbeddedActivityState.Ready.PaymentOptions(
                context = createContext(callbackIdentifier),
                initialSelection = null,
                previousNewSelections = PreviousNewSelections.empty,
                customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            )
        }

        fun createManageState(): EmbeddedActivityState.Ready.Manage {
            return EmbeddedActivityState.Ready.Manage(
                context = createContext("callback_identifier"),
                initialSelection = null,
                previousNewSelections = PreviousNewSelections.empty,
                customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            )
        }

        fun createLoadingState(): EmbeddedActivityState.LoadingPaymentOptions {
            return EmbeddedActivityState.LoadingPaymentOptions(
                context = createContext("callback_identifier"),
                initialSelection = null,
                previousNewSelections = PreviousNewSelections.empty,
                customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            )
        }

        private fun createContext(callbackIdentifier: String): EmbeddedActivityState.Context {
            return EmbeddedActivityState.Context(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                productUsage = setOf("EmbeddedPaymentElement"),
                paymentElementCallbackIdentifier = callbackIdentifier,
                statusBarColor = null,
            )
        }

        fun createIntent(state: EmbeddedActivityState): Intent {
            return Intent().putExtra(EmbeddedActivityArgs.EXTRA_ARGS, state.toArgs())
        }
    }
}
