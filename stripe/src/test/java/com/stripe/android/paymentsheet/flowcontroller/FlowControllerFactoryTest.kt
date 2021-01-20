package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
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
    private val factory: FlowControllerFactory by lazy {
        createFactory()
    }

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
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `create() should return a FlowController instance`() {
        assertThat(factory.create())
            .isNotNull()
    }

    private fun createFactory(): FlowControllerFactory {
        return FlowControllerFactory(
            activity,
            PaymentConfiguration.getInstance(context),
            mock(),
            mock()
        )
    }
}
