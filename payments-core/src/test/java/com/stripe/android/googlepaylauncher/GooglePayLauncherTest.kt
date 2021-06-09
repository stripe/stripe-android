package com.stripe.android.googlepaylauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherTest {

    @Test
    fun `no-op should not return any results`() {
        val results = mutableListOf<GooglePayLauncherResult>()
        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        scenario.onFragment { fragment ->
            GooglePayLauncher(fragment) { result ->
                results.add(result)
            }
        }

        assertThat(results)
            .isEmpty()
    }

    @Test
    fun `configure() should invoke callback with expected value`() {
        val launcher = FakeGooglePayController(true)

        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        val configResults = mutableListOf<Boolean>()
        scenario.onFragment { fragment ->
            val sheet = GooglePayLauncher(
                { fragment.viewLifecycleOwner.lifecycleScope },
                launcher,
            )
            scenario.moveToState(Lifecycle.State.RESUMED)

            sheet.configure(CONFIG) { success, _ ->
                configResults.add(success)
            }
        }

        assertThat(configResults)
            .containsExactly(true)
    }

    internal class FakeGooglePayController(
        private val isConfigured: Boolean
    ) : GooglePayController {
        override suspend fun configure(
            configuration: GooglePayConfig
        ): Boolean = isConfigured

        override fun present() {
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
