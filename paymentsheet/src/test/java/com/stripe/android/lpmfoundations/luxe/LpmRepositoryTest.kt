package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository()

    private val paymentIntentWithShipping = PaymentIntentFactory.create(
        paymentMethodTypes = listOf("card", "afterpay_clearpay", "affirm"),
    ).copy(
        shipping = PaymentIntent.Shipping(
            name = "Example buyer",
            address = Address(line1 = "123 Main st.", country = "US", postalCode = "12345"),
        )
    )

    @Test
    fun `Verify failing to read server schema reads from disk`() {
        lpmRepository.getSharedDataSpecs(
            paymentIntentWithShipping,
            """
          [
            {
                "type": "affirm",
                invalid schema
              }
         ]
            """.trimIndent()
        ).verifyContainsTypes("affirm")
    }

    @Test
    fun `Verify field not found in schema is read from disk`() {
        lpmRepository.getSharedDataSpecs(
            paymentIntentWithShipping,
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
        ).verifyContainsTypes("afterpay_clearpay")
    }

    @Test
    fun `Verify latest server spec`() {
        lpmRepository.getSharedDataSpecs(
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    "bancontact",
                    "ideal",
                    "sepa_debit",
                    "p24",
                    "eps",
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
        ).verifyContainsTypes("ideal", "bancontact", "p24", "eps")
    }

    @Test
    fun `Repository will contain LPMs in ordered and schema`() {
        lpmRepository.getSharedDataSpecs(
            paymentIntentWithShipping,
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
        ).verifyContainsTypes("afterpay_clearpay", "affirm")
    }

    @Test
    fun `Verify the repository only shows card if in lpms json`() {
        lpmRepository.getSharedDataSpecs(
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
        ).verifyDoesNotContainTypes("card")
    }

    @Test
    fun `Verify that unknown LPMs are shown`() {
        lpmRepository.getSharedDataSpecs(
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
        ).verifyContainsTypes("unknown_lpm")
    }

    @Test
    fun `getSharedDataSpecs load from network`() {
        val serverSpecs = """
            [
                {
                    "type": "card",
                    "async": false,
                    "fields": []
                },
                {
                    "type": "cashapp",
                    "async": false,
                    "fields": [],
                    "selector_icon": {
                        "light_theme_png": "https://js.stripe.com/cashapptest.png",
                        "light_theme_svg": "https://js.stripe.com/cashapptest.svg"
                    }
                }
            ]
        """.trimIndent()
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp")
        )
        val result = lpmRepository.getSharedDataSpecs(paymentIntent, serverSpecs)
        assertThat(result.failedToParseServerResponse).isFalse()
        val sharedDataSpecs = result.sharedDataSpecs
        assertThat(sharedDataSpecs).hasSize(2)
        assertThat(sharedDataSpecs[0].type).isEqualTo("card")
        assertThat(sharedDataSpecs[1].type).isEqualTo("cashapp")
        assertThat(sharedDataSpecs[1].selectorIcon?.lightThemePng)
            .isEqualTo("https://js.stripe.com/cashapptest.png")
    }

    @Test
    fun `getSharedDataSpecs loads missing LPMs from disk`() {
        val serverSpecs = """
            [
                {
                    "type": "card",
                    "async": false,
                    "fields": []
                }
            ]
        """.trimIndent()
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp")
        )
        val result = lpmRepository.getSharedDataSpecs(paymentIntent, serverSpecs)
        assertThat(result.failedToParseServerResponse).isFalse()
        val sharedDataSpecs = result.sharedDataSpecs
        assertThat(sharedDataSpecs).hasSize(2)
        assertThat(sharedDataSpecs[0].type).isEqualTo("card")
        assertThat(sharedDataSpecs[1].type).isEqualTo("cashapp")
    }

    @Test
    fun `getSharedDataSpecs loads from disk when server specs don't load`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp")
        )
        val result = lpmRepository.getSharedDataSpecs(paymentIntent, null)
        assertThat(result.failedToParseServerResponse).isFalse()
        val sharedDataSpecs = result.sharedDataSpecs
        assertThat(sharedDataSpecs).hasSize(1)
        assertThat(sharedDataSpecs[0].type).isEqualTo("cashapp")
    }

    @Test
    fun `getSharedDataSpecs loads from disk when server specs are malformed`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "cashapp")
        )
        val result = lpmRepository.getSharedDataSpecs(paymentIntent, "{[]}")
        assertThat(result.failedToParseServerResponse).isTrue()
        val sharedDataSpecs = result.sharedDataSpecs
        assertThat(sharedDataSpecs).hasSize(1)
        assertThat(sharedDataSpecs[0].type).isEqualTo("cashapp")
    }

    private fun LpmRepository.Result.verifyContainsTypes(vararg expectedTypes: String) {
        val actualTypes = sharedDataSpecs.map { it.type }
        for (expectedType in expectedTypes) {
            assertThat(actualTypes).contains(expectedType)
        }
    }

    private fun LpmRepository.Result.verifyDoesNotContainTypes(vararg expectedMissingTypes: String) {
        val actualTypes = sharedDataSpecs.map { it.type }
        for (expectedMissingType in expectedMissingTypes) {
            assertThat(actualTypes).doesNotContain(expectedMissingType)
        }
    }
}
