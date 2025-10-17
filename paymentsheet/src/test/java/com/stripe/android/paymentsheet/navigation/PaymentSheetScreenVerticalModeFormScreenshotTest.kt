package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.VerticalModeForm
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.verticalmode.FakeVerticalModeFormInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenVerticalModeFormScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun displaysCard() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeVerticalModeFormInteractor.create(
            paymentMethodCode = "card",
            metadata = metadata,
        )
        val initialScreen = VerticalModeForm(interactor, showsWalletHeader = true)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        viewModel.walletsStateSource.value = WalletsState.create(
            isLinkAvailable = true,
            linkEmail = "email@email.com",
            isGooglePayReady = false,
            googlePayButtonType = GooglePayButtonType.Checkout,
            buttonsEnabled = true,
            paymentMethodTypes = listOf("card"),
            onGooglePayPressed = {},
            onLinkPressed = {},
            isSetupIntent = false,
            walletsAllowedInHeader = listOf(WalletType.GooglePay, WalletType.Link),
            googlePayLauncherConfig = null,
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }
}
