package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DisplayableSavedPaymentMethodTest {

    private val context: Context = getApplicationContext()

    @Test
    fun getDescription_usesDisplayedCardBrand() {
        val visaCardUsingCartesBancaires = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = visaCardUsingCartesBancaires
        )

        val description = displayableSavedPaymentMethod.getDescription().resolve(context)

        assertThat(description).isEqualTo("Cartes Bancaires ending in 4242")
    }

    @Test
    fun getDescription_usesBrandIfDisplayBrandIsUnknown() {
        val cardWithoutDisplayBrand = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(displayBrand = null)
        )
        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = cardWithoutDisplayBrand
        )

        val description = displayableSavedPaymentMethod.getDescription().resolve(context)

        assertThat(description).isEqualTo("Visa ending in 4242")
    }

    @Test
    fun create_forCard_createsCard() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = false,
        )

        assertThat(displayableSavedPaymentMethod.savedPaymentMethod).isInstanceOf<SavedPaymentMethod.Card>()
    }

    @Test
    fun create_forUSBankAccount_createsUSBankAccount() {
        val paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT

        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = false,
        )

        assertThat(displayableSavedPaymentMethod.savedPaymentMethod).isInstanceOf<SavedPaymentMethod.USBankAccount>()
    }

    @Test
    fun create_forSepaDebit_createsSepaDebit() {
        val paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD

        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = false,
        )

        assertThat(displayableSavedPaymentMethod.savedPaymentMethod).isInstanceOf<SavedPaymentMethod.SepaDebit>()
    }

    @Test
    fun create_missingPaymentMethodInfo_createsUnexpected() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = null
        )

        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = false,
        )

        assertThat(displayableSavedPaymentMethod.savedPaymentMethod).isInstanceOf<SavedPaymentMethod.Unexpected>()
    }

    @Test
    fun create_unexpectedType_createsUnexpected() {
        val paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD

        val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "unused".resolvableString,
            paymentMethod = paymentMethod,
            isCbcEligible = false,
        )

        assertThat(displayableSavedPaymentMethod.savedPaymentMethod).isInstanceOf<SavedPaymentMethod.Unexpected>()
    }

    data class IsModifiableParams(
        val canUpdatePaymentMethod: Boolean = false,
        val isCbcEligible: Boolean = false,
        val availableNetworks: Set<String> = setOf("visa"),
        val cardExpired: Boolean = false,
        val expectedResult: Boolean,
    )
}
