package com.stripe.android.googlepaysheet

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePaySheetTest {

    @Test
    fun `no-op should not return any results`() {
        val results = mutableListOf<GooglePaySheetResult>()
        launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
            TestFragment()
        }.onFragment { fragment ->
            GooglePaySheet(fragment) { result ->
                results.add(result)
            }
        }

        assertThat(results)
            .isEmpty()
    }

    internal class TestFragment : Fragment()
}
