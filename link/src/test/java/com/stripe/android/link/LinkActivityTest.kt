package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkActivityTest {

    @get:Rule
    val intentsTestRule = IntentsRule()

    @Test
    fun `finishes with a cancelled result when no popupUrl is passed`() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), LinkActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<LinkActivity>(intent)
        assertThat(scenario.result.resultCode)
            .isEqualTo(Activity.RESULT_CANCELED)
        assertNoUnverifiedIntents()
    }
}
