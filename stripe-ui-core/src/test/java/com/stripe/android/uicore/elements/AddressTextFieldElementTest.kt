package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddressTextFieldElementTest {
    @Test
    fun `Element should have a text field identifier`() = runTest {
        val element = AddressTextFieldElement(
            identifier = FormFieldId.OneLineAddress,
            label = "Address".resolvableString,
            inlineAutocompleteHandler = null,
            reportsFormValue = false,
            initialQuery = "",
            showEnterManually = true,
        )

        element.getTextFieldIdentifiers().test {
            assertThat(awaitItem()).containsExactly(
                FormFieldId.OneLineAddress
            )
        }
    }
}
