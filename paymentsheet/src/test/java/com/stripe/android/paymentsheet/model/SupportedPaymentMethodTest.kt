package com.stripe.android.paymentsheet.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.model.PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CARD_SFU_SET
import com.stripe.android.paymentsheet.PaymentSheetFixtures.CONFIG_CUSTOMER
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupportedPaymentMethodTest {
    @Test
    fun `If the intent has SFU set on top level or on LPM`() {
        assertThat(
            LpmRepositoryTestHelpers.card
                .getSpecWithFullfilledRequirements(
                    PI_REQUIRES_PAYMENT_METHOD_CARD_SFU_SET,
                    CONFIG_CUSTOMER
                )?.showCheckbox
        ).isFalse()
    }
}
