package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.VerticalMode
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
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
            link = WalletsState.Link(
                email = null,
            ),
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
}
