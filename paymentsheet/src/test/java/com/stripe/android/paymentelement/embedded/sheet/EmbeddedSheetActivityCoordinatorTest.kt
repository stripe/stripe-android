package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
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
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
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
        assertThat(createCall.args).isEqualTo(args)
        assertThat(createCall.activityResultCaller).isSameInstanceAs(activity)
    }

    @Test
    fun `register delegates to presentation`() = runScenario {
        coordinator.register()

        assertThat(presentation.registerCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `canDismiss delegates to presentation`() = runScenario {
        presentation.canDismissResult = false

        assertThat(coordinator.canDismiss()).isFalse()
        assertThat(presentation.canDismissCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `onDismissed delegates to presentation`() = runScenario {
        coordinator.onDismissed()

        assertThat(presentation.onDismissedCalls.awaitItem()).isEqualTo(Unit)
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

        assertThat(presentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val activity = Robolectric.buildActivity(EmbeddedSheetActivity::class.java).get()
        val args = createArgs()
        val presentation = FakeEmbeddedSheetPresentation()
        val presentationFactory = FakeEmbeddedSheetPresentationFactory(presentation)
        val coordinator = EmbeddedSheetActivityCoordinator(
            activity = activity,
            args = args,
            presentationFactory = presentationFactory,
        )
        val createCall = presentationFactory.createCalls.awaitItem()

        Scenario(
            activity = activity,
            args = args,
            coordinator = coordinator,
            presentation = presentation,
            createCall = createCall,
        ).block()

        presentationFactory.ensureAllEventsConsumed()
        presentation.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val activity: EmbeddedSheetActivity,
        val args: EmbeddedActivityArgs,
        val coordinator: EmbeddedSheetActivityCoordinator,
        val presentation: FakeEmbeddedSheetPresentation,
        val createCall: FakeEmbeddedSheetPresentationFactory.CreateCall,
    )

    private class FakeEmbeddedSheetPresentationFactory(
        private val presentation: EmbeddedSheetPresentation,
    ) : EmbeddedSheetPresentationFactory {
        val createCalls = Turbine<CreateCall>()

        override fun create(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            createCalls.add(CreateCall(activity, args, activityResultCaller))
            return presentation
        }

        fun ensureAllEventsConsumed() {
            createCalls.ensureAllEventsConsumed()
        }

        data class CreateCall(
            val activity: EmbeddedSheetActivity,
            val args: EmbeddedActivityArgs,
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

        fun createArgs(): EmbeddedActivityArgs {
            return EmbeddedActivityArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                productUsage = setOf("EmbeddedPaymentElement"),
                paymentElementCallbackIdentifier = "callback_identifier",
                statusBarColor = null,
                selection = null,
                previousNewSelections = Bundle(),
                customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                promotions = emptyList(),
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )
        }
    }
}
