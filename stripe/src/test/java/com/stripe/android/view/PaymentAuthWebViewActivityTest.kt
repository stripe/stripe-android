package com.stripe.android.view

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.payments.PaymentFlowResult
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentAuthWebViewActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = PaymentAuthWebViewContract()

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `setResult is expected value with default args`() {
        createActivityScenario { activityScenario ->
            activityScenario.onActivity {
                it.finish()
            }

            assertThat(contract.parseResult(REQUEST_CODE, activityScenario.result.resultData))
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = CLIENT_SECRET,
                        sourceId = ""
                    )
                )
        }
    }

    private fun createActivityScenario(
        onActivityScenario: (ActivityScenario<PaymentAuthWebViewActivity>) -> Unit
    ) {
        ActivityScenario.launch<PaymentAuthWebViewActivity>(
            contract.createIntent(
                context,
                ARGS
            )
        ).use {
            onActivityScenario(it)
        }
    }

    private companion object {
        private const val REQUEST_CODE = 1000
        private const val CLIENT_SECRET = "client_secret"

        private val ARGS = PaymentAuthWebViewContract.Args(
            requestCode = REQUEST_CODE,
            clientSecret = CLIENT_SECRET,
            url = "https://example.com"
        )
    }
}
