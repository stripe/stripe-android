package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class CardUiDefinitionFactoryTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    private val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card"),
        )
    )

    @Test
    fun testCard() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata
            )
        }
    }

    @Test
    fun testCardWithDefaultValues() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata,
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    requiresMandate = false,
                    overrideParamMap = mapOf(
                        "type" to "card",
                        "card" to mapOf(
                            "number" to "4242424242424242",
                            "exp_month" to "07",
                            "exp_year" to "2050",
                            "cvc" to "123",
                        ),
                        "billing_details" to mapOf(
                            "country" to "US",
                            "postal_code" to "94080",
                        ),
                    ),
                    productUsage = emptySet(),
                )
            )
        }
    }

    @Test
    fun testCardWithBillingDetailsCollectionConfiguration() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                )
            )
        }
    }

    @Test
    fun testCardWithSaveForLater() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    hasCustomerConfiguration = true,
                )
            )
        }
    }

    @Test
    fun testCardWithSameAsShipping() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    shippingDetails = AddressDetails(isCheckboxSelected = true),
                )
            )
        }
    }
}
