package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BillingAddressPayloadTest {
    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `Klarna Full preserves billing details payload`() = runTest {
        assertFullBillingDetailsPayload(
            definition = KlarnaDefinition,
            type = PaymentMethod.Type.Klarna,
        )
    }

    @Test
    fun `Wero Full preserves billing details payload`() = runTest {
        assertFullBillingDetailsPayload(
            definition = WeroDefinition,
            type = PaymentMethod.Type.Wero,
        )
    }

    private suspend fun assertFullBillingDetailsPayload(
        definition: PaymentMethodDefinition,
        type: PaymentMethod.Type,
    ) {
        val billingDetails = PaymentSheet.BillingDetails(
            name = "Jenny Rosen",
            email = "jenny.rosen@example.com",
            phone = "+491234567890",
            address = PaymentSheet.Address(
                line1 = "Unter den Linden 1",
                line2 = "Apartment 2",
                city = "Berlin",
                state = null,
                country = "DE",
                postalCode = "10117",
            ),
        )
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(type.code),
            ),
            billingDetailsCollectionConfiguration = collectionConfiguration,
            defaultBillingDetails = billingDetails,
        )
        val formViewModel = FormViewModel(
            formElements = definition.formElements(metadata),
            formArguments = COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = type.code,
                billingDetails = billingDetails,
                billingDetailsCollectionConfiguration = collectionConfiguration,
            ),
        ).also { viewModelStoreRule.track(it) }

        val formValues = requireNotNull(formViewModel.completeFormValues.first())
        val payload = formValues.transformToPaymentMethodCreateParams(type.code, metadata).toParamMap()

        assertThat(payload.filterKeys { it == "type" || it == "billing_details" }).isEqualTo(
            mapOf(
                "type" to type.code,
                "billing_details" to mapOf(
                    "address" to mapOf(
                        "city" to "Berlin",
                        "country" to "DE",
                        "line1" to "Unter den Linden 1",
                        "line2" to "Apartment 2",
                        "postal_code" to "10117",
                    ),
                    "email" to "jenny.rosen@example.com",
                    "name" to "Jenny Rosen",
                    "phone" to "+491234567890",
                ),
            ),
        )
    }
}
