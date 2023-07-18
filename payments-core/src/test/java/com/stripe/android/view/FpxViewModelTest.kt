package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.BankStatuses
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FpxViewModelTest {
    private val viewModel = FpxViewModel(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        FakeStripeRepository()
    )

    @Test
    internal fun `getFpxBankStatues should update LiveData`() = runTest {
        var bankStatuses: BankStatuses? = null
        viewModel.getFpxBankStatues().observeForever {
            bankStatuses = it
        }

        assertThat(FpxBank.get("affin_bank")?.let { bankStatuses?.isOnline(it) })
            .isTrue()
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun getFpxBankStatus(options: ApiRequest.Options): Result<BankStatuses> {
            return Result.success(BankStatuses())
        }
    }
}
