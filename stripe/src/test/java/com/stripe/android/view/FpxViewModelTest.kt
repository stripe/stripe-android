package com.stripe.android.view

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.MainScope
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FpxViewModelTest {
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PaymentConfiguration.init(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Test
    fun loadFpxBankStatues_workingOnMainThread_shouldUpdateLiveData() {
        val viewModel = FpxViewModel(context, MainScope())
        viewModel.loadFpxBankStatues()
        val fpxBankStatuses = requireNotNull(viewModel.fpxBankStatuses.value)
        assertTrue(fpxBankStatuses.isOnline(FpxBank.Hsbc.id))
    }
}
