package com.stripe.android.uicore.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SectionControllerTest {

    @Test
    fun `validation message updates reactively when child controller changes`() = runTest {
        val controller1 = FakeSectionFieldValidationController(null)
        val controller2 = FakeSectionFieldValidationController(
            FieldValidationMessage.Warning(2)
        )

        val sectionController = sectionController(
            sectionFieldValidationControllers = listOf(controller1, controller2),
        )

        // Fake FieldValidationMessageComparator sorts by message ID ascending
        sectionController.validationMessage.test {
            assertThat(awaitItem()).isEqualTo(FieldValidationMessage.Warning(2))

            controller1.updateValidationMessage(FieldValidationMessage.Warning(1))

            assertThat(awaitItem()).isEqualTo(FieldValidationMessage.Warning(1))
        }
    }

    @Test
    fun `validation message remains unchanged when lower priority message changes`() = runTest {
        val controller1 = FakeSectionFieldValidationController(
            FieldValidationMessage.Warning(1)
        )
        val controller2 = FakeSectionFieldValidationController(null)

        val sectionController = sectionController(
            sectionFieldValidationControllers = listOf(controller1, controller2),
        )

        sectionController.validationMessage.test {
            assertThat(awaitItem()).isEqualTo(FieldValidationMessage.Warning(1))

            controller2.updateValidationMessage(FieldValidationMessage.Warning(2))

            expectNoEvents()
        }
    }

    @Test
    fun `returns null when no controllers provided`() = runTest {
        val sectionController = sectionController(
            sectionFieldValidationControllers = emptyList(),
        )

        sectionController.validationMessage.test {
            assertThat(awaitItem()).isNull()
        }
    }

    private fun sectionController(
        sectionFieldValidationControllers: List<SectionFieldValidationController>
    ): SectionController {
        return SectionController(
            label = resolvableString("Section"),
            sectionFieldValidationControllers = sectionFieldValidationControllers,
            validationMessageComparator = object : FieldValidationMessageComparator {
                override fun compare(
                    a: FieldValidationMessage?,
                    b: FieldValidationMessage?
                ): Int {
                    return when {
                        a == null && b == null -> 0
                        a == null -> 1
                        b == null -> -1
                        else -> a.message.compareTo(b.message)
                    }
                }
            }
        )
    }

    private class FakeSectionFieldValidationController(
        initialValidationMessage: FieldValidationMessage?
    ) : SectionFieldValidationController {
        private val _validationMessage = MutableStateFlow(initialValidationMessage)
        override val validationMessage = _validationMessage

        fun updateValidationMessage(message: FieldValidationMessage?) {
            _validationMessage.value = message
        }
    }
}
