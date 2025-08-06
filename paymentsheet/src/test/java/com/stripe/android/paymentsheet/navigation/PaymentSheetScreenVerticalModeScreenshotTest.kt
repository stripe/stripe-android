package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.AppearanceAPIAdditionsPreview
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.VerticalMode
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.OutlinedIconsAppearance
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenVerticalModeScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val outlinedPaparazziRule = PaparazziRule(
        listOf(OutlinedIconsAppearance),
        boxModifier = Modifier
            .padding(16.dp),
    )

    @get:Rule
    val smallVerticalModeRowPaddingAppearance = PaparazziRule(
        listOf(AdditionalPaddingAppearance(verticalModeRowPadding = 2f)),
        boxModifier = Modifier
            .padding(16.dp),
    )

    @get:Rule
    val largeVerticalModeRowPaddingAppearance = PaparazziRule(
        listOf(AdditionalPaddingAppearance(verticalModeRowPadding = 12f)),
        boxModifier = Modifier
            .padding(16.dp),
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun displaysVerticalModeList() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalMode(FakePaymentMethodVerticalLayoutInteractor.create(metadata))
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalMode(FakePaymentMethodVerticalLayoutInteractor.create(metadata))
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.onError("Example error".resolvableString)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithMandate() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        )
        val initialScreen = VerticalMode(
            FakePaymentMethodVerticalLayoutInteractor.create(
                paymentMethodMetadata = metadata,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.New("cashapp"),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.mandateHandler.updateMandateText("Example Mandate".resolvableString, showAbove = true)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithErrorAndMandate() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
        )
        val initialScreen = VerticalMode(
            FakePaymentMethodVerticalLayoutInteractor.create(
                paymentMethodMetadata = metadata,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.New("cashapp"),
            )
        )
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.onError("Example error".resolvableString)
        viewModel.mandateHandler.updateMandateText("Example Mandate".resolvableString, showAbove = true)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithLink() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = metadata,
            initialShowsWalletsHeader = true
        )
        val initialScreen = VerticalMode(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)
        viewModel.walletsStateSource.value = WalletsState(
            link = WalletsState.Link(state = LinkButtonState.Default),
            googlePay = null,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
            onGooglePayPressed = { throw AssertionError("Not expected.") },
            onLinkPressed = { throw AssertionError("Not expected.") },
        )

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeWithOutlinedIcons() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("card", "crypto", "us_bank_account"),
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}""",
            ),
            allowsDelayedPaymentMethods = true,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Lite,
        )

        val initialScreen = VerticalMode(
            FakePaymentMethodVerticalLayoutInteractor.create(
                paymentMethodMetadata = metadata,
            )
        )

        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        outlinedPaparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithSmallRowPadding() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalMode(FakePaymentMethodVerticalLayoutInteractor.create(metadata))
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        smallVerticalModeRowPaddingAppearance.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysVerticalModeListWithLargeRowPadding() {
        val metadata = PaymentMethodMetadataFactory.create()
        val initialScreen = VerticalMode(FakePaymentMethodVerticalLayoutInteractor.create(metadata))
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = false)

        largeVerticalModeRowPaddingAppearance.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @OptIn(AppearanceAPIAdditionsPreview::class)
    private data class AdditionalPaddingAppearance(
        val verticalModeRowPadding: Float,
    ) : PaparazziConfigOption {
        val appearance = Appearance.Builder()
            .verticalModeRowPadding(verticalModeRowPadding)
            .build()

        override fun initialize() {
            appearance.parseAppearance()
        }

        override fun reset() {
            PaymentSheetAppearance.DefaultAppearance.reset()
        }
    }
}
