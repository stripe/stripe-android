package com.stripe.android.stripe3ds2.views

import androidx.core.content.ContextCompat
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.databinding.StripeProgressViewLayoutBinding
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ChallengeProgressFragmentTest {

    @Test
    fun `brand logo should be updated with content description`() {
        launchFragmentInContainer(
            themeResId = R.style.Stripe3ds2ProgressTheme
        ) {
            ChallengeProgressFragment(
                directoryServerName = "visa",
                sdkTransactionId = SdkTransactionId.create(),
                accentColor = ContextCompat.getColor(
                    ApplicationProvider.getApplicationContext(),
                    R.color.stripe_3ds2_accent
                )
            )
        }.onFragment { fragment ->
            val viewBinding = StripeProgressViewLayoutBinding.bind(requireNotNull(fragment.view))
            assertThat(viewBinding.brandLogo.contentDescription)
                .isEqualTo("Visa")
        }
    }
}
