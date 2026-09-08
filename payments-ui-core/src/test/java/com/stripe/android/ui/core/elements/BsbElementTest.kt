package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BsbElementTest {
    @Test
    fun `default banks include server bank prefixes`() = runTest {
        val element = BsbElement(
            formFieldId = FormFieldId.Generic("au_becs_debit[bsb_number]"),
            initialValue = null,
        )

        element.controller.onValueChange("369000")

        element.bankName.test {
            assertThat(awaitItem()).isEqualTo("BNK Banking Corporation Ltd")
        }
    }

    @Test
    fun `most specific bank prefix determines bank name`() = runTest {
        val element = BsbElement(
            formFieldId = FormFieldId.Generic("au_becs_debit[bsb_number]"),
            initialValue = null,
        )

        element.controller.onValueChange("611000")

        element.bankName.test {
            assertThat(awaitItem()).isEqualTo("Select Credit Union")
        }
    }

    @Test
    fun `controller updates bank name and complete form entry`() = runTest {
        val identifier = FormFieldId.Generic("au_becs_debit[bsb_number]")
        val element = BsbElement(
            formFieldId = identifier,
            initialValue = null,
        )

        element.controller.onValueChange("000000")

        element.bankName.test {
            assertThat(awaitItem()).isEqualTo("Stripe Test Bank")
        }
        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactly(
                identifier to FormFieldEntry("000000", true),
            )
        }
    }
}
