package com.stripe.android.googlepaysheet

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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultGooglePaySheetLauncherTest {
    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `init and present should return empty results`() {
        val testRegistry = FakeActivityResultRegistry(GooglePaySheetResult.Canceled)

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<GooglePaySheetResult>()
                val launcher = DefaultGooglePaySheetLauncher(fragment, testRegistry) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)

                // this will be empty until DefaultGooglePaySheetLauncher is implemented
                assertThat(results)
                    .isEmpty()
            }
        }
    }

    private class FakeActivityResultRegistry(
        private val result: GooglePaySheetResult
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
}
