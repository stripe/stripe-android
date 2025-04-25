package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

internal class DefaultVerticalModeFormInteractorSetAsDefaultTest {

    @Test
    fun `SetAsDefault shown when SaveForFutureUseChecked when hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.shouldShowElementFlow.value).isTrue()
        }
    }

    @Test
    fun `SetAsDefault field default false when SaveForFutureUseChecked when hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = true
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller
            val setAsDefaultController = setAsDefaultPaymentMethodElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultController.fieldValue.value.toBoolean()).isFalse()
        }
    }

    @Test
    fun `SetAsDefault hidden when SaveForFutureUseChecked when not hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = false
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `SetAsDefault field true when SaveForFutureUseChecked when not hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = false
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.controller.fieldValue.value.toBoolean()).isTrue()
        }
    }

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    private fun testSetAsDefaultElements(
        hasSavedPaymentMethods: Boolean,
        block: (SaveForFutureUseElement?, SetAsDefaultPaymentMethodElement?) -> Unit
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            hasCustomerConfiguration = true,
            isPaymentMethodSetAsDefaultEnabled = true,
        )
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val stateHolder = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter()
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
        )
        val eventReporter = FakeEventReporter()
        val setAsDefaultInteractor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethodCode = "card",
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            formActivityStateHelper = stateHolder,
            eventReporter = eventReporter
        ).create()

        val formElements = setAsDefaultInteractor.state.value.formElements

        val saveForFutureUseElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SaveForFutureUse
        } as? SaveForFutureUseElement
        val setAsDefaultElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SetAsDefaultPaymentMethod
        } as? SetAsDefaultPaymentMethodElement

        block(saveForFutureUseElement, setAsDefaultElement)
    }
}
