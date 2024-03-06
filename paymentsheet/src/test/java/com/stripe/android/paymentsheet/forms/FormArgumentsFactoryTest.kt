package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.update
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BancontactDefinition
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SharedDataSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(RobolectricTestRunner::class)
class FormArgumentsFactoryTest {

    private val resources = ApplicationProvider.getApplicationContext<Application>().resources

    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(resources)
    ).apply {
        this.update(
            PaymentIntentFactory.create(
                paymentMethodTypes =
                listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.USBankAccount.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Bancontact.code
                )
            ),
            null
        )
    }

    @Test
    fun `Create correct FormArguments for new generic payment method with customer requested save`() {
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            code = "bancontact",
            requiresMandate = true,
            overrideParamMap = mapOf(
                "type" to "bancontact",
                "billing_details" to mapOf("name" to "Jenny Rosen"),
            ),
            productUsage = emptySet(),
        )

        val actualArgs = FormArgumentsFactory.create(
            paymentMethod = lpmRepository.fromCode("bancontact")!!,
            metadata = PaymentMethodMetadataFactory.create(),
            config = PaymentSheetFixtures.CONFIG_MINIMUM,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            amount = Amount(50, "USD"),
            newLpm = PaymentSelection.New.GenericPaymentMethod(
                labelResource = resources.getString(StripeUiCoreR.string.stripe_paymentsheet_payment_method_bancontact),
                iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_bancontact,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = paymentMethodCreateParams,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(actualArgs.initialPaymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)
        assertThat(actualArgs.showCheckbox).isFalse()
        assertThat(actualArgs.saveForFutureUseInitialValue).isFalse()
    }

    @Test
    fun `Create correct FormArguments for null newLpm with setup intent and paymentMethod not supportedAsSavedPaymentMethod`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(BancontactDefinition.supportedAsSavedPaymentMethod).isFalse()
        val supportedPaymentMethod = BancontactDefinition.supportedPaymentMethod(
            metadata = metadata,
            sharedDataSpec = SharedDataSpec("bancontact"),
        )

        val actualArgs = FormArgumentsFactory.create(
            paymentMethod = supportedPaymentMethod,
            metadata = metadata,
            config = PaymentSheetFixtures.CONFIG_MINIMUM,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            amount = null,
            newLpm = null,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(actualArgs.showCheckbox).isFalse()
        assertThat(actualArgs.saveForFutureUseInitialValue).isFalse()
    }

    @Test
    fun `Create correct FormArguments for new card with customer requested save`() {
        val actualFromArguments = testCardFormArguments(
            customerReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        assertThat(actualFromArguments.saveForFutureUseInitialValue).isTrue()
    }

    @Test
    fun `Create correct FormArguments for new card with no requested save`() {
        val actualFromArguments = testCardFormArguments(
            customerReuse = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        assertThat(actualFromArguments.saveForFutureUseInitialValue).isFalse()
    }

    @Test
    fun `Create correct FormArguments with custom billing details collection`() {
        val actualFromArguments = testCardFormArguments(
            customerReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
            config = PaymentSheetFixtures.CONFIG_BILLING_DETAILS_COLLECTION,
        )

        assertThat(actualFromArguments.billingDetailsCollectionConfiguration).isEqualTo(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
            )
        )
    }

    private fun testCardFormArguments(
        customerReuse: PaymentSelection.CustomerRequestedSave,
        config: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
    ): FormArguments {
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            code = "card",
            requiresMandate = false,
            overrideParamMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                    "cvc" to "123",
                    "number" to "4242424242424242",
                    "exp_date" to "1250"
                ),
                "billing_details" to mapOf(
                    "address" to mapOf(
                        "country" to "Jenny Rosen"
                    )
                ),
            ),
            productUsage = emptySet()
        )

        val actualArgs = FormArgumentsFactory.create(
            paymentMethod = LpmRepositoryTestHelpers.card,
            metadata = PaymentMethodMetadataFactory.create(),
            config = config,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            amount = Amount(50, "USD"),
            newLpm = PaymentSelection.New.Card(
                paymentMethodCreateParams = paymentMethodCreateParams,
                brand = CardBrand.Visa,
                customerRequestedSave = customerReuse
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(actualArgs.initialPaymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)

        return actualArgs
    }
}
