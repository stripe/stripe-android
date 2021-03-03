package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetLauncherTest {
    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `init and present should return expected PaymentResult`() {
        val testRegistry = FakeActivityResultRegistry(PAYMENT_RESULT_COMPLETED)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<PaymentResult>()
                val launcher = DefaultPaymentSheetLauncher(fragment, testRegistry) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present("pi_fake")
                assertThat(results)
                    .containsExactly(PAYMENT_RESULT_COMPLETED)
            }
        }
    }

    private class FakeActivityResultRegistry(
        private val result: PaymentResult
    ) : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            dispatchResult(
                requestCode,
                result
            )
        }
    }

    internal class TestFragment : Fragment()

    private companion object {
        private val PAYMENT_RESULT_COMPLETED = PaymentResult.Completed(
            paymentIntent = PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2
        )
    }
}
