package com.stripe.android.customersheet

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
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
        runActivityScenario {
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

    @Test
    fun `Finish with cancel on canceled result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(),
            result = InternalCustomerSheetResult.Canceled,
        ) {
            composeTestRule.waitForIdle()
            activity.finish()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isEqualTo(
                InternalCustomerSheetResult.Canceled
            )
        }
    }

    @Test
    fun `Finish with selected on selected result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(),
            result = InternalCustomerSheetResult.Selected(
                paymentMethodId = "pm_123",
                drawableResourceId = 123,
                label = "test",
            ),
        ) {
            composeTestRule.waitForIdle()
            activity.finish()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isInstanceOf(
                InternalCustomerSheetResult.Selected::class.java
            )
        }
    }

    @Test
    fun `Finish with error on error result`() = runTest {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(),
            result = InternalCustomerSheetResult.Error(
                exception = Exception("test")
            ),
        ) {
            composeTestRule.waitForIdle()
            activity.finish()
            assertThat(
                InternalCustomerSheetResult.fromIntent(scenario.getResult().resultData)
            ).isInstanceOf(
                InternalCustomerSheetResult.Error::class.java
            )
        }
    }

    @Test
    fun `Verify bottom sheet expands on start with default title`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                title = null
            ),
        ) {
            page.waitForText("Select your payment method")
        }
    }

    @Test
    fun `When savedPaymentMethods is not empty, payment method is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                savedPaymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                )
            ),
        ) {
            page.waitForText("····4242")
        }
    }

    @Test
    fun `When isGooglePayEnabled is true, google pay is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                isGooglePayEnabled = true
            ),
        ) {
            page.waitForText("google pay")
        }
    }

    @Test
    fun `When primaryButtonLabel is not null, primary button is visible`() {
        runActivityScenario(
            viewState = createSelectPaymentMethodViewState(
                primaryButtonLabel = "Testing Primary Button",
                primaryButtonEnabled = true,
            ),
        ) {
            page.waitForText("Testing Primary Button")
        }
    }

    @Test
    fun `When adding a payment method, title and primary button label is for adding payment method`() {
        runActivityScenario(
            viewState = createAddPaymentMethodViewState(),
        ) {
            page.waitForText("Add your payment information")
            page.waitForTextExactly("Add")
        }
    }

    private fun activityScenario(
        viewState: CustomerSheetViewState,
        result: InternalCustomerSheetResult?,
        providePaymentMethodName: (PaymentMethodCode) -> String,
    ): InjectableActivityScenario<CustomerSheetActivity> {
        val viewModel = mock<CustomerSheetViewModel>()

        whenever(viewModel.viewState).thenReturn(
            MutableStateFlow(viewState)
        )
        whenever(viewModel.result).thenReturn(
            MutableStateFlow(result)
        )
        whenever(viewModel.providePaymentMethodName(any())).then {
            val code = it.arguments.first() as PaymentMethodCode
            providePaymentMethodName(code)
        }

        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private fun runActivityScenario(
        viewState: CustomerSheetViewState = CustomerSheetViewState.Loading(
            isLiveMode = false,
        ),
        result: InternalCustomerSheetResult? = null,
        providePaymentMethodName: (PaymentMethodCode) -> String = { it },
        testBlock: CustomerSheetTestData.() -> Unit,
    ) {
        activityScenario(
            viewState = viewState,
            result = result,
            providePaymentMethodName = providePaymentMethodName,
        )
            .launchForResult(intent)
            .use { injectableActivityScenario ->
                injectableActivityScenario.onActivity { activity ->
                    with(
                        CustomerSheetTestDataImpl(
                            scenario = injectableActivityScenario,
                            activity = activity
                        )
                    ) {
                        testBlock()
                    }
                }
            }
    }

    private fun createSelectPaymentMethodViewState(
        title: String? = null,
        savedPaymentMethods: List<PaymentMethod> = listOf(),
        paymentSelection: PaymentSelection? = null,
        isLiveMode: Boolean = false,
        isProcessing: Boolean = false,
        isEditing: Boolean = false,
        isGooglePayEnabled: Boolean = false,
        primaryButtonLabel: String? = null,
        primaryButtonEnabled: Boolean = false,
    ): CustomerSheetViewState.SelectPaymentMethod {
        return CustomerSheetViewState.SelectPaymentMethod(
            title = title,
            savedPaymentMethods = savedPaymentMethods,
            paymentSelection = paymentSelection,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            isGooglePayEnabled = isGooglePayEnabled,
            primaryButtonLabel = primaryButtonLabel,
            primaryButtonEnabled = primaryButtonEnabled,
        )
    }

    private fun createAddPaymentMethodViewState(
        isLiveMode: Boolean = false,
        formViewData: FormViewModel.ViewData = FormViewModel.ViewData(),
        enabled: Boolean = true,
    ): CustomerSheetViewState.AddPaymentMethod {
        return CustomerSheetViewState.AddPaymentMethod(
            formViewDataFlow = flowOf(formViewData),
            enabled = enabled,
            isLiveMode = isLiveMode,
        )
    }

    private class CustomerSheetTestDataImpl(
        override val scenario: InjectableActivityScenario<CustomerSheetActivity>,
        override val activity: CustomerSheetActivity
    ) : CustomerSheetTestData

    interface CustomerSheetTestData {
        val scenario: InjectableActivityScenario<CustomerSheetActivity>
        val activity: CustomerSheetActivity
    }
}
