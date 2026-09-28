package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement

internal suspend fun SectionFieldElement.errorTest(fieldValidationMessage: FieldValidationMessage?) {
    sectionFieldErrorController().validationMessage.test {
        fieldValidationMessage?.let {
            val error = awaitItem()

            assertThat(error?.message).isEqualTo(it.message)
            assertThat(error?.formatArgs).isEqualTo(it.formatArgs)
        } ?: run {
            assertThat(awaitItem()).isNull()
        }
    }
}

internal fun List<SectionFieldElement>.element(formFieldId: FormFieldId): SectionFieldElement {
    val element = nullableElement(formFieldId)

    assertThat(element).isNotNull()

    return requireNotNull(element)
}

private fun List<SectionFieldElement>.nullableElement(formFieldId: FormFieldId): SectionFieldElement? {
    for (element in this) {
        if (element is RowElement) {
            element.fields.nullableElement(formFieldId)?.let {
                return it
            }
        } else if (element.identifier == formFieldId) {
            return element
        }
    }

    return null
}
