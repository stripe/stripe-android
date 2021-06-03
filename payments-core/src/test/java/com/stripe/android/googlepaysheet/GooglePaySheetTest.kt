package com.stripe.android.googlepaysheet

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
class GooglePaySheetTest {

    @Test
    fun `no-op should not return any results`() {
        val results = mutableListOf<GooglePaySheetResult>()
        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        scenario.onFragment { fragment ->
            GooglePaySheet(fragment) { result ->
                results.add(result)
            }
        }

        assertThat(results)
            .isEmpty()
    }

    @Test
    fun `configure() should invoke callback with expected value`() {
        val launcher = FakeGooglePaySheetLauncher(true)

        val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }

        val configResults = mutableListOf<Boolean>()
        scenario.onFragment { fragment ->
            val sheet = GooglePaySheet(
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

    internal class FakeGooglePaySheetLauncher(
        private val isConfigured: Boolean
    ) : GooglePaySheetLauncher {
        override suspend fun configure(
            configuration: GooglePaySheetConfig
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
        val CONFIG = GooglePaySheetConfig(
            GooglePaySheetEnvironment.Test,
            amount = 1000,
            countryCode = "US",
            currencyCode = "usd"
        )
    }
}
