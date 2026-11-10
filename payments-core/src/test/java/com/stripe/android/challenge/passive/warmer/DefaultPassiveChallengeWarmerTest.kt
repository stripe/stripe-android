package com.stripe.android.challenge.passive.warmer

import android.app.Activity
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.FakeHCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
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
internal class DefaultPassiveChallengeWarmerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @get:Rule
    val activityCleanupRule = CleanupTestRule<ActivityController<out Activity>> {
        if (!get().isDestroyed) pause().stop().destroy()
    }

    @Test
    fun `uses a FragmentActivity lifecycle owner`() = runScenario {
        warmer.register(DummyActivityResultCaller.noOp(), activity)

        assertWarmUpUses(activity)
    }

    @Test
    fun `supports AppCompatActivity hosts`() = runScenario {
        val appCompatActivity = createActivity(AppCompatActivity::class.java).get()

        warmer.register(DummyActivityResultCaller.noOp(), appCompatActivity)

        assertWarmUpUses(appCompatActivity)
    }

    @Test
    fun `uses an attached Fragment lifecycle owner`() = runScenario {
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        warmer.register(DummyActivityResultCaller.noOp(), fragment)

        assertWarmUpUses(activity)
    }

    @Test
    fun `falls back to a FragmentActivity caller`() = runScenario {
        warmer.register(activity, componentActivityOwner())

        assertWarmUpUses(activity)
    }

    @Test
    fun `falls back to an attached Fragment caller`() = runScenario {
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        warmer.register(fragment, componentActivityOwner())

        assertWarmUpUses(activity)
    }

    @Test
    fun `prefers the lifecycle owner over the caller`() = runScenario {
        val otherActivity = createActivity(FragmentActivity::class.java).get()

        warmer.register(otherActivity, activity)

        assertWarmUpUses(activity)
    }

    @Test
    fun `skips warm-up before registration`() = runScenario {
        assertNoWarmUp()
    }

    @Test
    fun `skips warm-up for ComponentActivity hosts`() = runScenario {
        val componentActivity = createActivity(ComponentActivity::class.java).get()
        warmer.register(componentActivity, componentActivity)

        assertNoWarmUp()
    }

    @Test
    fun `skips warm-up when no activity can be resolved`() = runScenario {
        warmer.register(DummyActivityResultCaller.noOp(), Fragment())

        assertNoWarmUp()
    }

    @Test
    fun `does not register a finishing activity`() = runScenario {
        activity.finish()

        warmer.register(activity, componentActivityOwner())

        assertNoWarmUp()
    }

    @Test
    fun `does not register a destroyed activity`() = runScenario {
        controller.pause().stop().destroy()

        warmer.register(activity, componentActivityOwner())

        assertNoWarmUp()
    }

    @Test
    fun `launches on Main even when requested from a background dispatcher`() = runScenario {
        val loopers = Turbine<Looper?>()
        warmer.register(activity, activity)

        hCaptchaService.warmUpResult = { loopers.add(Looper.myLooper()) }
        withContext(Dispatchers.Default) {
            warmer.start(passiveCaptchaParams)
        }

        hCaptchaService.awaitWarmUpCall()
        assertThat(loopers.awaitItem()).isSameInstanceAs(Looper.getMainLooper())
        loopers.ensureAllEventsConsumed()
    }

    @Test
    fun `unregister cancels warm-up and clears the host`() = runScenario {
        warmer.register(activity, activity)
        startWarmUp()
        assertThat(hCaptchaService.awaitWarmUpCall().activity).isSameInstanceAs(activity)

        warmer.unregister()

        cancelled.awaitItem()
        assertThat(activity.lifecycleScope.isActive).isTrue()
        assertNoWarmUp()
    }

    @Test
    fun `activity destruction cancels warm-up`() = runScenario {
        warmer.register(activity, activity)
        startWarmUp()
        hCaptchaService.awaitWarmUpCall()

        controller.pause().stop().destroy()

        cancelled.awaitItem()
        assertNoWarmUp()
    }

    @Test
    fun `replacement cancels old warm-up and registers the new host`() = runScenario {
        warmer.register(activity, activity)
        startWarmUp()
        hCaptchaService.awaitWarmUpCall()
        val newActivity = createActivity(FragmentActivity::class.java).get()

        warmer.register(newActivity, newActivity)

        cancelled.awaitItem()
        controller.pause().stop().destroy()
        hCaptchaService.warmUpResult = {}
        assertWarmUpUses(newActivity)
    }

    @Test
    fun `ignores duplicate launches while warm-up is running`() = runScenario {
        warmer.register(activity, activity)
        startWarmUp()
        hCaptchaService.awaitWarmUpCall()

        startWarmUp()
        testScheduler.runCurrent()
        warmer.unregister()

        cancelled.awaitItem()
    }

    @Test
    fun `relaunches once the previous warm-up finishes`() = runScenario {
        warmer.register(activity, activity)
        warmer.start(passiveCaptchaParams)
        hCaptchaService.awaitWarmUpCall()

        warmer.start(passiveCaptchaParams)
        testScheduler.runCurrent()
        hCaptchaService.awaitWarmUpCall()
    }

    @Test
    fun `unregister prevents queued warm-up from using the old activity`() = runScenario {
        warmer.register(activity, activity)
        warmer.start(passiveCaptchaParams)

        warmer.unregister()

        testScheduler.runCurrent()
        assertNoWarmUp()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val controller = createActivity(FragmentActivity::class.java)
        val hCaptchaService = FakeHCaptchaService()
        val scenario = Scenario(
            controller = controller,
            warmer = DefaultPassiveChallengeWarmer(hCaptchaService),
            hCaptchaService = hCaptchaService,
            cancelled = Turbine(),
            testScheduler = testScheduler,
        )
        try {
            scenario.block()
            testScheduler.runCurrent()
        } finally {
            scenario.warmer.unregister()
            testScheduler.runCurrent()
            scenario.hCaptchaService.ensureAllEventsConsumed()
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

    private fun componentActivityOwner() = createActivity(ComponentActivity::class.java).get()

    private data class Scenario(
        val controller: ActivityController<FragmentActivity>,
        val warmer: DefaultPassiveChallengeWarmer,
        val hCaptchaService: FakeHCaptchaService,
        val cancelled: Turbine<Unit>,
        val testScheduler: TestCoroutineScheduler,
    ) {
        val activity: FragmentActivity
            get() = controller.get()

        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "site-key",
            rqData = "rq-data",
            tokenTimeoutSeconds = null,
        )

        suspend fun assertWarmUpUses(expectedActivity: FragmentActivity) {
            warmer.start(passiveCaptchaParams)
            val call = hCaptchaService.awaitWarmUpCall()
            assertThat(call.activity).isSameInstanceAs(expectedActivity)
            assertThat(call.siteKey).isEqualTo(passiveCaptchaParams.siteKey)
            assertThat(call.rqData).isEqualTo(passiveCaptchaParams.rqData)
        }

        fun assertNoWarmUp() {
            warmer.start(passiveCaptchaParams)
            testScheduler.runCurrent()
            hCaptchaService.ensureAllEventsConsumed()
        }

        fun startWarmUp() {
            hCaptchaService.warmUpResult = {
                try {
                    awaitCancellation()
                } finally {
                    cancelled.add(Unit)
                }
            }
            warmer.start(passiveCaptchaParams)
        }
    }
}
