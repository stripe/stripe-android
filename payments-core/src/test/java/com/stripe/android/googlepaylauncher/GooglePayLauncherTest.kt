package com.stripe.android.googlepaylauncher

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
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.GooglePayRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    private val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
        TestFragment()
    }

    private val readyCallbackInvocations = mutableListOf<Boolean>()
    private val results = mutableListOf<GooglePayLauncher.Result>()

    private val readyCallback = GooglePayLauncher.ReadyCallback(readyCallbackInvocations::add)
    private val resultCallback = GooglePayLauncher.ResultCallback(results::add)

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `present() should successfully return a result when Google Pay is available`() {
        scenario.onFragment { fragment ->
            val launcher = GooglePayLauncher(
                testScope,
                CONFIG,
                { FakeGooglePayRepository(true) },
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayLauncherContract(),
                    FakeActivityResultRegistry(GooglePayLauncher.Result.Completed)
                ) {
                    resultCallback.onResult(it)
                }
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(readyCallbackInvocations)
                .containsExactly(true)

            launcher.present("pi_123_secret_456")

            assertThat(results)
                .containsExactly(GooglePayLauncher.Result.Completed)
        }
    }

    @Test
    fun `present() should throw IllegalStateException when Google Pay is not available`() {
        scenario.onFragment { fragment ->
            val launcher = GooglePayLauncher(
                testScope,
                CONFIG,
                { FakeGooglePayRepository(false) },
                readyCallback,
                fragment.registerForActivityResult(
                    GooglePayLauncherContract(),
                    FakeActivityResultRegistry(GooglePayLauncher.Result.Completed)
                ) {
                    resultCallback.onResult(it)
                }
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(readyCallbackInvocations)
                .containsExactly(false)

            assertFailsWith<IllegalStateException> {
                launcher.present("pi_123")
            }
        }
    }

    private class FakeGooglePayRepository(
        private var isReady: Boolean
    ) : GooglePayRepository {
        override fun isReady(): Flow<Boolean> = flowOf(isReady)
    }

    private class FakeActivityResultRegistry(
        private val result: GooglePayLauncher.Result
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
        val CONFIG = GooglePayLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
