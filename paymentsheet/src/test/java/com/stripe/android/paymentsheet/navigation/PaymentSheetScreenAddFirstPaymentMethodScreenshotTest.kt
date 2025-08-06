package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Appearance
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor.Companion.createState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.OutlinedIconsAppearance
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance.DefaultAppearance
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenAddFirstPaymentMethodScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val outlinedPaparazziRule = PaparazziRule(
        listOf(OutlinedIconsAppearance),
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val customPrimaryButtonHeightPaparazziRule = PaparazziRule(
        listOf(CustomPrimaryButtonHeightAppearance),
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun displaysCard() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState())
        val initialScreen = AddFirstPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun displaysError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState())
        val initialScreen = AddFirstPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.onError("An error occurred.".resolvableString)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun displaysWithExpandedPrimaryButtonHeight() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState(metadata))
        val initialScreen = AddFirstPaymentMethod(interactor)

        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        viewModel.walletsStateSource.value = WalletsState(
            link = WalletsState.Link(
                state = LinkButtonState.Email("email@email.com"),
            ),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_or_pay_with_card,
            onLinkPressed = {},
            onGooglePayPressed = {},
        )

        customPrimaryButtonHeightPaparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun displaysOutlined() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "crypto", "us_bank_account"),
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}""",
            ),
            allowsDelayedPaymentMethods = true,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Lite,
        )
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState(metadata))
        val initialScreen = AddFirstPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.onError("An error occurred.".resolvableString)

        outlinedPaparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    private data object CustomPrimaryButtonHeightAppearance : PaparazziConfigOption {
        val appearance = Appearance.Builder()
            .primaryButton(
                Appearance.PrimaryButton(
                    shape = Appearance.PrimaryButton.Shape(
                        heightDp = 80f,
                    )
                )
            )
            .build()

        override fun initialize() {
            appearance.parseAppearance()
        }

        override fun reset() {
            DefaultAppearance.appearance.parseAppearance()
        }
    }
}
