package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.ui.CardBrandChoice
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class InitialManageScreenFactoryTest {

    @Test
    fun `initialScreen is All when multiple paymentMethods`() = testScenario {
        customerStateHolder.setCustomerState(
            PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = PaymentMethodFixtures.createCards(2)
            )
        )
        assertThat(factory.createInitialScreen()).isInstanceOf<ManageNavigator.Screen.All>()
    }

    @Test
    fun `initialScreen is Update when one paymentMethod`() = testScenario {
        customerStateHolder.setCustomerState(
            PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = PaymentMethodFixtures.createCards(1)
            )
        )
        assertThat(factory.createInitialScreen()).isInstanceOf<ManageNavigator.Screen.Update>()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val customerStateHolder = CustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = stateFlowOf(null),
            customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
        )
        val factory = InitialManageScreenFactory(
            customerStateHolder = customerStateHolder,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            updateScreenInteractorFactory = { displayableSavedPaymentMethod ->
                FakeUpdatePaymentMethodInteractor(
                    displayableSavedPaymentMethod = displayableSavedPaymentMethod,
                    canRemove = customerStateHolder.canRemove.value,
                    isExpiredCard = false,
                    isModifiablePaymentMethod = true,
                    viewActionRecorder = ViewActionRecorder(),
                    initialState = UpdatePaymentMethodInteractor.State(
                        error = null,
                        status = UpdatePaymentMethodInteractor.Status.Idle,
                        cardBrandChoice = CardBrandChoice(brand = CardBrand.Visa, enabled = true),
                        setAsDefaultCheckboxChecked = false,
                        isSaveButtonEnabled = false,
                    ),
                    shouldShowSetAsDefaultCheckbox = false,
                    shouldShowSaveButton = false,
                )
            },
            manageInteractorFactory = {
                FakeManageScreenInteractor()
            }
        )
        Scenario(
            factory = factory,
            customerStateHolder = customerStateHolder,
        ).block()
    }

    private class Scenario(
        val factory: InitialManageScreenFactory,
        val customerStateHolder: CustomerStateHolder,
    )
}
