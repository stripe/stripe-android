package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class FlowControllerFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()

    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private lateinit var activityScenario: ActivityScenario<*>
    private lateinit var activity: ComponentActivity

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        Dispatchers.setMain(testDispatcher)

        activityScenarioFactory
            .createAddPaymentMethodActivity()
            .use { activityScenario ->
                this.activityScenario = activityScenario
                activityScenario.onActivity { activity ->
                    this.activity = activity
                }
            }
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `create() should return a FlowController instance`() {
        val factory = createFactory(activity)
        assertThat(factory.create())
            .isNotNull()
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
}
