package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement

internal suspend fun SectionFieldElement.errorTest(fieldError: FieldError?) {
    sectionFieldErrorController().error.test {
        fieldError?.let {
            val error = awaitItem()

            assertThat(error?.errorMessage).isEqualTo(it.errorMessage)
            assertThat(error?.formatArgs).isEqualTo(it.formatArgs)
        } ?: run {
            assertThat(awaitItem()).isNull()
        }
    }
}

internal fun List<SectionFieldElement>.element(identifierSpec: IdentifierSpec): SectionFieldElement {
    val element = nullableElement(identifierSpec)

    assertThat(element).isNotNull()

    return requireNotNull(element)
}

private fun List<SectionFieldElement>.nullableElement(identifierSpec: IdentifierSpec): SectionFieldElement? {
    for (element in this) {
        if (element is RowElement) {
            element.fields.nullableElement(identifierSpec)?.let {
                return it
            }
        } else if (element.identifier == identifierSpec) {
            return element
        }
    }

    return null
}
