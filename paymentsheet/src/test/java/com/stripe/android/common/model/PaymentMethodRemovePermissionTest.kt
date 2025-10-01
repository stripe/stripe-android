package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import kotlin.test.Test

class PaymentMethodRemovePermissionTest {
    @Test
    fun `on 'Full' permissions, remove message should be null`() {
        assertThat(PaymentMethodRemovePermission.Full.removeMessage("Merchant, Inc."))
            .isNull()
    }

    @Test
    fun `on 'None' permissions, remove message should be null`() {
        assertThat(PaymentMethodRemovePermission.None.removeMessage("Merchant, Inc."))
            .isNull()
    }

    @Test
    fun `on 'Partial' permissions, remove message should be expected message`() {
        assertThat(PaymentMethodRemovePermission.Partial.removeMessage("Merchant, Inc."))
            .isEqualTo(
                resolvableString(R.string.stripe_paymentsheet_remove_partial_description, "Merchant, Inc.")
            )
    }
}
