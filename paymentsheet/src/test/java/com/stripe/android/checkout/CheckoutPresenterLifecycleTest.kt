package com.stripe.android.checkout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class CheckoutPresenterLifecycleTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `presenter lifecycle follows activity lifecycle`() = runTest {
        val activity = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val presenter = CheckoutPresenterLifecycle().create(activity)

        assertThat(presenter.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
        listOf(
            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_STOP,
            Lifecycle.Event.ON_DESTROY,
        ).forEach { event ->
            activity.handleLifecycleEvent(event)

            assertThat(presenter.lifecycle.currentState).isEqualTo(event.targetState)
        }
    }

    @Test
    fun `destroy transitions every presenter lifecycle to destroyed`() = runTest {
        val activity = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val presenterLifecycle = CheckoutPresenterLifecycle()
        val firstPresenter = presenterLifecycle.create(activity)
        val secondPresenter = presenterLifecycle.create(activity)

        presenterLifecycle.destroy()
        presenterLifecycle.destroy()
        activity.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        activity.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        assertThat(firstPresenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(secondPresenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `destroy before activity creation destroys presenter`() = runTest {
        val activity = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        val presenterLifecycle = CheckoutPresenterLifecycle()
        val presenter = presenterLifecycle.create(activity)

        presenterLifecycle.destroy()

        assertThat(presenter.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(activity.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)
    }

    @Test
    fun `cannot create presenter for destroyed activity`() = runTest {
        val activity = TestLifecycleOwner()
        activity.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertFailsWith<IllegalStateException> {
            CheckoutPresenterLifecycle().create(activity)
        }
    }
}
