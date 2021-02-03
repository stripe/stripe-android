package com.stripe.android.paymentsheet

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.ui.SheetMode
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val viewModel = PaymentOptionsViewModel(
        args = PaymentOptionContract.Args(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            sessionId = SessionId(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
            isGooglePayReady = false,
            newCard = null
        ),
        prefsRepository = FakePrefsRepository(),
        eventReporter = eventReporter
    )

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `click outside of bottom sheet should return cancel result`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(emptyList())
        ).onActivity { activity ->
            // wait for bottom sheet to animate in
            testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            viewModel.updateMode(SheetMode.Wrapped)

            activity.viewBinding.root.performClick()
            activity.finish()
        }

        assertThat(
            PaymentOptionResult.fromIntent(scenario.getResult().resultData)
        ).isEqualTo(
            PaymentOptionResult.Canceled(null)
        )
    }

    @Test
    fun `AddButton should be hidden when showing payment options`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(PaymentMethodFixtures.createCards(5))
        ).onActivity { activity ->
            // wait for bottom sheet to animate in
            testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            viewModel.updateMode(SheetMode.Wrapped)

            assertThat(activity.viewBinding.addButton.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `AddButton should be visible when showing add payment method form`() {
        val scenario = activityScenario()
        scenario.launch(
            createIntent(emptyList())
        ).onActivity { activity ->
            // wait for bottom sheet to animate in
            testDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            viewModel.updateMode(SheetMode.Wrapped)

            assertThat(activity.viewBinding.addButton.isVisible)
                .isTrue()
        }
    }

    private fun createIntent(
        paymentMethods: List<PaymentMethod>
    ): Intent {
        return Intent(
            ApplicationProvider.getApplicationContext(),
            PaymentOptionsActivity::class.java
        ).putExtras(
            bundleOf(
                ActivityStarter.Args.EXTRA to
                    PaymentOptionContract.Args(
                        paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        paymentMethods = paymentMethods,
                        sessionId = SessionId(),
                        config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                        isGooglePayReady = false,
                        newCard = null
                    )
            )
        )
    }

    private fun activityScenario(
        viewModel: PaymentOptionsViewModel = this.viewModel
    ): InjectableActivityScenario<PaymentOptionsActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }
}
