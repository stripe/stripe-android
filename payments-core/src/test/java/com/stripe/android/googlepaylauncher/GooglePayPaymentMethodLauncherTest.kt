package com.stripe.android.googlepaylauncher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
        TestFragment()
    }

    private val readyCallbackInvocations = mutableListOf<Boolean>()
    private val results = mutableListOf<GooglePayPaymentMethodLauncher.Result>()

    private val readyCallback =
        GooglePayPaymentMethodLauncher.ReadyCallback(readyCallbackInvocations::add)
    private val resultCallback = GooglePayPaymentMethodLauncher.ResultCallback(results::add)

    val context: Context = ApplicationProvider.getApplicationContext()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        context,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
    )
    private val firedEvents = mutableListOf<String>()
    private val analyticsRequestExecutor = AnalyticsRequestExecutor {
        firedEvents.add(it.params["event"].toString())
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `present() should successfully return a result when Google Pay is available`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        scenario.onFragment { fragment ->
            val launcher = GooglePayPaymentMethodLauncher(
                testScope,
                CONFIG,
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayPaymentMethodLauncherContract(),
                    FakeActivityResultRegistry(result)
                ) {
                    resultCallback.onResult(it)
                },
                false,
                context,
                { FakeGooglePayRepository(true) },
                emptySet(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                { null },
                analyticsRequestFactory = analyticsRequestFactory,
                analyticsRequestExecutor = analyticsRequestExecutor
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(readyCallbackInvocations)
                .containsExactly(true)

            launcher.present(
                currencyCode = "usd"
            )

            assertThat(results)
                .containsExactly(result)
        }
    }

    @Test
    fun `init should fire expected event`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        scenario.onFragment { fragment ->
            GooglePayPaymentMethodLauncher(
                testScope,
                CONFIG,
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayPaymentMethodLauncherContract(),
                    FakeActivityResultRegistry(result)
                ) {
                    resultCallback.onResult(it)
                },
                false,
                context,
                { FakeGooglePayRepository(true) },
                emptySet(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                { null },
                analyticsRequestFactory = analyticsRequestFactory,
                analyticsRequestExecutor = analyticsRequestExecutor
            )

            assertThat(firedEvents)
                .containsExactly("stripe_android.googlepaypaymentmethodlauncher_init")
        }
    }

    @Test
    fun `present() should throw IllegalStateException when Google Pay is not available`() {
        scenario.onFragment { fragment ->
            val launcher = GooglePayPaymentMethodLauncher(
                testScope,
                CONFIG,
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayPaymentMethodLauncherContract(),
                    FakeActivityResultRegistry(
                        GooglePayPaymentMethodLauncher.Result.Completed(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD
                        )
                    )
                ) {
                    resultCallback.onResult(it)
                },
                false,
                context,
                { FakeGooglePayRepository(false) },
                emptySet(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                { null },
                analyticsRequestFactory = analyticsRequestFactory,
                analyticsRequestExecutor = analyticsRequestExecutor
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(readyCallbackInvocations)
                .containsExactly(false)

            assertFailsWith<IllegalStateException> {
                launcher.present(
                    currencyCode = "usd"
                )
            }
        }
    }

    @Test
    fun `when skipReadyCheck should not check if Google Pay is ready`() {
        scenario.onFragment { fragment ->
            val launcher = GooglePayPaymentMethodLauncher(
                testScope,
                CONFIG,
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayPaymentMethodLauncherContract(),
                    FakeActivityResultRegistry(
                        GooglePayPaymentMethodLauncher.Result.Completed(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD
                        )
                    )
                ) {
                    resultCallback.onResult(it)
                },
                true,
                context,
                { FakeGooglePayRepository(false) },
                emptySet(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                { null },
                analyticsRequestFactory = analyticsRequestFactory,
                analyticsRequestExecutor = analyticsRequestExecutor
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(readyCallbackInvocations)
                .isEmpty()

            // Should not throw error
            launcher.present(currencyCode = "usd")
        }
    }

    private class FakeActivityResultRegistry(
        private val result: GooglePayPaymentMethodLauncher.Result
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

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = FrameLayout(inflater.context)
    }

    private companion object {
        val CONFIG = GooglePayPaymentMethodLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
