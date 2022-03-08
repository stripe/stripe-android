package com.stripe.android.link

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.utils.InjectableActivityScenario
import com.stripe.android.link.utils.injectableActivityScenario
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val intent = LinkActivityContract().createIntent(
        context,
        LinkActivityContract.Args(
            merchantName = "Example, Inc.",
            injectionParams = LinkActivityContract.Args.InjectionParams(
                injectorKey = "dummy",
                productUsage = emptySet(),
                enableLogging = true,
                publishableKey = "key",
                stripeAccountId = null
            )
        )
    )

    @Before
    fun before() {
        PaymentConfiguration.init(context, "publishable_key")
    }

    @Test
    fun `Activity launches sign up UI`() {
        activityScenario().launch(intent).onActivity { activity ->
            assertThat(activity.navController.currentDestination?.route)
                .isEqualTo(LinkScreen.SignUp.route)
        }
    }

    private fun activityScenario(): InjectableActivityScenario<LinkActivity> {
        return injectableActivityScenario {
            injectActivity {}
        }
    }
}
