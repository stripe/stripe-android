package com.stripe.android.link

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
internal class LinkForegroundActivityTest {
    private val popupUrl = "https://checkout.link.com/from_unit_tests"
    private val redirectUrl = "stripesdk://link_return_url/example.app?payload=foo".toUri()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @Test
    fun `finishes with a cancelled result when no popupUrl is passed`() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), LinkForegroundActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<LinkForegroundActivity>(intent)
        assertThat(scenario.result.resultCode)
            .isEqualTo(Activity.RESULT_CANCELED)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with a failure result when activity fails to launch`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        intending(anyIntent()).respondWithFunction {
            throw ActivityNotFoundException("From Tests")
        }

        val scenario = ActivityScenario.launchActivityForResult<LinkForegroundActivity>(intent)
        assertThat(scenario.result.resultCode)
            .isEqualTo(LinkForegroundActivity.RESULT_FAILURE)
    }

    @Test
    fun `launches chrome custom tabs with url onResume`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val scenario = ActivityScenario.launch<LinkForegroundActivity>(intent)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(popupUrl),
            )
        )
        assertThat(scenario.state).isEquivalentAccordingToCompareTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun `closing the browser emits a cancelled result`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val scenario = ActivityScenario.launchActivityForResult<LinkForegroundActivity>(intent)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(popupUrl),
            )
        )
        assertThat(scenario.state).isEquivalentAccordingToCompareTo(Lifecycle.State.RESUMED)

        scenario.moveToState(Lifecycle.State.STARTED).moveToState(Lifecycle.State.RESUMED)

        assertThat(scenario.result.resultCode)
            .isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun `launches url only once when activity is recreated`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val scenario = ActivityScenario.launch<LinkForegroundActivity>(intent)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(popupUrl),
            ),
            times(1)
        )
        scenario.recreate()
        assertNoUnverifiedIntents()
    }

    @Test
    fun `launches url only once when activity is resumed after paused`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val scenario = ActivityScenario.launch<LinkForegroundActivity>(intent)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(popupUrl),
            ),
            times(1)
        )
        scenario.moveToState(Lifecycle.State.STARTED).moveToState(Lifecycle.State.RESUMED)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `handles onNewIntent having intent with redirect action`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val scenario = ActivityScenario.launchActivityForResult<LinkForegroundActivity>(intent)
        scenario.onActivity { activity ->
            activity.onNewIntent(
                LinkForegroundActivity.redirectIntent(
                    activity,
                    redirectUrl,
                )
            )
        }
        assertThat(scenario.result.resultCode)
            .isEqualTo(LinkForegroundActivity.RESULT_COMPLETE)
        assertThat(scenario.result.resultData.data)
            .isEqualTo(redirectUrl)
    }

    @Test
    fun `handles onCreate having intent with redirect action`() {
        val intent = LinkForegroundActivity.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            popupUrl = popupUrl,
        )

        val controller = Robolectric.buildActivity(LinkForegroundActivity::class.java, intent)
        controller.create().resume()
        controller.newIntent(
            LinkForegroundActivity.redirectIntent(
                controller.get(),
                redirectUrl,
            )
        )
        assertThat(controller.get().isFinishing).isTrue()
        val shadowActivity = Shadows.shadowOf(controller.get()) as ShadowActivity
        assertThat(shadowActivity.resultCode)
            .isEqualTo(LinkForegroundActivity.RESULT_COMPLETE)
        assertThat(shadowActivity.resultIntent.data)
            .isEqualTo(redirectUrl)
    }
}
