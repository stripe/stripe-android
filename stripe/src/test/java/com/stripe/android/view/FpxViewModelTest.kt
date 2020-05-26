package com.stripe.android.view

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.FpxBankStatuses
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class FpxViewModelTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(application, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Test
    fun loadFpxBankStatues_workingOnMainThread_shouldUpdateLiveData() {
        testScope.runBlockingTest {
            var bankStatuses: FpxBankStatuses? = null
            val viewModel = FpxViewModel(application, workContext = coroutineContext)
            viewModel.getFpxBankStatues().observeForever {
                bankStatuses = it
            }

            assertThat(requireNotNull(bankStatuses).isOnline(FpxBank.Hsbc))
                .isTrue()
        }
    }
}
