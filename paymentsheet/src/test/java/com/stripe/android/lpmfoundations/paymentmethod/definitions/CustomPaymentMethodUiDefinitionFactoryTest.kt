package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.FormUI
import org.junit.Rule
import org.junit.Test

internal class CustomPaymentMethodUiDefinitionFactoryTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testCustomPaymentMethod() {
        paparazziRule.snapshot {
            CreateFormUi()
        }
    }

    @Test
    fun testCustomPaymentMethodWithBillingDetailsCollectionConfiguration() {
        paparazziRule.snapshot {
            CreateFormUi(
                displayableCustomPaymentMethod = createCustomPaymentMethod(doesNotCollectBillingDetails = false),
                billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                    name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        }
    }

    @Test
    fun testCustomPaymentMethodWithIgnoredBillingDetailsRequirements() {
        paparazziRule.snapshot {
            CreateFormUi(
                displayableCustomPaymentMethod = createCustomPaymentMethod(doesNotCollectBillingDetails = true),
                billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                    name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        }
    }

    @Composable
    private fun CreateFormUi(
        displayableCustomPaymentMethod: DisplayableCustomPaymentMethod = createCustomPaymentMethod(),
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration(),
        createParams: PaymentMethodCreateParams? = null
    ) {
        FormUI(
            hiddenIdentifiers = emptySet(),
            enabled = true,
            elements = CustomPaymentMethodUiDefinitionFactory(
                displayableCustomPaymentMethod = displayableCustomPaymentMethod,
            ).createFormElements(
                metadata = PaymentMethodMetadataFactory.create(),
                arguments = TestUiDefinitionFactoryArgumentsFactory.create(
                    paymentMethodCreateParams = createParams,
                ).create(
                    metadata = PaymentMethodMetadataFactory.create(
                        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                    ),
                    requiresMandate = false
                )
            ),
            lastTextFieldIdentifier = null,
        )
    }

    private fun createCustomPaymentMethod(
        doesNotCollectBillingDetails: Boolean = true
    ) = DisplayableCustomPaymentMethod(
        id = "cpmt_paypal",
        displayName = "PayPal",
        logoUrl = "example_url",
        subtitle = "Pay now with PayPal".resolvableString,
        doesNotCollectBillingDetails = doesNotCollectBillingDetails,
    )
}
