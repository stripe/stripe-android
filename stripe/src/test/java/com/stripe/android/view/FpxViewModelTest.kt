package com.stripe.android.view

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
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

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    internal fun `getFpxBankStatues should update LiveData`() = testDispatcher.runBlockingTest {
        var bankStatuses: FpxBankStatuses? = null
        viewModel.getFpxBankStatues().observeForever {
            bankStatuses = it
        }

        assertThat(FpxBank.get("affin_bank")?.let { bankStatuses?.isOnline(it) })
            .isTrue()
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun getFpxBankStatus(options: ApiRequest.Options) = FpxBankStatuses()
    }
}
