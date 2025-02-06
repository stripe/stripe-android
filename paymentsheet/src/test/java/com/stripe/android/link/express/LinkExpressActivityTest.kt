package com.stripe.android.link.express

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkActivity
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
internal class LinkExpressActivityTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LinkActivity>()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `finishes with a cancelled result when no arg is passed`() {
        val intent = Intent(context, LinkExpressActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<LinkExpressActivity>(intent)

        assertThat(scenario.result.resultCode)
            .isEqualTo(Activity.RESULT_CANCELED)
        assertNoUnverifiedIntents()
    }
}
