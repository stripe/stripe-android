package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddressTextFieldElementTest {
    @Test
    fun `Element should have a text field identifier`() = runTest {
        val element = AddressTextFieldElement(
            identifier = IdentifierSpec.OneLineAddress,
            label = "Address".resolvableString,
            onNavigation = null,
        )

        element.getTextFieldIdentifiers().test {
            assertThat(awaitItem()).containsExactly(
                IdentifierSpec.OneLineAddress
            )
        }
    }

    @Test
    fun `inlinePredictionsState is passed through to controller`() {
        val state = stateFlowOf(AutocompleteAddressInteractor.InlinePredictionsState.Idle)
        val element = AddressTextFieldElement(
            identifier = IdentifierSpec.OneLineAddress,
            label = "Address".resolvableString,
            inlinePredictionsState = state,
        )
        assertThat(element.controller.inlinePredictionsState).isEqualTo(state)
    }

    @Test
    fun `inlinePredictionsState defaults to null in controller`() {
        val element = AddressTextFieldElement(
            identifier = IdentifierSpec.OneLineAddress,
            label = "Address".resolvableString,
        )
        assertThat(element.controller.inlinePredictionsState).isNull()
    }
}
