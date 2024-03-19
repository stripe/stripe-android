package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BancontactDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormArgumentsFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `Create correct FormArguments for new generic payment method with customer requested save`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "bancontact")
            )
        )
        val actualArgs = FormArgumentsFactory.create(
            paymentMethod = metadata.supportedPaymentMethodForCode("bancontact", context)!!,
            metadata = metadata,
            customerConfig = null,
        )

        assertThat(actualArgs.showCheckbox).isFalse()
    }

    @Test
    fun `Create correct FormArguments for null newLpm with setup intent and paymentMethod not supportedAsSavedPaymentMethod`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("bancontact")
            ),
        )
        assertThat(BancontactDefinition.supportedAsSavedPaymentMethod).isFalse()
        val supportedPaymentMethod = metadata.supportedPaymentMethodForCode("bancontact", context)!!

        val actualArgs = FormArgumentsFactory.create(
            paymentMethod = supportedPaymentMethod,
            metadata = metadata,
            customerConfig = null,
        )

        assertThat(actualArgs.showCheckbox).isFalse()
    }

    @Test
    fun `Create correct FormArguments with custom billing details collection`() {
        val actualFromArguments = testCardFormArguments(
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
        config: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
    ): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethod = LpmRepositoryTestHelpers.card,
            metadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
            ),
            customerConfig = config.customer,
        )
    }
}
