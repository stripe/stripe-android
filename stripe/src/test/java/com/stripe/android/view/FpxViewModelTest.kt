package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.model.FpxBankStatuses
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FpxViewModelTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val viewModel = FpxViewModel(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        FakeStripeRepository()
    )

    @Test
    internal fun `getFpxBankStatues should update LiveData`() = testDispatcher.runBlockingTest {
        var bankStatuses: FpxBankStatuses? = null
        viewModel.getFpxBankStatues().observeForever {
            bankStatuses = it
        }

        assertThat(bankStatuses?.isOnline(FpxBank.AffinBank))
            .isTrue()
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun getFpxBankStatus(options: ApiRequest.Options) = FpxBankStatuses()
    }
}
