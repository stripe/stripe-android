package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class CustomerSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = CustomerSheetContract()
    private val intent = contract.createIntent(
        context = context,
        input = CustomerSheetContract.Args
    )
    private val page = CustomerSheetPage(composeTestRule)

    @Test
    fun `Finish with cancel on back press`() {
        runActivityScenario { scenario ->
            scenario.onActivity {
                composeTestRule.waitForIdle()
                pressBack()
                composeTestRule.waitForIdle()
                assertThat(
                    InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
                ).isEqualTo(
                    InternalCustomerSheetResult.Canceled
                )
            }
        }
    }

    @Test
    fun `Finish with cancel on canceled result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                result = InternalCustomerSheetResult.Canceled
            ),
        ) { scenario ->
            scenario.onActivity {
                composeTestRule.waitForIdle()
                it.finish()
                assertThat(
                    InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
                ).isEqualTo(
                    InternalCustomerSheetResult.Canceled
                )
            }
        }
    }

    @Test
    fun `Finish with selected on selected result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                result = InternalCustomerSheetResult.Selected(
                    paymentMethodId = "pm_123",
                    drawableResourceId = 123,
                    label = "test",
                )
            ),
        ) { scenario ->
            scenario.onActivity {
                composeTestRule.waitForIdle()
                it.finish()
                assertThat(
                    InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
                ).isInstanceOf(
                    InternalCustomerSheetResult.Selected::class.java
                )
            }
        }
    }

    @Test
    fun `Finish with error on error result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                result = InternalCustomerSheetResult.Error(
                    exception = Exception("test")
                )
            ),
        ) { scenario ->
            scenario.onActivity {
                composeTestRule.waitForIdle()
                it.finish()
                assertThat(
                    InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
                ).isInstanceOf(
                    InternalCustomerSheetResult.Error::class.java
                )
            }
        }
    }

    @Test
    fun `Verify bottom sheet expands on start with default title`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                title = null
            ),
        ) { scenario ->
            scenario.onActivity {
                page.waitForText("Select your payment method")
            }
        }
    }

    private fun activityScenario(
        viewState: CustomerSheetViewState,
    ): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = mock<CustomerSheetViewModel>()

        whenever(viewModel.viewState).thenReturn(
            MutableStateFlow(viewState)
        )

        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun runActivityScenario(
        viewState: CustomerSheetViewState = CustomerSheetViewState.Loading,
        block: (InjectableActivityScenario<CustomerSheetActivity>) -> Unit
    ) {
        activityScenario(viewState = viewState)
            .launchForResult(intent)
            .use(block)
    }

    private fun createSelectPaymentMethodViewState(
        title: String? = null,
        paymentMethods: List<PaymentOptionsItem.SavedPaymentMethod> = listOf(),
        selectedPaymentMethodId: String? = null,
        isLiveMode: Boolean = false,
        isProcessing: Boolean = false,
        isEditing: Boolean = false,
        result: InternalCustomerSheetResult? = null
    ): CustomerSheetViewState.SelectPaymentMethod {
        return CustomerSheetViewState.SelectPaymentMethod(
            title = title,
            paymentMethods = paymentMethods,
            selectedPaymentMethodId = selectedPaymentMethodId,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            result = result,
        )
    }
}
