package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardValidatorTest {

    private val context = ApplicationProvider.getApplicationContext<Application>().applicationContext

    @Test
    fun `Cartes Bancaires bin possible brands contains CartesBancaires and MasterCard`() = runTest {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(context)

        assertThat(validator.possibleBrands("513130"))
            .containsExactly(CardBrand.CartesBancaires, CardBrand.MasterCard)
    }

    @Test
    fun `Cartes Bancaires bin possible brands contains CartesBancaires and Visa`() = runTest {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(context)

        assertThat(validator.possibleBrands("455673"))
            .containsExactly(CardBrand.CartesBancaires, CardBrand.Visa)
    }

    @Test
    fun `Visa bin possible brands contains Visa`() = runTest {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(context)

        assertThat(validator.possibleBrands("424242"))
            .containsExactly(CardBrand.Visa)
    }
}