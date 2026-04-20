package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeSavedPaymentMethodConfirmInteractor(
    formEnabled: Boolean = false,
) : SavedPaymentMethodConfirmInteractor {
    private val _state = MutableStateFlow(
        SavedPaymentMethodConfirmInteractor.State(
            displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
                displayName = "···· 4242".resolvableString,
                paymentMethod = PaymentMethod(
                    id = "pm_123",
                    created = null,
                    liveMode = false,
                    code = PaymentMethod.Type.Card.code,
                    type = PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(
                        brand = CardBrand.Visa,
                        last4 = "4242",
                    )
                ),
            ),
            form = SavedPaymentMethodConfirmInteractor.State.Form(
                elements = listOf(
                    SectionElement.wrap(
                        sectionFieldElements = listOf(
                            SimpleTextElement(
                                identifier = IdentifierSpec.Generic("Name"),
                                controller = SimpleTextFieldController(
                                    initialValue = "John Doe",
                                    textFieldConfig = SimpleTextFieldConfig(
                                        label = "Name".resolvableString
                                    )
                                )
                            )
                        )
                    )
                ),
                enabled = formEnabled,
            ),
        )
    )
    override val state: StateFlow<SavedPaymentMethodConfirmInteractor.State> = _state.asStateFlow()

    class Factory : SavedPaymentMethodConfirmInteractor.Factory {
        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit
        ): SavedPaymentMethodConfirmInteractor {
            return FakeSavedPaymentMethodConfirmInteractor()
        }
    }
}
