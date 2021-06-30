package com.stripe.android.googlepaylauncher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherContractTest {
    private val scenario = launchFragmentInContainer { TestFragment() }
    private val contract = GooglePayLauncherContract()

    @Test
    fun `createIntent() should only add statusBarColor extra if available`() {
        scenario.onFragment {
            val intent = contract.createIntent(
                it.requireActivity(),
                GooglePayLauncherContract.PaymentIntentArgs(
                    "pi_123_secret_456",
                    CONFIG
                )
            )

            assertThat(intent.hasExtra(GooglePayLauncherContract.EXTRA_STATUS_BAR_COLOR))
                .isTrue()
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
