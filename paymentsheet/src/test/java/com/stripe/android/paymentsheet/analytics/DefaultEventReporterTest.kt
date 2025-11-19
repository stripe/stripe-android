package com.stripe.android.paymentsheet.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.state.WalletsState
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Stack
import javax.inject.Provider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultEventReporterTest {

    private val paymentMethodMetadataWithTestAnalyticsMetadata = PaymentMethodMetadataFactory.create(
        analyticsMetadata = AnalyticsMetadata(
            mapOf("example_from_test" to AnalyticsMetadata.Value.SimpleBoolean(true))
        )
    )

    @Test
    fun `onInit fires event`() = runScenario {
        eventReporter.onInit()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_init")
    }

    @Test
    fun `onLoadStarted fires event`() = runScenario {
        durationProvider.startCalls.push(
            FakeDurationProvider.StartCall(
                key = DurationProvider.Key.Loading,
                reset = true,
            )
        )
        eventReporter.onLoadStarted(initializedViaCompose = true)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_load_started")
        assertThat(request.params).containsEntry("compose", true)
    }

    @Test
    fun `onLoadSucceeded fires event`() = runScenario {
        durationProvider.startCalls.push(
            FakeDurationProvider.StartCall(
                key = DurationProvider.Key.Checkout,
                reset = true,
            )
        )
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.Loading,
                duration = 1.seconds,
            )
        )
        eventReporter.onLoadSucceeded(
            paymentSelection = PaymentSelection.GooglePay,
            paymentMethodMetadata = paymentMethodMetadataWithTestAnalyticsMetadata,
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_load_succeeded")
        assertThat(request.params).containsEntry("duration", 1.0f)
        assertThat(request.params).containsEntry("selected_lpm", "google_pay")
        assertThat(request.params).containsEntry("ordered_lpms", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onLoadFailed fires event`() = runScenario {
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.Loading,
                duration = 2.seconds,
            )
        )
        val error = RuntimeException("Test error")
        eventReporter.onLoadFailed(error = error)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_load_failed")
        assertThat(request.params).containsEntry("duration", 2.0f)
        assertThat(request.params).containsEntry("error_message", "java.lang.RuntimeException")
    }

    @Test
    fun `onElementsSessionLoadFailed fires event`() = runScenario {
        val error = RuntimeException("Elements session error")
        eventReporter.onElementsSessionLoadFailed(error = error)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_elements_session_load_failed")
        assertThat(request.params).containsEntry("error_message", "java.lang.RuntimeException")
    }

    @Test
    fun `onLpmSpecFailure fires event`() = runScenario {
        eventReporter.onLpmSpecFailure(errorMessage = "Failed to serialize LPM spec")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "luxe_serialize_failure")
        assertThat(request.params).containsEntry("error_message", "Failed to serialize LPM spec")
    }

    @Test
    fun `onDismiss fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onDismiss()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_dismiss")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShowExistingPaymentOptions fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShowExistingPaymentOptions()

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.PresentedSheet())

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_sheet_savedpm_show")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShowManageSavedPaymentMethods fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShowManageSavedPaymentMethods()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_manage_savedpm_show")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShowNewPaymentOptions fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShowNewPaymentOptions()

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.PresentedSheet())

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_sheet_newpm_show")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onSelectPaymentMethod fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onSelectPaymentMethod(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.SelectedPaymentMethodType("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_carousel_payment_method_tapped")
        assertThat(request.params).containsEntry("selected_lpm", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onRemoveSavedPaymentMethod fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onRemoveSavedPaymentMethod(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.RemovedSavedPaymentMethod("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_paymentoption_removed")
        assertThat(request.params).containsEntry("selected_lpm", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentMethodFormShown fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.startCalls.push(
            FakeDurationProvider.StartCall(
                key = DurationProvider.Key.ConfirmButtonClicked,
                reset = true,
            )
        )

        eventReporter.onPaymentMethodFormShown(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.DisplayedPaymentMethodForm("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_form_shown")
        assertThat(request.params).containsEntry("selected_lpm", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentMethodFormInteraction fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onPaymentMethodFormInteraction(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.StartedInteractionWithPaymentMethodForm("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_form_interacted")
        assertThat(request.params).containsEntry("selected_lpm", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentMethodFormCompleted fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onPaymentMethodFormCompleted(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.CompletedPaymentMethodForm("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_form_completed")
        assertThat(request.params).containsEntry("selected_lpm", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardNumberCompleted fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onCardNumberCompleted()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_card_number_completed")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onDisallowedCardBrandEntered fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onDisallowedCardBrandEntered(brand = CardBrand.Visa)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_disallowed_card_brand")
        assertThat(request.params).containsEntry("brand", "visa")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPressConfirmButton fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.ConfirmButtonClicked,
                duration = 3.seconds,
            )
        )

        eventReporter.onPressConfirmButton(paymentSelection = PaymentSelection.GooglePay)

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.TappedConfirmButton("google_pay"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_confirm_button_tapped")
        assertThat(request.params).containsEntry("duration", 3.0f)
        assertThat(request.params).containsEntry("selected_lpm", "google_pay")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onAutofill fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onAutofill(type = "email")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "autofill_email")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShowEditablePaymentOption fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShowEditablePaymentOption()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_open_edit_screen")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onHideEditablePaymentOption fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onHideEditablePaymentOption()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cancel_edit_screen")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onBrandChoiceSelected fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onBrandChoiceSelected(
            source = EventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.Visa
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cbc_selected")
        assertThat(request.params).containsEntry("cbc_event_source", "edit")
        assertThat(request.params).containsEntry("selected_card_brand", "visa")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUpdatePaymentMethodSucceeded fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onUpdatePaymentMethodSucceeded(selectedBrand = CardBrand.Visa)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_update_card")
        assertThat(request.params).containsEntry("selected_card_brand", "visa")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUpdatePaymentMethodFailed fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        val error = RuntimeException("Update failed")

        eventReporter.onUpdatePaymentMethodFailed(
            selectedBrand = CardBrand.Visa,
            error = error
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_update_card_failed")
        assertThat(request.params).containsEntry("selected_card_brand", "visa")
        assertThat(request.params).containsEntry("error_message", "Update failed")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onSetAsDefaultPaymentMethodSucceeded fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onSetAsDefaultPaymentMethodSucceeded(paymentMethodType = "card")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_set_default_payment_method")
        assertThat(request.params).containsEntry("payment_method_type", "card")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onSetAsDefaultPaymentMethodFailed fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        val error = RuntimeException("Set as default failed")

        eventReporter.onSetAsDefaultPaymentMethodFailed(
            paymentMethodType = "card",
            error = error
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_set_default_payment_method_failed")
        assertThat(request.params).containsEntry("payment_method_type", "card")
        assertThat(request.params).containsEntry("error_message", "Set as default failed")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCannotProperlyReturnFromLinkAndOtherLPMs fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_cannot_return_from_link_and_lpms")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onInitiallyDisplayedPaymentMethodVisibilitySnapshot fires event with wallets and horizontal layout`() =
        runScenario {
            paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

            val walletsState = WalletsState(
                link = WalletsState.Link(LinkButtonState.Email("test@example.com")),
                googlePay = WalletsState.GooglePay(
                    buttonType = GooglePayButtonType.Pay,
                    allowCreditCards = true,
                    billingAddressParameters = null,
                ),
                walletsAllowedInHeader = listOf(WalletType.GooglePay, WalletType.Link),
                buttonsEnabled = true,
                dividerTextResource = 0,
                onGooglePayPressed = {},
                onLinkPressed = {},
            )

            eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                visiblePaymentMethods = listOf("card", "cashapp"),
                hiddenPaymentMethods = listOf("afterpay", "klarna"),
                walletsState = walletsState,
                isVerticalLayout = false,
            )

            val request = analyticsRequestExecutor.requestTurbine.awaitItem()
            assertThat(request.params).containsEntry("event", "mc_initial_displayed_payment_methods")
            assertThat(request.params).containsEntry("visible_payment_methods", "google_pay,link,card,cashapp")
            assertThat(request.params).containsEntry("hidden_payment_methods", "afterpay,klarna")
            assertThat(request.params).containsEntry("payment_method_layout", "horizontal")
            assertThat(request.params).containsEntry("example_from_test", true)
        }

    @Test
    fun `onInitiallyDisplayedPaymentMethodVisibilitySnapshot fires event with vertical layout and no wallets`() =
        runScenario {
            paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

            eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                visiblePaymentMethods = listOf("card", "us_bank_account"),
                hiddenPaymentMethods = listOf(),
                walletsState = null,
                isVerticalLayout = true,
            )

            val request = analyticsRequestExecutor.requestTurbine.awaitItem()
            assertThat(request.params).containsEntry("event", "mc_initial_displayed_payment_methods")
            assertThat(request.params).containsEntry("visible_payment_methods", "card,us_bank_account")
            assertThat(request.params).containsEntry("hidden_payment_methods", "")
            assertThat(request.params).containsEntry("payment_method_layout", "vertical")
            assertThat(request.params).containsEntry("example_from_test", true)
        }

    @Test
    fun `onInitiallyDisplayedPaymentMethodVisibilitySnapshot fires event with wallets disabled`() =
        runScenario {
            paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

            val walletsState = WalletsState(
                link = WalletsState.Link(LinkButtonState.Email("test@example.com")),
                googlePay = WalletsState.GooglePay(
                    buttonType = GooglePayButtonType.Pay,
                    allowCreditCards = true,
                    billingAddressParameters = null,
                ),
                walletsAllowedInHeader = listOf(WalletType.GooglePay, WalletType.Link),
                buttonsEnabled = false,
                dividerTextResource = 0,
                onGooglePayPressed = {},
                onLinkPressed = {},
            )

            eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                visiblePaymentMethods = listOf("card"),
                hiddenPaymentMethods = listOf(),
                walletsState = walletsState,
                isVerticalLayout = false,
            )

            val request = analyticsRequestExecutor.requestTurbine.awaitItem()
            assertThat(request.params).containsEntry("event", "mc_initial_displayed_payment_methods")
            assertThat(request.params).containsEntry("visible_payment_methods", "card")
            assertThat(request.params).containsEntry("hidden_payment_methods", "")
            assertThat(request.params).containsEntry("payment_method_layout", "horizontal")
            assertThat(request.params).containsEntry("example_from_test", true)
        }

    @Test
    fun `onShopPayWebViewLoadAttempt fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShopPayWebViewLoadAttempt()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_shoppay_webview_load_attempt")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShopPayWebViewConfirmSuccess fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShopPayWebViewConfirmSuccess()

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_shoppay_webview_confirm_success")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShopPayWebViewCancelled fires event with didReceiveECEClick true`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShopPayWebViewCancelled(didReceiveECEClick = true)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_shoppay_webview_cancelled")
        assertThat(request.params).containsEntry("did_receive_ece_click", true)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onShopPayWebViewCancelled fires event with didReceiveECEClick false`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onShopPayWebViewCancelled(didReceiveECEClick = false)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_shoppay_webview_cancelled")
        assertThat(request.params).containsEntry("did_receive_ece_click", false)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanStarted fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.startCalls.push(
            FakeDurationProvider.StartCall(
                key = DurationProvider.Key.CardScan,
                reset = true,
            )
        )

        eventReporter.onCardScanStarted(implementation = "test-value")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_scan_started")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanSucceeded fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.CardScan,
                duration = 5.seconds,
            )
        )

        eventReporter.onCardScanSucceeded(implementation = "test-value")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_success")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("duration", 5.0f)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanFailed fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.CardScan,
                duration = 3.seconds,
            )
        )
        val error = RuntimeException("Card scan failed")

        eventReporter.onCardScanFailed(implementation = "test-value", error = error)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_failed")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("duration", 3.0f)
        assertThat(request.params).containsEntry("error_message", "RuntimeException")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanFailed fires event with null error`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.CardScan,
                duration = 3.seconds,
            )
        )

        eventReporter.onCardScanFailed(implementation = "test-value", error = null)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_failed")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("duration", 3.0f)
        assertThat(request.params).containsEntry("error_message", null)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanCancelled fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.CardScan,
                duration = 2.seconds,
            )
        )

        eventReporter.onCardScanCancelled(implementation = "test-value")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_cancel")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("duration", 2.0f)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanApiCheckSucceeded fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onCardScanApiCheckSucceeded(implementation = "test-value")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_api_check_succeeded")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanApiCheckFailed fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        val error = RuntimeException("API check failed")

        eventReporter.onCardScanApiCheckFailed(implementation = "test-value", error = error)

        assertThat(userFacingLoggerTurbine.awaitItem()).isEqualTo("Card scan check failed: API check failed")

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_api_check_failed")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("error_message", "RuntimeException")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onCardScanApiCheckFailed fires event with null error`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onCardScanApiCheckFailed(implementation = "test-value", error = null)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_cardscan_api_check_failed")
        assertThat(request.params).containsEntry("implementation", "test-value")
        assertThat(request.params).containsEntry("error_message", null)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUsBankAccountFormEvent fires started event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onUsBankAccountFormEvent(USBankAccountFormViewModel.AnalyticsEvent.Started)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "stripe_android.bankaccountcollector.started")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUsBankAccountFormEvent fires finished event with completed result`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val finishedEvent = USBankAccountFormViewModel.AnalyticsEvent.Finished(
            result = "completed",
            linkAccountSessionId = "las_123",
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
        )

        eventReporter.onUsBankAccountFormEvent(finishedEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "stripe_android.bankaccountcollector.finished")
        assertThat(request.params).containsEntry("fc_sdk_result", "completed")
        assertThat(request.params).containsEntry("link_account_session_id", "las_123")
        assertThat(request.params).containsEntry("intent_id", "pi_1IRg6VCRMbs6F")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUsBankAccountFormEvent fires finished event with failed result`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val finishedEvent = USBankAccountFormViewModel.AnalyticsEvent.Finished(
            result = "failed",
            linkAccountSessionId = null,
            intent = null,
        )

        eventReporter.onUsBankAccountFormEvent(finishedEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "stripe_android.bankaccountcollector.finished")
        assertThat(request.params).containsEntry("fc_sdk_result", "failed")
        assertThat(request.params).containsEntry("link_account_session_id", null)
        assertThat(request.params).containsEntry("intent_id", null)
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onUsBankAccountFormEvent fires finished event with cancelled result`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val finishedEvent = USBankAccountFormViewModel.AnalyticsEvent.Finished(
            result = "cancelled",
            linkAccountSessionId = "las_456",
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        eventReporter.onUsBankAccountFormEvent(finishedEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "stripe_android.bankaccountcollector.finished")
        assertThat(request.params).containsEntry("fc_sdk_result", "cancelled")
        assertThat(request.params).containsEntry("link_account_session_id", "las_456")
        assertThat(request.params).containsEntry("intent_id", "pi_1F7J1aCRMbs6FrXfaJcvbxF6")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentSuccess fires event with null deferredIntentConfirmationType`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.Checkout,
                duration = 10.seconds,
            )
        )

        eventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = null,
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_payment_googlepay_success")
        assertThat(request.params).containsEntry("duration", 10.0f)
        assertThat(request.params).doesNotContainKey("deferred_intent_confirmation_type")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentSuccess fires event with Client deferredIntentConfirmationType`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.Checkout,
                duration = 8.seconds,
            )
        )

        eventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.GooglePay,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_payment_googlepay_success")
        assertThat(request.params).containsEntry("duration", 8.0f)
        assertThat(request.params).containsEntry("deferred_intent_confirmation_type", "client")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onSelectPaymentOption fires event with saved payment method`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val savedSelection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        eventReporter.onSelectPaymentOption(savedSelection)

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.SelectedSavedPaymentMethod("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_paymentoption_savedpm_select")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onSelectPaymentOption fires event with new card payment method`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val newSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        eventReporter.onSelectPaymentOption(newSelection)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_paymentoption_newpm_select")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onPaymentFailure fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)
        durationProvider.endCalls.push(
            FakeDurationProvider.EndCall(
                key = DurationProvider.Key.Checkout,
                duration = 6.seconds,
            )
        )

        val error = PaymentSheetConfirmationError.Stripe(
            cause = RuntimeException("Payment failed")
        )

        eventReporter.onPaymentFailure(
            paymentSelection = PaymentSelection.GooglePay,
            error = error,
        )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_complete_payment_googlepay_failure")
        assertThat(request.params).containsEntry("duration", 6.0f)
        assertThat(request.params).containsEntry("error_message", "java.lang.RuntimeException")
        assertThat(request.params).containsEntry("selected_lpm", "google_pay")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `onAnalyticsEvent fires event`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        val testEvent = object : AnalyticsEvent {
            override val eventName: String = "test_analytics_event"
        }

        eventReporter.onAnalyticsEvent(testEvent)

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "test_analytics_event")
        assertThat(request.params).containsEntry("example_from_test", true)
    }

    @Test
    fun `emitting event after changing paymentMethodMetadata fires event with updated data`() = runScenario {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onDismiss()

        val request1 = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request1.params).containsEntry("event", "mc_dismiss")
        assertThat(request1.params).containsEntry("example_from_test", true)
        assertThat(request1.params).doesNotContainKey("pmm2")

        paymentMethodMetadataStack.push(
            paymentMethodMetadataWithTestAnalyticsMetadata.copy(
                analyticsMetadata = AnalyticsMetadata(
                    mapOf("pmm2" to AnalyticsMetadata.Value.SimpleString("hi i'm new here."))
                )
            )
        )

        eventReporter.onDismiss()

        val request2 = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request2.params).containsEntry("event", "mc_dismiss")
        assertThat(request2.params).doesNotContainKey("example_from_test")
        assertThat(request2.params).containsEntry("pmm2", "hi i'm new here.")
    }

    @Test
    fun `event specific params override default params`() = runScenario {
        paymentMethodMetadataStack.push(
            PaymentMethodMetadataFactory.create(
                analyticsMetadata = AnalyticsMetadata(
                    mapOf("selected_lpm" to AnalyticsMetadata.Value.SimpleString("This shouldn't exist"))
                )
            )
        )

        eventReporter.onPaymentMethodFormCompleted(code = "card")

        val analyticEvent = analyticsEventTurbine.awaitItem()
        assertThat(analyticEvent).isEqualTo(AnalyticEvent.CompletedPaymentMethodForm("card"))

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_form_completed")
        assertThat(request.params).containsEntry("selected_lpm", "card")
    }

    @Test
    fun `analyticsEvent that throws should not crash`() = runScenario(throwInAnalyticsCallback = true) {
        paymentMethodMetadataStack.push(paymentMethodMetadataWithTestAnalyticsMetadata)

        eventReporter.onPaymentMethodFormCompleted(code = "card")

        assertThat(userFacingLoggerTurbine.awaitItem())
            .isEqualTo(
                "AnalyticEventCallback.onEvent() failed for event: " +
                    "CompletedPaymentMethodForm(paymentMethodType=card)"
            )

        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params).containsEntry("event", "mc_form_completed")
        assertThat(request.params).containsEntry("selected_lpm", "card")
    }

    private fun runScenario(
        throwInAnalyticsCallback: Boolean = false,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDispatcher = UnconfinedTestDispatcher()
        val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val analyticsRequestV2Executor = FakeAnalyticsRequestV2Executor()
        val paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context = context,
            publishableKey = "pk_test_123",
            defaultProductUsageTokens = setOf(""),
        )

        val durationProvider = FakeDurationProvider()

        val analyticsEventTurbine = Turbine<AnalyticEvent>()
        val analyticsEventCallback = AnalyticEventCallback { event ->
            if (throwInAnalyticsCallback) {
                throw IllegalStateException("Bad implementation.")
            }
            analyticsEventTurbine.add(event)
        }
        val analyticEventCallbackProvider = Provider<AnalyticEventCallback?> { analyticsEventCallback }

        val userFacingLoggerTurbine = Turbine<String>()
        val logger = object : UserFacingLogger {
            override fun logWarningWithoutPii(message: String) {
                userFacingLoggerTurbine.add(message)
            }
        }

        val paymentMethodMetadataStack = Stack<PaymentMethodMetadata?>()

        val eventReporter = DefaultEventReporter(
            context = context,
            mode = EventReporter.Mode.Complete,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsRequestV2Executor,
            paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
            durationProvider = durationProvider,
            analyticEventCallbackProvider = analyticEventCallbackProvider,
            workContext = testDispatcher,
            logger = logger,
            paymentMethodMetadataProvider = { paymentMethodMetadataStack.pop() },
        )

        val scenario = Scenario(
            eventReporter = eventReporter,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsRequestV2Executor,
            durationProvider = durationProvider,
            paymentMethodMetadataStack = paymentMethodMetadataStack,
            analyticsEventTurbine = analyticsEventTurbine,
            userFacingLoggerTurbine = userFacingLoggerTurbine,
        )

        block(scenario)

        analyticsRequestExecutor.validate()
        analyticsRequestV2Executor.validate()
        durationProvider.validate()
        assertThat(paymentMethodMetadataStack).isEmpty()
        userFacingLoggerTurbine.ensureAllEventsConsumed()
        analyticsEventTurbine.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val eventReporter: DefaultEventReporter,
        val analyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val analyticsRequestV2Executor: FakeAnalyticsRequestV2Executor,
        val durationProvider: FakeDurationProvider,
        val paymentMethodMetadataStack: Stack<PaymentMethodMetadata?>,
        val analyticsEventTurbine: Turbine<AnalyticEvent>,
        val userFacingLoggerTurbine: Turbine<String>,
    )

    private class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        private val _requestTurbine = Turbine<AnalyticsRequest>()
        val requestTurbine: ReceiveTurbine<AnalyticsRequest> = _requestTurbine

        override fun executeAsync(request: AnalyticsRequest) {
            _requestTurbine.add(request)
        }

        fun validate() {
            _requestTurbine.ensureAllEventsConsumed()
        }
    }

    private class FakeDurationProvider : DurationProvider {
        val startCalls: Stack<StartCall> = Stack()
        val endCalls: Stack<EndCall> = Stack()

        override fun start(key: DurationProvider.Key, reset: Boolean) {
            val call = startCalls.pop()
            assertThat(call.key).isEqualTo(key)
            assertThat(call.reset).isEqualTo(reset)
        }

        override fun end(key: DurationProvider.Key): Duration {
            val call = endCalls.pop()
            assertThat(call.key).isEqualTo(key)
            return call.duration
        }

        fun validate() {
            assertThat(startCalls).isEmpty()
            assertThat(endCalls).isEmpty()
        }

        data class StartCall(val key: DurationProvider.Key, val reset: Boolean)
        data class EndCall(val key: DurationProvider.Key, val duration: Duration)
    }
}
