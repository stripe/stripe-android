package com.stripe.android.paymentsheet.utils

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldElement

internal suspend fun List<SectionFieldElement>.errorTest(
    identifierSpec: IdentifierSpec,
    error: FieldValidationMessage?,
) {
    val element = find { it.identifier == identifierSpec }

    assertThat(element).isNotNull()

    requireNotNull(element).sectionFieldErrorController().validationMessage.test {
        error?.let {
            val storedError = awaitItem()

            assertThat(storedError?.message).isEqualTo(error.message)
            assertThat(storedError?.formatArgs).isEqualTo(error.formatArgs)
        } ?: run {
            assertThat(awaitItem()).isNull()
        }
    }
}
