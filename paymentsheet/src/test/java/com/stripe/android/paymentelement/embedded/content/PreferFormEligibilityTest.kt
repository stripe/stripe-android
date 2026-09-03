package com.stripe.android.paymentelement.embedded.content

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.FormHelper.FormType
import kotlin.test.Test

internal class PreferFormEligibilityTest {
    @Test
    fun `user interaction form is eligible`() {
        assertThat(eligibility(selectedFormType = FormType.UserInteractionRequired)).isTrue()
    }

    @Test
    fun `empty preferred form is ineligible`() {
        assertThat(eligibility(selectedFormType = FormType.Empty)).isFalse()
    }

    @Test
    fun `mandate-only preferred form is ineligible`() {
        assertThat(eligibility(selectedFormType = FormType.MandateOnly("Mandate".resolvableString)))
            .isFalse()
    }

    private fun eligibility(
        selectedFormType: FormType = FormType.UserInteractionRequired,
    ): Boolean {
        return isPreferFormEligible(
            selectedFormType = selectedFormType,
        )
    }
}
