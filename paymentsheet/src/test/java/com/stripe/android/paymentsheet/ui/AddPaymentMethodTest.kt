package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.BlikDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams.Companion.getNameFromParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.uicore.forms.FormFieldEntry
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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
        // TODO: update to be epms
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
}
