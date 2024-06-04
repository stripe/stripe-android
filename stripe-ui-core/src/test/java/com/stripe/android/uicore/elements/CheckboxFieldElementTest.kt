package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckboxFieldElementTest {
    @Test
    fun `when controller checked state is false, form value should indicate incomplete`() = runTest {
        val controller = CheckboxFieldController()
        val element = CheckboxFieldElement(
            identifier = IdentifierSpec.Generic("test_checkbox"),
            controller = controller
        )

        element.getFormFieldValueFlow().test {
            val formFieldValue = awaitItem()

            Truth.assertThat(formFieldValue[0].second.isComplete).isFalse()
        }
    }

    @Test
    fun `when controller checked state is true, form value should indicate complete`() = runTest {
        val controller = CheckboxFieldController(initialValue = true)
        val element = CheckboxFieldElement(
            identifier = IdentifierSpec.Generic("test_checkbox"),
            controller = controller
        )

        element.getFormFieldValueFlow().test {
            val formFieldValue = awaitItem()

            Truth.assertThat(formFieldValue[0].second.isComplete).isTrue()
        }
    }
}
