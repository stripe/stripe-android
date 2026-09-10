package com.stripe.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CheckoutPresenterLifecycleTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `presenter lifecycle follows activity lifecycle`() = runTest {
        val activity = TestLifecycleOwner()
        val presenter = CheckoutPresenterLifecycle().create(activity)

        activity.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        activity.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertThat(presenter.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        activity.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(presenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `destroy transitions every presenter lifecycle to destroyed`() = runTest {
        val activity = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val presenterLifecycle = CheckoutPresenterLifecycle()
        val firstPresenter = presenterLifecycle.create(activity)
        val secondPresenter = presenterLifecycle.create(activity)

        presenterLifecycle.destroy()

        assertThat(firstPresenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(secondPresenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `destroy invokes controller destroy listener`() = runTest {
        val presenterLifecycle = CheckoutPresenterLifecycle()
        val presenter = presenterLifecycle.create(TestLifecycleOwner(Lifecycle.State.RESUMED))
        val listenerCalls = Turbine<Unit>()
        presenter.addControllerDestroyListener { listenerCalls.add(Unit) }

        presenterLifecycle.destroy()

        listenerCalls.awaitItem()
        listenerCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `activity destroy removes presenter without invoking controller destroy listener`() = runTest {
        val activity = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val presenterLifecycle = CheckoutPresenterLifecycle()
        val presenter = presenterLifecycle.create(activity)
        val listenerCalls = Turbine<Unit>()
        presenter.addControllerDestroyListener { listenerCalls.add(Unit) }

        activity.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        presenterLifecycle.destroy()

        assertThat(presenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        listenerCalls.expectNoEvents()
        listenerCalls.ensureAllEventsConsumed()
    }
}
