package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.asEmbeddedActivityArgs
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test

@OptIn(AddressAutocompletePreview::class)
internal class SheetActivityViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

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
            args = paymentSheetArgs.asEmbeddedActivityArgs(),
            loaderComponent = loaderComponent(loader, promotions),
        ) {
            val ready = viewModel.state.value as SheetActivityViewModel.State.Ready

            assertThat(ready.args.launchMode).isEqualTo(EmbeddedLaunchMode.Complete)
            assertThat(ready.args.configuration.formSheetAction)
                .isEqualTo(EmbeddedPaymentElement.FormSheetAction.Confirm)
            assertThat(ready.args.selection).isEqualTo(PaymentSelection.GooglePay)
            assertThat(ready.args.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            assertThat(ready.args.promotions).isEqualTo(promotions)
            assertThat(ready.args.configuration.googlePlacesApiKey).isEqualTo("places_key")
            assertThat(ready.args.activityConfiguration)
                .isEqualTo(EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet(paymentSheetArgs))
            assertThat(ready.args.presentationState)
                .isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
            assertThat(loader.lastIntegrationConfiguration)
                .isInstanceOf(PaymentElementLoader.Configuration.PaymentSheet::class.java)
        }
    }

    @Test
    fun `payment sheet load failure is exposed to the activity`() = runScenario(
        args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.asEmbeddedActivityArgs(),
        loaderComponent = loaderComponent(FakePaymentElementLoader(shouldFail = true), null),
    ) {
        val failed = viewModel.state.value as SheetActivityViewModel.State.Failed

        assertThat(failed.error).hasMessageThat().isEqualTo("oh no")
    }

    @Test
    fun `payment sheet validation failure is exposed to the activity`() {
        val validationError = PaymentSheetLoadingException.MissingAmountOrCurrency

        runScenario(
            args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.asEmbeddedActivityArgs(),
            loaderComponent = loaderComponent(
                FakePaymentElementLoader(validationError = validationError),
                promotions = null,
            ),
        ) {
            val failed = viewModel.state.value as SheetActivityViewModel.State.Failed

            assertThat(failed.error).isSameInstanceAs(validationError)
        }
    }

    private fun runScenario(
        args: EmbeddedActivityArgs,
        loaderComponent: SheetActivityLoaderComponent,
        block: Scenario.() -> Unit,
    ) {
        val viewModel = viewModelStoreRule.track(
            SheetActivityViewModel(
                args = args,
                loaderComponent = loaderComponent,
                savedStateHandle = SavedStateHandle(),
                customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            )
        )

        block(Scenario(viewModel))
    }

    private data class Scenario(
        val viewModel: SheetActivityViewModel,
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
