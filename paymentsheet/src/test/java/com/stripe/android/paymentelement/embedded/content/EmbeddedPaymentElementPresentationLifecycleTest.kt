package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedPaymentElementPresentationLifecycleTest {
    @Test
    fun `presentation follows host foreground and background events`() = runScenario(
        initialState = Lifecycle.State.CREATED,
    ) {
        val events = Turbine<Lifecycle.Event>()
        presentation.lifecycle.addObserver(LifecycleEventObserver { _, event -> events.add(event) })
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_CREATE)

        host.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_START)
        host.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_RESUME)
        host.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_PAUSE)
        host.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_STOP)

        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        cleanup.destroyCalls.expectNoEvents()

        host.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_START)
        host.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(events.awaitItem()).isEqualTo(Lifecycle.Event.ON_RESUME)
        events.ensureAllEventsConsumed()
    }

    @Test
    fun `permanent removal cleans up presentation while host remains alive`() = runScenario {
        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(host.observerCount).isEqualTo(1)

        presentation.destroy()

        assertThat(cleanup.destroyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(host.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(host.observerCount).isEqualTo(0)

        host.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        host.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        host.handleLifecycleEvent(Lifecycle.Event.ON_START)
        host.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        cleanup.destroyCalls.expectNoEvents()
    }

    @Test
    fun `host destruction ends old presentation and replacement host has its own presentation`() = runScenario {
        host.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(cleanup.destroyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(host.observerCount).isEqualTo(0)

        val replacementHost = TestLifecycleOwner(
            initialState = Lifecycle.State.RESUMED,
            coroutineDispatcher = Dispatchers.Unconfined,
        )
        val replacementPresentation = EmbeddedPaymentElementPresentationLifecycle(replacementHost)
        val replacementCleanup = FakePresentationCleanup()
        replacementPresentation.lifecycle.addObserver(replacementCleanup)

        assertThat(replacementPresentation.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        replacementCleanup.destroyCalls.expectNoEvents()

        replacementPresentation.destroy()

        assertThat(replacementCleanup.destroyCalls.awaitItem()).isEqualTo(Unit)
        assertThat(replacementHost.currentState).isEqualTo(Lifecycle.State.RESUMED)
        replacementCleanup.ensureAllEventsConsumed()
    }

    @Test
    fun `destroying presentation repeatedly cleans up only once`() = runScenario {
        presentation.destroy()
        assertThat(cleanup.destroyCalls.awaitItem()).isEqualTo(Unit)

        presentation.destroy()
        host.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        presentation.destroy()

        cleanup.destroyCalls.expectNoEvents()
        assertThat(presentation.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    private fun runScenario(
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val host = TestLifecycleOwner(
            initialState = initialState,
            coroutineDispatcher = Dispatchers.Unconfined,
        )
        val presentation = EmbeddedPaymentElementPresentationLifecycle(host)
        val cleanup = FakePresentationCleanup()
        presentation.lifecycle.addObserver(cleanup)

        Scenario(
            host = host,
            presentation = presentation,
            cleanup = cleanup,
        ).block()
        cleanup.ensureAllEventsConsumed()
    }

    private class Scenario(
        val host: TestLifecycleOwner,
        val presentation: EmbeddedPaymentElementPresentationLifecycle,
        val cleanup: FakePresentationCleanup,
    )

    private class FakePresentationCleanup : DefaultLifecycleObserver {
        val destroyCalls = Turbine<Unit>()

        override fun onDestroy(owner: LifecycleOwner) {
            destroyCalls.add(Unit)
        }

        fun ensureAllEventsConsumed() {
            destroyCalls.ensureAllEventsConsumed()
        }
    }
}
