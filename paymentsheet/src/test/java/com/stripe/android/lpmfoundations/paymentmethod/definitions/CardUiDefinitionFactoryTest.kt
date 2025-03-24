package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA_PERMISSIONS
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.screenshottesting.LayoutDirection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Rule
import org.junit.Test

class CardUiDefinitionFactoryTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @get:Rule
    val rightToLeftPaparazziRule = PaparazziRule(
        listOf(LayoutDirection.RightToLeft)
    )

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
                    billingDetails = null,
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
                    customerMetadata = CustomerMetadata(
                        hasCustomerConfiguration = true,
                        isPaymentMethodSetAsDefaultEnabled = false,
                        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
                    ),
                )
            )
        }
    }

    @Test
    fun testCardWithSaveForLaterAndSetAsDefaultShown() {
        val formElements = CardDefinition.formElements(
            metadata = metadata.copy(
                customerMetadata = CustomerMetadata(
                    hasCustomerConfiguration = true,
                    isPaymentMethodSetAsDefaultEnabled = true,
                    permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
                ),
            )
        )

        val saveForFutureUseElement = formElements.first {
            it.identifier == IdentifierSpec.SaveForFutureUse
        } as SaveForFutureUseElement

        saveForFutureUseElement.controller.onValueChange(true)

        paparazziRule.snapshot {
            FormUI(
                hiddenIdentifiers = emptySet(),
                enabled = true,
                elements = formElements,
                lastTextFieldIdentifier = null,
            )
        }
    }

    @Test
    fun testCardWithSameAsShipping() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    shippingDetails = AddressDetails(
                        address = PaymentSheet.Address(
                            line1 = "354 Oyster Point Blvd",
                            city = "South San Francisco",
                            state = "CA",
                            country = "US",
                            postalCode = "94080",
                        ),
                        isCheckboxSelected = true
                    ),
                )
            )
        }
    }

    @Test
    fun testCardWithMandate() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf("card"),
                    ),
                ),
            )
        }
    }

    @Test
    fun testCardWithMandateAndSaveForLater() {
        paparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                metadata = metadata.copy(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf("card"),
                    ),
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    customerMetadata = CustomerMetadata(
                        hasCustomerConfiguration = true,
                        isPaymentMethodSetAsDefaultEnabled = false,
                        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
                    ),
                ),
            )
        }
    }

    @Test
    fun testCardRightToLeft() {
        rightToLeftPaparazziRule.snapshot {
            CardDefinition.CreateFormUi(
                paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
                    code = "card",
                    billingDetails = null,
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
                            "name" to "John Doe",
                            "phone" to "+11234567890",
                            "address" to mapOf(
                                "country" to "US",
                                "line1" to "123 Apple Street",
                                "state" to "CA",
                                "city" to "South San Francisco",
                                "postal_code" to "94080",
                            )
                        ),
                    ),
                    productUsage = emptySet(),
                ),
                metadata = metadata.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    ),
                ),
            )
        }
    }
}
