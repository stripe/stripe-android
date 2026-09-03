package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(AddressAutocompletePreview::class)
internal class EmbeddedSheetViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `payment options arguments use the embedded payment options runtime`() = runScenario(
        args = SheetActivityArgs.PaymentOptions(PAYMENT_OPTIONS_CONTRACT_ARGS),
    ) {
        val ready = viewModel.state.value as EmbeddedSheetViewModel.State.Ready

        assertThat(ready.args.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        assertThat(ready.args.configuration.formSheetAction)
            .isEqualTo(EmbeddedPaymentElement.FormSheetAction.Continue)
        assertThat(ready.args.paymentMethodMetadata)
            .isEqualTo(PAYMENT_OPTIONS_CONTRACT_ARGS.state.paymentMethodMetadata)
        assertThat(ready.args.activityConfiguration)
            .isEqualTo(
                EmbeddedActivityArgs.ActivityConfiguration.PaymentOptions(
                    initialSelection = PAYMENT_OPTIONS_CONTRACT_ARGS.state.paymentSelection,
                    initialLinkAccount = PAYMENT_OPTIONS_CONTRACT_ARGS.linkAccountInfo,
                    productUsageTokens = PAYMENT_OPTIONS_CONTRACT_ARGS.productUsage,
                )
            )
    }

    @Test
    @Suppress("DEPRECATION")
    fun `payment sheet load uses the embedded complete runtime`() {
        val promotions = FakePaymentMethodMessagePromotionsHelper.promotions
        val loader = FakePaymentElementLoader(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            paymentSelection = PaymentSelection.GooglePay,
        )
        val paymentSheetArgs = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                .googlePlacesApiKey("places_key")
                .build(),
        )

        runScenario(
            args = SheetActivityArgs.PaymentSheet(paymentSheetArgs),
            loaderComponent = loaderComponent(loader, promotions),
        ) {
            val ready = viewModel.state.value as EmbeddedSheetViewModel.State.Ready

            assertThat(ready.args.launchMode).isEqualTo(EmbeddedLaunchMode.Complete)
            assertThat(ready.args.configuration.formSheetAction)
                .isEqualTo(EmbeddedPaymentElement.FormSheetAction.Confirm)
            assertThat(ready.args.selection).isEqualTo(PaymentSelection.GooglePay)
            assertThat(ready.args.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            assertThat(ready.args.promotions).isEqualTo(promotions)
            assertThat(ready.args.configuration.googlePlacesApiKey).isEqualTo("places_key")
            assertThat(ready.args.activityConfiguration)
                .isEqualTo(EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet)
            assertThat(loader.lastIntegrationConfiguration)
                .isInstanceOf(PaymentElementLoader.Configuration.PaymentSheet::class.java)
        }
    }

    @Test
    fun `payment sheet load failure is exposed to the activity`() = runScenario(
        args = SheetActivityArgs.PaymentSheet(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY),
        loaderComponent = loaderComponent(FakePaymentElementLoader(shouldFail = true), null),
    ) {
        val failed = viewModel.state.value as EmbeddedSheetViewModel.State.Failed

        assertThat(failed.error).hasMessageThat().isEqualTo("oh no")
    }

    private fun runScenario(
        args: SheetActivityArgs,
        loaderComponent: SheetActivityLoaderComponent? = null,
        block: Scenario.() -> Unit,
    ) {
        val component = mock<EmbeddedSheetComponent> {
            on { selectionHolder } doReturn mock()
            on { customerStateHolder } doReturn mock()
        }
        val viewModel = viewModelStoreRule.track(
            EmbeddedSheetViewModel(
                args = args,
                loaderComponent = loaderComponent,
                savedStateHandle = SavedStateHandle(),
                customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
                componentFactory = { component },
            )
        )

        block(Scenario(viewModel))
    }

    private data class Scenario(
        val viewModel: EmbeddedSheetViewModel,
    )

    private fun loaderComponent(
        loader: PaymentElementLoader,
        promotions: List<PaymentMethodMessagePromotion>?,
    ): SheetActivityLoaderComponent {
        return object : SheetActivityLoaderComponent {
            override val paymentElementLoader: PaymentElementLoader = loader
            override val promotionsHelper: PaymentMethodMessagePromotionsHelper =
                FakePaymentMethodMessagePromotionsHelper(promotions)
            override val eventReporter: EventReporter = FakeEventReporter()
        }
    }
}
