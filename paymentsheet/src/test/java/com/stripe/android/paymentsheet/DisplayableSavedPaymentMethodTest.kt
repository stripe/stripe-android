package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DisplayableSavedPaymentMethodTest {

    val context: Context = getApplicationContext()

    @Test
    fun getDescription_usesDisplayedCardBrand() {
        val visaCardUsingCartesBancaires = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod(
            displayName = "unused".resolvableString,
            paymentMethod = visaCardUsingCartesBancaires
        )

        val description = displayableSavedPaymentMethod.getDescription().resolve(context)

        assertThat(description).isEqualTo("Cartes Bancaires ending in 4242")
    }
}
