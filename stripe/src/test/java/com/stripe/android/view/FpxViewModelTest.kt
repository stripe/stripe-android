package com.stripe.android.view

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import kotlin.test.BeforeTest
import kotlin.test.assertTrue
import kotlinx.coroutines.MainScope
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FpxViewModelTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(application, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Ignore("FPX bank statuses endpoint is down")
    fun loadFpxBankStatues_workingOnMainThread_shouldUpdateLiveData() {
        val viewModel = FpxViewModel(application, MainScope())
        viewModel.loadFpxBankStatues()
        val fpxBankStatuses = requireNotNull(viewModel.fpxBankStatuses.value)
        assertTrue(fpxBankStatuses.isOnline(FpxBank.Hsbc.id))
    }
}
