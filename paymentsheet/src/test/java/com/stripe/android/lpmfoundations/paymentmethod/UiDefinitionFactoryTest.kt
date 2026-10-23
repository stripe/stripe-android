package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.InstantDebitsDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.LinkCardBrandDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.UsBankAccountDefinition
import com.stripe.android.paymentsheet.FormHelper
import org.junit.Test

internal class UiDefinitionFactoryTest {
    @Test
    fun `card requires interaction without constructing form elements`() {
        assertRequiresInteractionWithoutFormElements(CardDefinition)
    }

    @Test
    fun `US bank account requires interaction without constructing form elements`() {
        assertRequiresInteractionWithoutFormElements(UsBankAccountDefinition)
    }

    @Test
    fun `instant debits requires interaction without constructing form elements`() {
        assertRequiresInteractionWithoutFormElements(InstantDebitsDefinition)
    }

    @Test
    fun `Link card brand requires interaction without constructing form elements`() {
        assertRequiresInteractionWithoutFormElements(LinkCardBrandDefinition)
    }

    private fun assertRequiresInteractionWithoutFormElements(definition: PaymentMethodDefinition) {
        val factory = definition.uiDefinitionFactory(PaymentMethodMetadataFactory.create())

        assertThat(factory).isInstanceOf(UiDefinitionFactory.Custom::class.java)
        assertThat(factory.formType { error("Form elements should not be constructed") })
            .isEqualTo(FormHelper.FormType.UserInteractionRequired)
    }
}
