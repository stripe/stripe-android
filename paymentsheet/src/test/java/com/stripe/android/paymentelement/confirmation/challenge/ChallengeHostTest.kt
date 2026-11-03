package com.stripe.android.paymentelement.confirmation.challenge

import android.app.Activity
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class ChallengeHostTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @get:Rule
    val activityCleanupRule = CleanupTestRule<ActivityController<out Activity>> {
        if (!get().isDestroyed) pause().stop().destroy()
    }

    @Test
    fun `uses a FragmentActivity lifecycle owner behind a wrapped caller`() = runScenario {
        challengeHost.register(DummyActivityResultCaller.noOp(), activity)

        assertThat(challengeHost.host).isSameInstanceAs(activity)
    }

    @Test
    fun `supports AppCompatActivity hosts`() = runScenario {
        val appCompatActivity = createActivity(AppCompatActivity::class.java).get()

        challengeHost.register(DummyActivityResultCaller.noOp(), appCompatActivity)

        assertThat(challengeHost.host).isSameInstanceAs(appCompatActivity)
    }

    @Test
    fun `uses an attached Fragment lifecycle owner`() = runScenario {
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        challengeHost.register(DummyActivityResultCaller.noOp(), fragment)

        assertThat(challengeHost.host).isSameInstanceAs(activity)
    }

    @Test
    fun `falls back to a FragmentActivity caller`() = runScenario {
        challengeHost.register(activity, fakeLifecycleOwner())

        assertThat(challengeHost.host).isSameInstanceAs(activity)
    }

    @Test
    fun `falls back to an attached Fragment caller`() = runScenario {
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        challengeHost.register(fragment, fakeLifecycleOwner())

        assertThat(challengeHost.host).isSameInstanceAs(activity)
    }

    @Test
    fun `prefers the lifecycle owner over the caller`() = runScenario {
        val otherActivity = createActivity(FragmentActivity::class.java).get()

        challengeHost.register(otherActivity, activity)

        assertThat(challengeHost.host).isSameInstanceAs(activity)
    }

    @Test
    fun `skips warm-up for ComponentActivity hosts`() = runScenario {
        val componentActivity = createActivity(ComponentActivity::class.java).get()
        challengeHost.register(componentActivity, componentActivity)

        challengeHost.launchWarmUp { started.add(it) }

        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `skips warm-up when no activity can be resolved`() = runScenario {
        challengeHost.register(DummyActivityResultCaller.noOp(), Fragment())

        challengeHost.launchWarmUp { started.add(it) }

        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `does not register a destroyed owner`() = runScenario {
        val owner = fakeLifecycleOwner()
        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        challengeHost.register(activity, owner)

        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `does not register a destroyed activity`() = runScenario {
        controller.pause().stop().destroy()

        challengeHost.register(activity, fakeLifecycleOwner())

        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `does not register an activity whose lifecycle is already destroyed`() = runScenario {
        controller.pause().stop()
        (activity.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.DESTROYED
        assertThat(activity.isDestroyed).isFalse()

        challengeHost.register(activity, fakeLifecycleOwner())

        assertThat(challengeHost.host).isNull()
        controller.destroy()
    }

    @Test
    fun `launches on Main even when requested from a background dispatcher`() = runScenario {
        val loopers = Turbine<Looper?>()
        challengeHost.register(activity, activity)

        withContext(Dispatchers.Default) {
            challengeHost.launchWarmUp { loopers.add(Looper.myLooper()) }
        }

        assertThat(loopers.awaitItem()).isSameInstanceAs(Looper.getMainLooper())
        loopers.ensureAllEventsConsumed()
    }

    @Test
    fun `unregister cancels warm-up and clears the host`() = runScenario {
        challengeHost.register(activity, activity)
        startWarmUp()
        assertThat(started.awaitItem()).isSameInstanceAs(activity)

        challengeHost.unregister()

        cancelled.awaitItem()
        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `owner destruction cancels warm-up while activity is still alive`() = runScenario {
        val owner = fakeLifecycleOwner()
        challengeHost.register(activity, owner)
        startWarmUp()
        started.awaitItem()

        owner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        cancelled.awaitItem()
        assertThat(activity.isDestroyed).isFalse()
        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `activity destruction cancels warm-up with a separate owner`() = runScenario {
        challengeHost.register(activity, fakeLifecycleOwner())
        startWarmUp()
        started.awaitItem()

        controller.pause().stop().destroy()

        cancelled.awaitItem()
        assertThat(challengeHost.host).isNull()
    }

    @Test
    fun `replacement cancels old warm-up and ignores old owner destruction`() = runScenario {
        val oldOwner = fakeLifecycleOwner()
        challengeHost.register(activity, oldOwner)
        startWarmUp()
        started.awaitItem()
        val newActivity = createActivity(FragmentActivity::class.java).get()

        challengeHost.register(newActivity, newActivity)

        cancelled.awaitItem()
        oldOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        controller.pause().stop().destroy()
        assertThat(challengeHost.host).isSameInstanceAs(newActivity)
        challengeHost.launchWarmUp { started.add(it) }
        assertThat(started.awaitItem()).isSameInstanceAs(newActivity)
    }

    @Test
    fun `ignores duplicate launches while warm-up is running`() = runScenario {
        challengeHost.register(activity, activity)
        startWarmUp()
        started.awaitItem()

        startWarmUp()
        testScheduler.runCurrent()
        challengeHost.unregister()

        cancelled.awaitItem()
    }

    @Test
    fun `unregister prevents queued warm-up from using the old activity`() = runScenario {
        challengeHost.register(activity, activity)
        challengeHost.launchWarmUp { started.add(it) }

        challengeHost.unregister()

        testScheduler.runCurrent()
        assertThat(challengeHost.host).isNull()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val controller = createActivity(FragmentActivity::class.java)
        val scenario = Scenario(
            controller = controller,
            challengeHost = ChallengeHost(),
            started = Turbine(),
            cancelled = Turbine(),
            testScheduler = testScheduler,
        )
        try {
            scenario.block()
            testScheduler.runCurrent()
        } finally {
            scenario.challengeHost.unregister()
            testScheduler.runCurrent()
            scenario.started.ensureAllEventsConsumed()
            scenario.cancelled.ensureAllEventsConsumed()
        }
    }

    private fun <A : Activity> createActivity(activityClass: Class<A>): ActivityController<A> {
        return Robolectric.buildActivity(activityClass).apply {
            get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            setup()
            activityCleanupRule.track(this)
        }
    }

    private data class Scenario(
        val controller: ActivityController<FragmentActivity>,
        val challengeHost: ChallengeHost,
        val started: Turbine<FragmentActivity>,
        val cancelled: Turbine<Unit>,
        val testScheduler: TestCoroutineScheduler,
    ) {
        val activity: FragmentActivity
            get() = controller.get()

        fun startWarmUp() {
            challengeHost.launchWarmUp {
                started.add(it)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.add(Unit)
                }
            }
        }
    }
}
