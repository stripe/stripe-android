package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.utils.PaymentIntentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = ApplicationProvider.getApplicationContext<Application>().resources,
            isFinancialConnectionsAvailable = { true }
        )
    )

    @Test
    fun `Test label for afterpay show correctly when clearpay string`() {
        Locale.setDefault(Locale.UK)
        val lpmRepository = LpmRepository(
            LpmRepository.LpmRepositoryArguments(
                ApplicationProvider.getApplicationContext<Application>().resources
            )
        )
        lpmRepository.updateFromDisk(PaymentIntentFactory.create())
        assertThat(lpmRepository.fromCode("afterpay_clearpay")?.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_clearpay)

        Locale.setDefault(Locale.US)
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        Locale.setDefault(Locale.US)
        val lpmRepository = LpmRepository(
            LpmRepository.LpmRepositoryArguments(
                ApplicationProvider.getApplicationContext<Application>().resources
            )
        )
        lpmRepository.updateFromDisk(PaymentIntentFactory.create())
        assertThat(lpmRepository.fromCode("afterpay_clearpay")?.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_afterpay)
    }

    @Test
    fun `Verify failing to read server schema reads from disk`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = listOf("affirm")),
            """
          [
            {
                "type": "affirm",
                invalid schema
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("affirm")).isNotNull()
    }

    @Test
    fun `Verify field not found in schema is read from disk`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = listOf("card", "afterpay_clearpay")),
            """
          [
            {
                "type": "afterpay_clearpay",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")).isNotNull()
        assertThat(lpmRepository.fromCode("card")).isNotNull()
    }

    @Test
    fun `Verify latest server spec`() {
        lpmRepository.update(
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    "bancontact",
                    "sofort",
                    "ideal",
                    "sepa_debit",
                    "p24",
                    "eps",
                    "giropay"
                )
            ),
            """
                [
                  {
                    "type": "an lpm",
                    "async": false,
                    "fields": [
                      {
                        "api_path": null,
                        "type": "afterpay_header"
                      }
                    ]
                  }
                ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("sofort")).isNotNull()
        assertThat(lpmRepository.fromCode("ideal")).isNotNull()
        assertThat(lpmRepository.fromCode("bancontact")).isNotNull()
        assertThat(lpmRepository.fromCode("p24")).isNotNull()
        assertThat(lpmRepository.fromCode("eps")).isNotNull()
        assertThat(lpmRepository.fromCode("giropay")).isNotNull()
    }

    @Test
    fun `Repository will contain LPMs in ordered and schema`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = listOf("afterpay_clearpay")),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
            },
            {
                "type": "afterpay_clearpay",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")).isNotNull()
        assertThat(lpmRepository.fromCode("affirm")).isNotNull()
    }

    @Test
    fun `Verify no fields in the default json are ignored the lpms package should be correct`() {
        lpmRepository.updateFromDisk(PaymentIntentFactory.create())
        // If this test fails, check to make sure the spec's serializer is added to
        // FormItemSpecSerializer
        lpmRepository.supportedPaymentMethods.forEach { code ->
            if (!hasEmptyForm(code)) {
                assertThat(
                    lpmRepository.fromCode(code)!!.formSpec.items
                        .filter {
                            it is EmptyFormSpec && !hasEmptyForm(code)
                        }

                ).isEmpty()
            }
        }
    }

    private fun hasEmptyForm(code: String) =
        (code == "paypal" || code == "us_bank_account") &&
            lpmRepository.fromCode(code)!!.formSpec.items.size == 1 &&
            lpmRepository.fromCode(code)!!.formSpec.items.first() == EmptyFormSpec

    @Test
    fun `Verify the repository only shows card if in lpms json`() {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                isFinancialConnectionsAvailable = { true }
            )
        )

        assertThat(lpmRepository.fromCode("card")).isNull()
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = emptyList()),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("card")).isNull()
    }

    @Test
    fun `Verify that unknown LPMs are not shown because not listed as exposed`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = emptyList()),
            """
              [
                {
                    "type": "affirm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  },
                {
                    "type": "unknown_lpm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  }
             ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("unknown_lpm")).isNull()
    }

    @Test
    fun `Verify that payment methods hardcoded to delayed remain regardless of json`() {
        val stripeIntent = PaymentIntentFactory.create()
        lpmRepository.updateFromDisk(stripeIntent)
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)

        lpmRepository.update(
            stripeIntent,
            """
              [
                {
                    "type": "sofort",
                    "async": false,
                    "fields": []
                  }
             ]
            """.trimIndent()
        )
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)
    }

    @Test
    fun `Verify that us_bank_account is supported when financial connections sdk available`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = emptyList()),
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNotNull()
    }

    @Test
    fun `Verify that us_bank_account not supported when financial connections sdk not available`() {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                isFinancialConnectionsAvailable = { false }
            )
        )

        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = emptyList()),
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNull()
    }

    @Test
    fun `Verify that UPI is supported when it's expected`() {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                isFinancialConnectionsAvailable = { false },
            )
        )

        lpmRepository.update(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("upi")),
            serverLpmSpecs = "[]" // UPI doesn't come from the backend; we rely on the local specs
        )

        assertThat(lpmRepository.fromCode("upi")).isNotNull()
    }

    @Test
    fun `Verify LpmRepository waitUntilLoaded completes after update`() = runTest(
        context = TestCoroutineScheduler(),
        dispatchTimeoutMs = TimeUnit.SECONDS.toMillis(1)

    ) {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                isFinancialConnectionsAvailable = { false },
            )
        )

        assertThat(lpmRepository.isLoaded()).isFalse()

        val job = launch(Dispatchers.IO) {
            lpmRepository.waitUntilLoaded()
        }

        // Allow for the waitUntilLoaded to kick off.
        advanceTimeBy(1)

        lpmRepository.update(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("upi")),
            serverLpmSpecs = "[]"
        )

        assertThat(lpmRepository.isLoaded()).isTrue()
        job.join()
        assertThat(job.isCompleted).isTrue()
    }
}
