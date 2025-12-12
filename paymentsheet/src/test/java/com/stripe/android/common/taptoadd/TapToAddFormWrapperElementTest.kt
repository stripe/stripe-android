package com.stripe.android.common.taptoadd

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapToAddFormWrapperElementTest {
    @Test
    fun `allowsUserInteraction is true`() {
        val element = TapToAddFormWrapperElement(
            elements = emptyList(),
            tapToAddHelper = FakeTapToAddHelper.noOp(),
        )

        assertThat(element.allowsUserInteraction).isTrue()
    }

    @Test
    fun `getFormFieldValueFlow combines all elements`() = runTest {
        val element = TapToAddFormWrapperElement(
            elements = listOf(
                SectionElement.wrap(
                    sectionFieldElements = listOf(
                        SimpleTextElement(
                            identifier = IdentifierSpec.Generic("card_number"),
                            controller = SimpleTextFieldController(
                                textFieldConfig = SimpleTextFieldConfig(label = "Card".resolvableString),
                            ),
                        ),
                        SimpleTextElement(
                            identifier = IdentifierSpec.Generic("phone"),
                            controller = SimpleTextFieldController(
                                textFieldConfig = SimpleTextFieldConfig(label = "Phone".resolvableString),
                            ),
                        ),
                    ),
                )
            ),
            tapToAddHelper = FakeTapToAddHelper.noOp(),
        )

        element.getFormFieldValueFlow().test {
            val formFieldValues = awaitItem()

            // Should contain form field values from both card and non-card elements
            assertThat(formFieldValues).hasSize(2)

            val identifiers = formFieldValues.map { it.first }
            assertThat(identifiers).contains(IdentifierSpec.Generic("card_number"))
            assertThat(identifiers).contains(IdentifierSpec.Generic("phone"))
        }
    }
}
