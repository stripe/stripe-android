package com.stripe.android.link

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkRedirectHandlerActivityTest {
    @get:Rule
    val intentsTestRule = IntentsRule()

    @Test
    fun `launches LinkForegroundActivity onCreate with url information`() {
        val uri = Uri.parse("stripesdk://link_return_url/example.app?payload=foo")
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            LinkRedirectHandlerActivity::class.java,
        ).also { intent ->
            intent.data = uri
        }

        val scenario = ActivityScenario.launch<LinkRedirectHandlerActivity>(intent)
        intended(
            allOf(
                hasComponent(LinkForegroundActivity::class.java.name),
                hasAction(LinkForegroundActivity.ACTION_REDIRECT),
                hasData(uri),
            )
        )
        assertThat(scenario.state).isAtLeast(Lifecycle.State.DESTROYED)
    }
}
