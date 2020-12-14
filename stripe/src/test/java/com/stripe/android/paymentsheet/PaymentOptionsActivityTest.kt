package com.stripe.android.paymentsheet

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
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
import org.robolectric.Shadows

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val viewModel = PaymentOptionsViewModel(
        args = PaymentOptionsActivityStarter.Args(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY
        ),
        googlePayRepository = FakeGooglePayRepository(false),
        eventReporter = eventReporter
    )

    @Test
    fun `click outside of bottom sheet should return cancel result`() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            PaymentOptionsActivity::class.java
        ).putExtra(
            ActivityStarter.Args.EXTRA,
            PaymentOptionsActivityStarter.Args(
                paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
                paymentMethods = emptyList(),
                config = PaymentSheetFixtures.CONFIG_GOOGLEPAY
            )
        )

        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            viewModel.updateMode(SheetMode.Wrapped)

            activity.viewBinding.root.performClick()
            idleLooper()

            assertThat(
                PaymentOptionResult.fromIntent(Shadows.shadowOf(activity).resultIntent)
            ).isEqualTo(
                PaymentOptionResult.Cancelled(null)
            )
        }
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
