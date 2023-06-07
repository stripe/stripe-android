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
import kotlinx.coroutines.flow.MutableSharedFlow
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
    fun `Finish with cancel on back press`() = runTest {
        runActivityScenario { scenario ->
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
    }

    @Test
    fun `Finish with cancel on navigate up action`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(),
            viewEffect = CustomerSheetViewEffect.NavigateUp,
        ) { scenario ->
            scenario.onActivity {
                assertThat(
                    contract.parseResult(
                        scenario.getResult().resultCode,
                        scenario.getResult().resultData
                    )
                ).isEqualTo(
                    InternalCustomerSheetResult.Canceled
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
        viewEffect: CustomerSheetViewEffect?,
    ): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = mock<CustomerSheetViewModel>()

        whenever(viewModel.viewState).thenReturn(
            MutableStateFlow(viewState)
        )

        if (viewEffect != null) {
            whenever(viewModel.viewEffect).thenReturn(
                MutableStateFlow(viewEffect)
            )
        } else {
            whenever(viewModel.viewEffect).thenReturn(
                MutableSharedFlow()
            )
        }

        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun runActivityScenario(
        viewState: CustomerSheetViewState = CustomerSheetViewState.Loading,
        viewEffect: CustomerSheetViewEffect? = null,
        block: (InjectableActivityScenario<CustomerSheetActivity>) -> Unit
    ) {
        val scenario = activityScenario(
            viewState = viewState,
            viewEffect = viewEffect,
        ).launchForResult(intent)
        scenario.use(block)
    }

    private fun createSelectPaymentMethodViewState(
        title: String? = null,
        paymentMethods: List<PaymentOptionsItem.SavedPaymentMethod> = listOf(),
        selectedPaymentMethodId: String? = null,
        isLiveMode: Boolean = false,
        isProcessing: Boolean = false,
        isEditing: Boolean = false,
    ): CustomerSheetViewState.SelectPaymentMethod {
        return CustomerSheetViewState.SelectPaymentMethod(
            title = title,
            paymentMethods = paymentMethods,
            selectedPaymentMethodId = selectedPaymentMethodId,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
        )
    }
}
