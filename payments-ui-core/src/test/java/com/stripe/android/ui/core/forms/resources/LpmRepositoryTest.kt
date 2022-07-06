package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.EmptyFormSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository(
        ApplicationProvider.getApplicationContext<Application>().resources,
        object : IsFinancialConnectionsAvailable {
            override fun invoke(): Boolean {
                return true
            }
        }
    )

    @Test
    fun `Test label for afterpay show correctly when clearpay string`() {
        Locale.setDefault(Locale.UK)
        val lpmRepository = LpmRepository(
            ApplicationProvider.getApplicationContext<Application>().resources
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")?.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_clearpay)

        Locale.setDefault(Locale.US)
    }

    @Test
    fun `Test label for afterpay show correctly when afterpay string`() {
        Locale.setDefault(Locale.US)
        val lpmRepository = LpmRepository(
            ApplicationProvider.getApplicationContext<Application>().resources
        )
        assertThat(lpmRepository.fromCode("afterpay_clearpay")?.displayNameResource)
            .isEqualTo(R.string.stripe_paymentsheet_payment_method_afterpay)
    }

    @Test
    fun `Verify no fields in the default json are ignored`() {
        // If this test fails, check to make sure the spec's serializer is added to
        // FormItemSpecSerializer
        LpmRepository.exposedPaymentMethods.forEach { code ->
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
        assertThat(lpmRepository.fromCode("card")).isNotNull()
        lpmRepository.initialize(
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
            """.trimIndent().byteInputStream()
        )
        assertThat(lpmRepository.fromCode("card")).isNull()
    }

    // TODO(michelleb): Once we have the server implemented in production we can do filtering there instead
    // of in code here.
    @Test
    fun `Verify that unknown LPMs are not shown because not listed as exposed`() {
        lpmRepository.initialize(
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
            """.trimIndent().byteInputStream()
        )
        assertThat(lpmRepository.fromCode("unknown_lpm")).isNull()
    }

    @Test
    fun `Verify that payment methods hardcoded to delayed remain regardless of json`() {
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)

        lpmRepository.initialize(
            """
              [
                {
                    "type": "sofort",
                    "async": false,
                    "fields": []
                  }
             ]
            """.trimIndent().byteInputStream()
        )
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)
    }

    @Test
    fun `Verify that us_bank_account is supported when financial connections sdk available`() {
        lpmRepository.initialize(
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent().byteInputStream()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNotNull()
    }

    @Test
    fun `Verify that us_bank_account not supported when financial connections sdk not available`() {
        val lpmRepository = LpmRepository(
            ApplicationProvider.getApplicationContext<Application>().resources,
            object : IsFinancialConnectionsAvailable {
                override fun invoke(): Boolean {
                    return false
                }
            }
        )

        lpmRepository.initialize(
            """
              [
                {
                  "type": "us_bank_account"
                }
              ]
            """.trimIndent().byteInputStream()
        )

        assertThat(lpmRepository.fromCode("us_bank_account")).isNull()
    }
}
