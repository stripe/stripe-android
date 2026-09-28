package com.stripe.android.challenge.confirmation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.createTestActivityRule
import com.stripe.android.view.ActivityScenarioFactory
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeWebViewTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @get:Rule
    internal val testActivityRule = createTestActivityRule<ActivityScenarioFactory.TestActivity>()

    private val webView: IntentConfirmationChallengeWebView by lazy {
        activityScenarioFactory.createView {
            IntentConfirmationChallengeWebView(it)
        }
    }

    @Test
    fun `init should set background color to transparent`() {
        assertThat(webView.background).isNull()
    }

    @Test
    fun `init should enable JavaScript`() {
        assertThat(webView.settings.javaScriptEnabled).isTrue()
    }

    @Test
    fun `init should enable DOM storage`() {
        assertThat(webView.settings.domStorageEnabled).isTrue()
    }

    @Test
    fun `updateUserAgent should append user agent suffix`() {
        val originalUserAgent = webView.settings.userAgentString.orEmpty()

        webView.updateUserAgent("TestAgent/1.0")

        assertThat(webView.settings.userAgentString)
            .isEqualTo("$originalUserAgent [TestAgent/1.0]")
    }
}
