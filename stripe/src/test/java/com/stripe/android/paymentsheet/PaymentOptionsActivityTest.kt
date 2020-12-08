package com.stripe.android.paymentsheet

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.PaymentIntentFixtures
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

    private val viewModel = PaymentOptionsViewModel(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        stripeAccountId = null,
        args = PaymentOptionsActivityStarter.Args(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            paymentMethods = emptyList(),
            config = PaymentSheet.Configuration(
                googlePay = ConfigFixtures.GOOGLE_PAY
            )
        ),
        googlePayRepository = FakeGooglePayRepository(false)
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
                config = PaymentSheet.Configuration(
                    googlePay = ConfigFixtures.GOOGLE_PAY
                )
            )
        )

        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

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
