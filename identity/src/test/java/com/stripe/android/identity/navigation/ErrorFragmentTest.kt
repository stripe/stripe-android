package com.stripe.android.identity.navigation

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ErrorFragmentBinding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorFragmentTest {

    @Test
    fun `title and content are set correctly`() {
        launchErrorFragment().onFragment {
            val binding = ErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.errorTitle.text).isEqualTo(TEST_ERROR_TITLE)
            assertThat(binding.errorContent.text).isEqualTo(TEST_ERROR_CONTENT)
        }
    }

    @Test
    fun `go back button is hidden correctly when not set`() {
        launchErrorFragment().onFragment {
            assertThat(ErrorFragmentBinding.bind(it.requireView()).goBack.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `go back button is set correctly when set`() {
        launchErrorFragment(R.id.action_errorFragment_to_consentFragment).onFragment {
            val binding = ErrorFragmentBinding.bind(it.requireView())

            assertThat(ErrorFragmentBinding.bind(it.requireView()).goBack.visibility).isEqualTo(View.VISIBLE)
        }
    }

    private fun launchErrorFragment(
        navigationDestination: Int? = null
    ) = launchFragmentInContainer(
        bundleOf(
            ErrorFragment.ARG_ERROR_TITLE to TEST_ERROR_TITLE,
            ErrorFragment.ARG_ERROR_CONTENT to TEST_ERROR_CONTENT
        ).also { bundle ->
            navigationDestination?.let {
                bundle.putInt(ErrorFragment.ARG_GO_BACK_BUTTON_DESTINATION, navigationDestination)
            }
        },
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ErrorFragment()
    }

    private companion object {
        const val TEST_ERROR_TITLE = "test error title"
        const val TEST_ERROR_CONTENT = "test error content"
    }
}
