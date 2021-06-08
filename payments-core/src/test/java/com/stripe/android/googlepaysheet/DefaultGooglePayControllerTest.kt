package com.stripe.android.googlepaysheet

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
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.FakeGooglePayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultGooglePayControllerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `configure() and present() when Google Pay is ready should return expected result`() {
        val testRegistry = FakeActivityResultRegistry(GooglePayLauncherResult.Canceled)

        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }
        scenario.onFragment { fragment ->
            val results = mutableListOf<GooglePayLauncherResult>()
            val launcher = DefaultGooglePayController(
                fragment,
                testRegistry,
                testDispatcher,
                { FakeGooglePayRepository(true) }
            ) {
                results.add(it)
            }
            scenario.moveToState(Lifecycle.State.RESUMED)

            testScope.launch {
                val isReady = launcher.configure(CONFIG)
                assertThat(isReady)
                    .isTrue()

                launcher.present()
            }

            assertThat(results)
                .containsExactly(
                    GooglePayLauncherResult.Canceled
                )
        }
    }

    @Test
    fun `configure() when Google Pay is not ready should throw exception`() {
        val testRegistry = FakeActivityResultRegistry(GooglePayLauncherResult.Canceled)

        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }
        scenario.onFragment { fragment ->
            val results = mutableListOf<GooglePayLauncherResult>()
            val launcher = DefaultGooglePayController(
                fragment,
                testRegistry,
                testDispatcher,
                { FakeGooglePayRepository(false) }
            ) {
                results.add(it)
            }
            scenario.moveToState(Lifecycle.State.RESUMED)

            val exception = assertFailsWith<RuntimeException> {
                runBlocking { launcher.configure(CONFIG) }
            }
            assertThat(exception.message)
                .isEqualTo("Google Pay is not available.")
        }
    }

    @Test
    fun `present() without configure() should throw error`() {
        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val launcher = DefaultGooglePayController(
                    fragment,
                    googlePayRepositoryFactory = { FakeGooglePayRepository(true) }
                ) {
                }
                moveToState(Lifecycle.State.RESUMED)

                val exception = assertFailsWith<IllegalStateException> {
                    launcher.present()
                }
                assertThat(exception.message)
                    .isEqualTo("GooglePayLauncher must be successfully initialized using configure() before calling present().")
            }
        }
    }

    private class FakeActivityResultRegistry(
        private val result: GooglePayLauncherResult
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
        val CONFIG = GooglePayConfig(
            GooglePayEnvironment.Test,
            amount = 1000,
            countryCode = "US",
            currencyCode = "usd"
        )
    }
}
