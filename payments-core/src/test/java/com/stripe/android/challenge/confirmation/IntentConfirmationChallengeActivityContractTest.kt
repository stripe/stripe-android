package com.stripe.android.challenge.confirmation

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeActivityContractTest {

    private val contract = IntentConfirmationChallengeActivityContract()

    @Test
    fun `createIntent extracts captchaVendorName from intent nextActionData`() =
        runCreateIntentScenario(captchaVendorName = "arkose") { args ->
            assertThat(args?.captchaVendorName).isEqualTo("arkose")
        }

    @Test
    fun `createIntent extracts human_security captchaVendorName`() =
        runCreateIntentScenario(captchaVendorName = "human_security") { args ->
            assertThat(args?.captchaVendorName).isEqualTo("human_security")
        }

    @Test
    fun `createIntent extracts hcaptcha captchaVendorName`() =
        runCreateIntentScenario(captchaVendorName = "hcaptcha") { args ->
            assertThat(args?.captchaVendorName).isEqualTo("hcaptcha")
        }

    @Test
    fun `createIntent returns null captchaVendorName when nextActionData is not IntentConfirmationChallenge`() =
        runCreateIntentScenario(captchaVendorName = null) { args ->
            assertThat(args?.captchaVendorName).isNull()
        }

    @Test
    fun `createIntent passes publishableKey`() =
        runCreateIntentScenario { args ->
            assertThat(args?.publishableKey).isEqualTo(PUBLISHABLE_KEY)
        }

    @Test
    fun `createIntent passes productUsage as list`() =
        runCreateIntentScenario { args ->
            assertThat(args?.productUsage).isEqualTo(PRODUCT_USAGE.toList())
        }

    @Test
    fun `createIntent passes intent`() =
        runCreateIntentScenario { args ->
            assertThat(args?.intent).isEqualTo(PaymentIntentFixtures.PI_SUCCEEDED)
        }

    @Test
    fun `parseResult returns Success result`() {
        val expected = IntentConfirmationChallengeActivityResult.Success(clientSecret = "pi_secret_123")

        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(IntentConfirmationChallengeActivityContract.EXTRA_RESULT to expected)
                )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `parseResult returns Canceled result`() {
        val expected = IntentConfirmationChallengeActivityResult.Canceled(clientSecret = "pi_secret_123")

        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(IntentConfirmationChallengeActivityContract.EXTRA_RESULT to expected)
                )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `parseResult returns Failed result`() {
        val expected = IntentConfirmationChallengeActivityResult.Failed(
            clientSecret = "pi_secret_123",
            error = Throwable("Something went wrong")
        )

        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(
                    bundleOf(IntentConfirmationChallengeActivityContract.EXTRA_RESULT to expected)
                )
        )

        assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
        val failed = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failed.clientSecret).isEqualTo("pi_secret_123")
        assertThat(failed.error).hasMessageThat().isEqualTo("Something went wrong")
    }

    @Test
    fun `parseResult returns Failed when intent is null`() {
        val result = contract.parseResult(Activity.RESULT_OK, null)

        assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
        val failed = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failed.clientSecret).isNull()
        assertThat(failed.error).hasMessageThat().isEqualTo("No result")
    }

    @Test
    fun `parseResult returns Failed when intent has no extras`() {
        val result = contract.parseResult(Activity.RESULT_OK, Intent())

        assertThat(result).isInstanceOf(IntentConfirmationChallengeActivityResult.Failed::class.java)
        val failed = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failed.clientSecret).isNull()
        assertThat(failed.error).hasMessageThat().isEqualTo("No result")
    }

    private fun runCreateIntentScenario(
        captchaVendorName: String? = null,
        block: (IntentConfirmationChallengeArgs?) -> Unit,
    ) {
        val stripeIntent = if (captchaVendorName != null) {
            PaymentIntentFixtures.PI_SUCCEEDED.copy(
                nextActionData = IntentConfirmationChallenge(
                    stripeJs = IntentConfirmationChallenge.StripeJs(
                        captchaVendorName = captchaVendorName
                    )
                )
            )
        } else {
            PaymentIntentFixtures.PI_SUCCEEDED
        }

        val result = contract.createIntent(
            ApplicationProvider.getApplicationContext(),
            IntentConfirmationChallengeActivityContract.Args(
                publishableKey = PUBLISHABLE_KEY,
                productUsage = PRODUCT_USAGE,
                intent = stripeIntent
            )
        )

        val args = result.extras?.let {
            BundleCompat.getParcelable(
                it,
                IntentConfirmationChallengeActivity.EXTRA_ARGS,
                IntentConfirmationChallengeArgs::class.java
            )
        }

        block(args)
    }

    private companion object {
        const val PUBLISHABLE_KEY = "pk_test_123"
        val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
