package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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
internal class AutomaticTaxBillingAddressPayloadTest {
    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `Cash App automatic tax billing address reaches create params`() = runTest {
        val billingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                line1 = "510 Townsend Street",
                city = "San Francisco",
                state = "CA",
                country = "US",
                postalCode = "94103",
            ),
        )
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            attachDefaultsToPaymentMethod = false,
        )
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(PaymentMethod.Type.CashAppPay.code),
            ),
            billingDetailsCollectionConfiguration = collectionConfiguration,
            defaultBillingDetails = billingDetails,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            ),
        )
        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = PaymentMethod.Type.CashAppPay.code,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )
        val formViewModel = FormViewModel(
            formElements = formElements,
            formArguments = COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = PaymentMethod.Type.CashAppPay.code,
                billingDetails = billingDetails,
                billingDetailsCollectionConfiguration = collectionConfiguration,
            ),
        ).also { viewModelStoreRule.track(it) }

        val completeFormValues = requireNotNull(formViewModel.completeFormValues.first())
        val createParams = completeFormValues.transformToPaymentMethodCreateParams(
            paymentMethodCode = PaymentMethod.Type.CashAppPay.code,
            paymentMethodMetadata = metadata,
        )
        val address = requireNotNull(createParams.billingDetails?.address)

        assertThat(address.line1).isEqualTo("510 Townsend Street")
        assertThat(address.city).isEqualTo("San Francisco")
        assertThat(address.state).isEqualTo("CA")
        assertThat(address.country).isEqualTo("US")
        assertThat(address.postalCode).isEqualTo("94103")
    }
}
