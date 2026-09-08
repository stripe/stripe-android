package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityCoordinatorTest {

    @Test
    fun `register delegates to initial presentation`() = runScenario {
        coordinator.register()

        assertThat(initialPresentation.registerCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `canDismiss delegates to current presentation`() = runScenario {
        initialPresentation.canDismissResult = false

        assertThat(coordinator.canDismiss()).isFalse()
        assertThat(initialPresentation.canDismissCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `onDismissed delegates to current presentation`() = runScenario {
        coordinator.onDismissed()

        assertThat(initialPresentation.onDismissedCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `onDestroy delegates to initial presentation`() = runScenario {
        coordinator.onDestroy()

        assertThat(initialPresentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `valid loading to ready transition replaces presentation`() = runScenario {
        val readyArgs = createArgs(
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
            callbackIdentifier = "updated_callback_identifier",
        )
        val readyIntent = createIntent(readyArgs)

        coordinator.handleNewIntent(readyIntent)

        assertThat(initialPresentation.onDestroyCalls.awaitItem()).isEqualTo(Unit)
        val createCall = presentationFactory.createCalls.awaitItem()
        assertThat(createCall.activity).isSameInstanceAs(activity)
        assertThat(createCall.args).isEqualTo(readyArgs)
        assertThat(createCall.activityResultCaller).isNotSameInstanceAs(activity)
        assertThat(readyPresentation.registerCalls.awaitItem()).isEqualTo(Unit)
        assertThat(activity.intent).isSameInstanceAs(readyIntent)

        assertTransitionIgnored(
            createIntent(
                createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Ready),
            )
        )

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
            createIntent(
                createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Loading),
            )
        )
    }

    @Test
    fun `ready manage intent is ignored`() = runScenario {
        assertTransitionIgnored(
            createIntent(
                createArgs(
                    presentationState = EmbeddedActivityArgs.PresentationState.Ready,
                    launchMode = EmbeddedLaunchMode.Manage,
                )
            )
        )
    }

    @Test
    fun `ready coordinator ignores later ready intent`() = runScenario(
        initialArgs = createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Ready),
    ) {
        assertTransitionIgnored(
            createIntent(
                createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Ready),
            )
        )
    }

    @Test
    fun `non-payment-options coordinator ignores ready intent`() = runScenario(
        initialArgs = createArgs(
            presentationState = EmbeddedActivityArgs.PresentationState.Loading,
            launchMode = EmbeddedLaunchMode.Manage,
        ),
    ) {
        assertTransitionIgnored(
            createIntent(
                createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Ready),
            )
        )
    }

    @Test
    fun `finishing activity ignores ready intent`() = runScenario {
        activity.finish()
        assertThat(activity.isFinishing).isTrue()

        assertTransitionIgnored(
            createIntent(
                createArgs(presentationState = EmbeddedActivityArgs.PresentationState.Ready),
            )
        )
    }

    private fun runScenario(
        initialArgs: EmbeddedActivityArgs = createArgs(
            presentationState = EmbeddedActivityArgs.PresentationState.Loading,
        ),
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
            initialArgs = initialArgs,
            presentationFactory = presentationFactory,
        )
        val createCall = presentationFactory.createCalls.awaitItem()
        assertThat(createCall.activity).isSameInstanceAs(activity)
        assertThat(createCall.args).isEqualTo(initialArgs)
        assertThat(createCall.activityResultCaller).isSameInstanceAs(activity)

        Scenario(
            activity = activity,
            coordinator = coordinator,
            initialPresentation = initialPresentation,
            readyPresentation = readyPresentation,
            presentationFactory = presentationFactory,
        ).block()

        presentationFactory.ensureAllEventsConsumed()
        initialPresentation.ensureAllEventsConsumed()
        readyPresentation.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val activity: EmbeddedSheetActivity,
        val coordinator: EmbeddedSheetActivityCoordinator,
        val initialPresentation: FakeEmbeddedSheetPresentation,
        val readyPresentation: FakeEmbeddedSheetPresentation,
        val presentationFactory: FakeEmbeddedSheetPresentationFactory,
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
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            createCalls.add(CreateCall(activity, args, activityResultCaller))
            return when (args.presentationState) {
                EmbeddedActivityArgs.PresentationState.Loading -> initialPresentation
                EmbeddedActivityArgs.PresentationState.Ready -> readyPresentation
            }
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
        override fun Content() = Unit

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
        fun createArgs(
            presentationState: EmbeddedActivityArgs.PresentationState,
            launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions,
            callbackIdentifier: String = "callback_identifier",
        ): EmbeddedActivityArgs {
            return EmbeddedActivityArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                productUsage = setOf("EmbeddedPaymentElement"),
                paymentElementCallbackIdentifier = callbackIdentifier,
                statusBarColor = null,
                selection = null,
                previousNewSelections = Bundle(),
                customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                promotions = emptyList(),
                launchMode = launchMode,
                presentationState = presentationState,
            )
        }

        fun createIntent(args: EmbeddedActivityArgs): Intent {
            return Intent().putExtra(EmbeddedActivityArgs.EXTRA_ARGS, args)
        }
    }
}
