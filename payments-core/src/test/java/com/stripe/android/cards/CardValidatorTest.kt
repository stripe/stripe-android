package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import kotlin.test.assertFailsWith

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

    @Test
    fun `Visa card possible brands contains Visa`() = runTest {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(context)

        assertThat(validator.possibleBrands("424242424242"))
            .containsExactly(CardBrand.Visa)
    }

    @Test
    fun `Suspending possible brands exception`() = runTest {
        assertFailsWith(Exception::class) {
            PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            val validator = CardValidator(
                context,
                FakeCardAccountRangeRepository {
                    throw Exception("test")
                }
            )
            validator.possibleBrands("424242424242")
        }
    }

    @Test
    fun `Validator with success callback`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(context)

        var actualSet: Set<CardBrand> = setOf()
        var actualError: Throwable? = null
        val countDownLatch = CountDownLatch(1)
        validator.possibleBrands(
            cardNumber = "424242",
            onSuccess = {
                actualSet = it
                countDownLatch.countDown()
            },
            onFailure = {
                actualError = it
                countDownLatch.countDown()
            }
        )

        countDownLatch.await()

        assertThat(actualSet)
            .containsExactly(CardBrand.Visa)
        assertThat(actualError)
            .isNull()
    }

    @Test
    fun `Validator with failure callback`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val validator = CardValidator(
            context,
            FakeCardAccountRangeRepository {
                throw Exception("test")
            }
        )

        var actualSet: Set<CardBrand> = setOf()
        var actualError: Throwable? = null
        val countDownLatch = CountDownLatch(1)
        validator.possibleBrands(
            cardNumber = "424242",
            onSuccess = {
                actualSet = it
                countDownLatch.countDown()
            },
            onFailure = {
                actualError = it
                countDownLatch.countDown()
            }
        )

        countDownLatch.await()

        assertThat(actualSet)
            .isEmpty()
        assertThat(actualError)
            .isNotNull()
    }

    private class FakeCardAccountRangeRepository(
        val onGetAccountRanges: () -> Set<AccountRange>?
    ) : CardAccountRangeRepository {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? {
            return null
        }

        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): Set<AccountRange>? {
            return onGetAccountRanges()
        }

        override val loading: Flow<Boolean> = flowOf(false)
    }
}