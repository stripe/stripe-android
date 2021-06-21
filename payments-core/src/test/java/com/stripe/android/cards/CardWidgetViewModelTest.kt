package com.stripe.android.cards

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CardWidgetViewModelTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val viewModel = CardWidgetViewModel(
        application,
        object : CardAccountRangeRepository.Factory {
            override fun create() = FakeCardAccountRangeRepository()
        }
    )

    @Test
    fun `getAccountRange() should return expected value`() {
        var accountRange: AccountRange? = null
        viewModel.getAccountRange(CardNumberFixtures.VISA).observeForever {
            accountRange = it
        }
        assertThat(accountRange)
            .isEqualTo(ACCOUNT_RANGE)
    }

    private class FakeCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ) = ACCOUNT_RANGE

        override val loading: Flow<Boolean> = flowOf(false)
    }

    private companion object {
        private val ACCOUNT_RANGE = AccountRange(
            binRange = BinRange(
                low = "4242420000000000",
                high = "4242424239999999"
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            country = "GB"
        )
    }
}
