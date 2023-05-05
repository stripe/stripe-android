package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.model.PaymentMethod.Type.CashAppPay
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.ContactInformationSpec
import com.stripe.android.ui.core.elements.EmptyFormSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = ApplicationProvider.getApplicationContext<Application>().resources,
            isFinancialConnectionsAvailable = { true },
            enableACHV2InDeferredFlow = true
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
        lpmRepository.supportedPaymentMethodTypes.forEach { code ->
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
            PaymentIntentFactory.create(paymentMethodTypes = emptyList()).copy(
                paymentMethodOptionsJsonString = """
                    {
                        "us_bank_account": {
                            "verification_method": "automatic"
                        }
                    }
                """.trimIndent()
            ),
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
    fun `Verify LpmRepository filters out USBankAccount if flag is disabled`() = runTest {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                enableACHV2InDeferredFlow = false
            ),
        )

        val deferredPaymentIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "us_bank_account", "cashapp"),
        ).copy(
            clientSecret = null,
        )

        lpmRepository.update(
            stripeIntent = deferredPaymentIntent,
            serverLpmSpecs = null,
        )

        val supportedPaymentMethods = lpmRepository.values().map { it.code }
        assertThat(supportedPaymentMethods).containsExactly(Card.code, CashAppPay.code)
    }

    @Test
    fun `Verify LpmRepository filters out USBankAccount if verification method is unsupported`() = runTest {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                enableACHV2InDeferredFlow = true
            ),
        )

        val deferredPaymentIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "us_bank_account", "cashapp"),
        ).copy(
            clientSecret = null,
            paymentMethodOptionsJsonString = """
                {
                    "us_bank_account": {
                        "verification_method": "microdeposit"
                    }
                }
            """.trimIndent()
        )

        lpmRepository.update(
            stripeIntent = deferredPaymentIntent,
            serverLpmSpecs = null,
        )

        val supportedPaymentMethods = lpmRepository.values().map { it.code }
        assertThat(supportedPaymentMethods).containsExactly(Card.code, CashAppPay.code)
    }

    @Test
    fun `Verify LpmRepository filters out USBankAccount if verification method is supported`() = runTest {
        val lpmRepository = LpmRepository(
            lpmInitialFormData = LpmRepository.LpmInitialFormData(),
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                enableACHV2InDeferredFlow = false
            ),
        )

        val deferredPaymentIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "us_bank_account", "cashapp"),
        ).copy(
            paymentMethodOptionsJsonString = """
                {
                    "us_bank_account": {
                        "verification_method": "automatic"
                    }
                }
            """.trimIndent()
        )

        lpmRepository.update(
            stripeIntent = deferredPaymentIntent,
            serverLpmSpecs = null,
        )

        val supportedPaymentMethods = lpmRepository.values().map { it.code }
        assertThat(supportedPaymentMethods).containsExactly(Card.code, USBankAccount.code, CashAppPay.code)
    }

    @Test
    fun `Card contains fields according to billing details collection configuration`() {
        lpmRepository.update(
            PaymentIntentFactory.create(paymentMethodTypes = listOf("card")),
            """
          [
            {
                "type": "card",
                "async": false,
                "fields": []
            }
         ]
            """.trimIndent(),
            CardBillingDetailsCollectionConfiguration(
                collectName = true,
                collectEmail = true,
                collectPhone = false,
                address = CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )

        val card = lpmRepository.fromCode("card")!!
        // Contact information, Card information, Billing address and Save for future use.
        assertThat(card.formSpec.items.size).isEqualTo(4)
        assertThat(card.formSpec.items[0]).isInstanceOf(ContactInformationSpec::class.java)
        assertThat(card.formSpec.items[1]).isInstanceOf(CardDetailsSectionSpec::class.java)
        assertThat(card.formSpec.items[2]).isInstanceOf(CardBillingSpec::class.java)
        assertThat(card.formSpec.items[3]).isInstanceOf(SaveForFutureUseSpec::class.java)

        val contactInfoSpec = card.formSpec.items[0] as ContactInformationSpec
        // Name is collected in the card details section.
        assertThat(contactInfoSpec.collectName).isFalse()
        assertThat(contactInfoSpec.collectEmail).isTrue()
        assertThat(contactInfoSpec.collectPhone).isFalse()

        val cardSpec = card.formSpec.items[1] as CardDetailsSectionSpec
        assertThat(cardSpec.collectName).isTrue()

        val addressSpec = card.formSpec.items[2] as CardBillingSpec
        assertThat(addressSpec.collectionMode)
            .isEqualTo(CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }
}
