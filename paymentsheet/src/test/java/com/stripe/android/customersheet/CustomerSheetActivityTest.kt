package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = CustomerSheetContract()
    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(
            resources = context.resources,
            isFinancialConnectionsAvailable = { true },
            enableACHV2InDeferredFlow = true
        )
    )
    val intent = contract.createIntent(
        context = context,
        input = CustomerSheetContract.Args
    )

    private lateinit var viewModel: CustomerSheetViewModel

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        viewModel = createViewModel()
    }

    @Test
    fun `Finish with cancel on back press`() {
        val scenario = activityScenario().launchForResult(intent)

        scenario.onActivity {
            pressBack()
        }

        assertThat(
            contract.parseResult(
                scenario.getResult().resultCode,
                scenario.getResult().resultData
            )
        ).isEqualTo(
            InternalCustomerSheetResult.Canceled
        )
    }

    private fun activityScenario(
        viewModel: CustomerSheetViewModel = this.viewModel,
    ): InjectableActivityScenario<CustomerSheetActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun createViewModel(
        customerEphemeralKeyProvider: CustomerEphemeralKeyProvider =
            CustomerEphemeralKeyProvider {
                Result.success(
                    CustomerEphemeralKey(
                        customerId = "cus_123",
                        ephemeralKey = "ek_123",
                    )
                )
            },
        setupIntentClientSecretProvider: SetupIntentClientSecretProvider =
            SetupIntentClientSecretProvider {
                Result.success("seti_123")
            },
        customerAdapter: CustomerAdapter = CustomerAdapter.create(
            context = context,
            customerEphemeralKeyProvider = customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = setupIntentClientSecretProvider,
        ),
        lpmRepository: LpmRepository = this.lpmRepository
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository
        )
    }
}
