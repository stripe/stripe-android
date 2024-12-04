package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.createTestActivityRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FlowControllerFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    internal val testActivityRule = createTestActivityRule<TestActivity>()

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create() should return a FlowController instance`() {
        ActivityScenario.launch<TestActivity>(
            Intent(context, TestActivity::class.java)
        ).moveToState(Lifecycle.State.CREATED)
            .use { activityScenario ->
                activityScenario.onActivity { activity ->
                    val factory = createFactory(activity)
                    assertThat(factory.create())
                        .isNotNull()
                }
            }
    }

    @Test
    fun `create() with Fragment should return a FlowController instance`() {
        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val factory = createFactory(fragment)
                assertThat(factory.create())
                    .isNotNull()
            }
        }
    }

    private fun createFactory(
        activity: ComponentActivity
    ): FlowControllerFactory {
        return FlowControllerFactory(
            activity,
            mock(),
            mock()
        )
    }

    private fun createFactory(
        fragment: Fragment
    ): FlowControllerFactory {
        return FlowControllerFactory(
            fragment,
            mock(),
            mock()
        )
    }

    internal class TestFragment : Fragment()

    internal class TestActivity : AppCompatActivity()
}
