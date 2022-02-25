package com.stripe.android.link

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.utils.InjectableActivityScenario
import com.stripe.android.link.utils.injectableActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val intent = LinkActivityContract().createIntent(
        context,
        LinkActivityContract.Args("Example, Inc.")
    )

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
