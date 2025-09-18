package com.stripe.android.paymentsheet.utils

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionFieldElement

internal suspend fun List<SectionFieldElement>.errorTest(
    identifierSpec: IdentifierSpec,
    error: FieldError?,
) {
    val element = find { it.identifier == identifierSpec }

    assertThat(element).isNotNull()

    requireNotNull(element).sectionFieldErrorController().error.test {
        error?.let {
            val storedError = awaitItem()

            assertThat(storedError?.errorMessage).isEqualTo(error.errorMessage)
            assertThat(storedError?.formatArgs).isEqualTo(error.formatArgs)
        } ?: run {
            assertThat(awaitItem()).isNull()
        }
    }
}
