package com.stripe.android.view

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.FpxBankStatuses
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FpxViewModelTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(application, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Test
    fun loadFpxBankStatues_workingOnMainThread_shouldUpdateLiveData() {
        var bankStatuses: FpxBankStatuses? = null
        val viewModel = FpxViewModel(application, MainScope())
        viewModel.loadFpxBankStatues()
        viewModel.fpxBankStatuses.observeForever {
            bankStatuses = it
        }

        assertTrue(requireNotNull(bankStatuses).isOnline(FpxBank.Hsbc))
    }
}
