package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.utils.createTestActivityRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @get:Rule
    internal val testActivityRule = createTestActivityRule<ActivityScenarioFactory.TestActivity>()

    private val webView: PaymentAuthWebView by lazy {
        activityScenarioFactory.createView {
            PaymentAuthWebView(it)
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `userAgentString should add SDK user agent suffix`() {
        val userAgent = webView.settings.userAgentString
        assertThat(userAgent)
            .contains("AppleWebkit")
        assertThat(userAgent)
            .endsWith(" [Stripe/v1 AndroidBindings/${StripeSdkVersion.VERSION_NAME}]")
    }
}
