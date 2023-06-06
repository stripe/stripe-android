package com.stripe.android.customersheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = context.resources,
            isFinancialConnectionsAvailable = { true },
            enableACHV2InDeferredFlow = true
        )
    )

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `ViewModel initializes with loading`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            assertThat(awaitItem()).isEqualTo(CustomerSheetViewState.Loading)
        }
    }

    private fun createViewModel(
        customerAdapter: CustomerAdapter = CustomerAdapter.create(
            context = context,
            customerEphemeralKeyProvider = {
                Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123",
                    )
                )
            },
            setupIntentClientSecretProvider = {
                Result.success("seti_123")
            },
        ),
        lpmRepository: LpmRepository = this.lpmRepository
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            resources = context.resources,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Testing"
            )
        )
    }
}
