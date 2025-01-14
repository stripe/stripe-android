package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.VerticalModeForm
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

internal class VerticalModeFormUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun cardFormIsDisplayed() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata)
        }
    }

    @Test
    fun cardFieldsAreDisabledWhenProcessing() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create()
            CreateTestScenario(paymentMethodCode = "card", metadata = metadata, isProcessing = true)
        }
    }

    @Test
    fun fullCardForm() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)
        viewModel.onError("An error occurred.".resolvableString)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithLink() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
                canGoBack = false,
            ),
            showsWalletHeader = true,
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)
        viewModel.walletsStateSource.value = WalletsState(
            link = WalletsState.Link(
                email = null,
            ),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_with_card,
            onGooglePayPressed = { throw AssertionError("Not expected.") },
            onLinkPressed = { throw AssertionError("Not expected.") },
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCardFormWithSaveForLater() {
        val metadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
        )
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "card",
                metadata = metadata,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun fullCashAppForm() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            ),
        )
        val initialScreen = VerticalModeForm(
            FakeVerticalModeFormInteractor.create(
                paymentMethodCode = "cashapp",
                metadata = metadata,
                canGoBack = false,
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun cashappShowsBillingFields() {
        paparazziRule.snapshot {
            val metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                )
            )
            CreateTestScenario(paymentMethodCode = "cashapp", metadata = metadata)
        }
    }

    @Composable
    private fun CreateTestScenario(
        paymentMethodCode: PaymentMethodCode,
        metadata: PaymentMethodMetadata,
        isProcessing: Boolean = false,
        showsWalletHeader: Boolean = false,
    ) {
        ViewModelStoreOwnerContext {
            VerticalModeFormUI(
                interactor = FakeVerticalModeFormInteractor.create(
                    paymentMethodCode = paymentMethodCode,
                    metadata = metadata,
                    isProcessing = isProcessing,
                ),
                showsWalletHeader = showsWalletHeader,
            )
        }
    }
}
