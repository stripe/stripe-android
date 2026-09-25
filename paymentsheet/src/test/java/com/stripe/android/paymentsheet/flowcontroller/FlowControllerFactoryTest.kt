package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Identifiable
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FlowControllerFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    internal val testActivityRule = createTestActivityRule<TestActivity>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(StandardTestDispatcher())

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `create() with Fragment uses the provided ID`() {
        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val factory = createFactory(fragment)
                val id = Identifiable()
                val flowController = factory.create(id) as DefaultFlowController

                assertThat(flowController.id).isEqualTo(id)
            }
        }
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
