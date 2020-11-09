package com.stripe.android.paymentsheet

import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetAddCardFragmentTest {

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `shouldSavePaymentMethod should default to true when in default mode`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.DEFAULT_ARGS
            )
            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    @Test
    fun `shouldSavePaymentMethod should be false when checkbox is unchecked`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.DEFAULT_ARGS
            )

            val checkbox = fragment.requireView().findViewById<CheckBox>(R.id.save_card_checkbox)
            checkbox.isChecked = false

            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isFalse()
        }
    }

    @Test
    fun `shouldSavePaymentMethod should default to false when in guest mode`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(
                fragment,
                PaymentSheetFixtures.GUEST_ARGS
            )
            assertThat(activityViewModel.shouldSavePaymentMethod)
                .isTrue()
        }
    }

    private fun activityViewModel(
        fragment: PaymentSheetAddCardFragment,
        args: PaymentSheetActivityStarter.Args
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { args }
            )
        }.value
    }

    private fun createScenario(): FragmentScenario<PaymentSheetAddCardFragment> {
        return launchFragmentInContainer<PaymentSheetAddCardFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_STARTER_ARGS to PaymentSheetFixtures.DEFAULT_ARGS
            ),
            R.style.StripeDefaultTheme
        )
    }
}
