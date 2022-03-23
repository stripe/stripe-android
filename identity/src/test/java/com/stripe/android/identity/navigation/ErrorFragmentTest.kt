package com.stripe.android.identity.navigation

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorFragmentTest {

    @Test
    fun `title and content are set correctly`() {
        launchErrorFragment().onFragment {
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.titleText.text).isEqualTo(TEST_ERROR_TITLE)
            assertThat(binding.message1.text).isEqualTo(TEST_ERROR_CONTENT)
        }
    }

    @Test
    fun `go back button is hidden correctly when not set`() {
        launchErrorFragment().onFragment {
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.bottomButton.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun `go back button is set correctly when set`() {
        launchErrorFragment(R.id.action_errorFragment_to_consentFragment).onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.errorFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            val binding = BaseErrorFragmentBinding.bind(it.requireView())

            assertThat(binding.topButton.visibility).isEqualTo(View.GONE)
            assertThat(binding.bottomButton.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.bottomButton.text).isEqualTo(TEST_GO_BACK_BUTTON_TEXT)

            binding.bottomButton.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.consentFragment)
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
                bundle.putString(ErrorFragment.ARG_GO_BACK_BUTTON_TEXT, TEST_GO_BACK_BUTTON_TEXT)
            }
        },
        themeResId = R.style.Theme_MaterialComponents
    ) {
        ErrorFragment()
    }

    private companion object {
        const val TEST_ERROR_TITLE = "test error title"
        const val TEST_ERROR_CONTENT = "test error content"
        const val TEST_GO_BACK_BUTTON_TEXT = "go back"
    }
}
