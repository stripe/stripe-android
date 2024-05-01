package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class AddPaymentMethodTest {

    val context: Context = getApplicationContext()
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "klarna")
        ),
        externalPaymentMethodSpecs = listOf(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC)
    )

    @Test
    fun `transformToPaymentSelection transforms cards correctly`() {
        val cardBrand = "visa"
        val name = "Joe"
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.CardBrand to FormFieldEntry(cardBrand, true),
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            showsMandate = false,
            userRequestedReuse = customerRequestedSave,
        )
        val cardPaymentMethod = metadata.supportedPaymentMethodForCode("card")!!

        val cardPaymentSelection = formFieldValues.transformToPaymentSelection(
            context,
            cardPaymentMethod,
            metadata
        ) as PaymentSelection.New.Card

        assertThat(cardPaymentSelection.brand.code).isEqualTo(cardBrand)
        assertThat(cardPaymentSelection.customerRequestedSave).isEqualTo(customerRequestedSave)
        assertThat(getNameFromParams(cardPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
    }

    @Test
    fun `transformToPaymentSelection transforms external payment methods correctly`() {
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val paypalSpec = PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC
        val name = "Joe"
        val addressLine1 = "123 Main Street"
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Name to FormFieldEntry(name, true),
                IdentifierSpec.Line1 to FormFieldEntry(addressLine1, true)
            ),
            showsMandate = false,
            userRequestedReuse = customerRequestedSave,
        )
        val externalPaymentMethod = metadata.supportedPaymentMethodForCode(paypalSpec.type)!!

        val externalPaymentSelection = formFieldValues.transformToPaymentSelection(
            context,
            externalPaymentMethod,
            metadata
        ) as PaymentSelection.ExternalPaymentMethod

        assertThat(externalPaymentSelection.type)
            .isEqualTo(PaymentMethodFixtures.PAYPAL_EXTERNAL_PAYMENT_METHOD_SPEC.type)
        assertThat(externalPaymentSelection.label).isEqualTo(paypalSpec.label)
        assertThat(externalPaymentSelection.lightThemeIconUrl).isEqualTo(paypalSpec.lightImageUrl)
        assertThat(externalPaymentSelection.darkThemeIconUrl).isEqualTo(paypalSpec.darkImageUrl)
        assertThat(externalPaymentSelection.iconResource).isEqualTo(0)
        assertThat(externalPaymentSelection.billingDetails?.name).isEqualTo(name)
        assertThat(externalPaymentSelection.billingDetails?.address?.line1).isEqualTo(addressLine1)
    }

    @Test
    fun `transformToPaymentSelection transforms generic payment methods correctly`() {
        val customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        val name = "Joe"
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.Name to FormFieldEntry(name, true),
            ),
            showsMandate = false,
            userRequestedReuse = customerRequestedSave,
        )
        val klarnaPaymentMethod = metadata.supportedPaymentMethodForCode("klarna")!!

        val klarnaPaymentSelection = formFieldValues.transformToPaymentSelection(
            context,
            klarnaPaymentMethod,
            metadata
        ) as PaymentSelection.New.GenericPaymentMethod

        assertThat(klarnaPaymentSelection.paymentMethodCreateParams.typeCode).isEqualTo(klarnaPaymentMethod.code)
        assertThat(klarnaPaymentSelection.labelResource).isEqualTo(klarnaPaymentMethod.displayName.resolve(context))
        assertThat(klarnaPaymentSelection.lightThemeIconUrl).isEqualTo(klarnaPaymentMethod.lightThemeIconUrl)
        assertThat(klarnaPaymentSelection.darkThemeIconUrl).isEqualTo(klarnaPaymentMethod.darkThemeIconUrl)
        assertThat(klarnaPaymentSelection.iconResource).isEqualTo(klarnaPaymentMethod.iconResource)
        assertThat(getNameFromParams(klarnaPaymentSelection.paymentMethodCreateParams)).isEqualTo(name)
    }
}
