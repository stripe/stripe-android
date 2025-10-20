package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.customersheet.analytics.CustomerSheetEvent
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_CANCELED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_CARD_NUMBER_COMPLETED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_HIDE_EDITABLE_PAYMENT_OPTION
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_HIDE_PAYMENT_OPTION_BRANDS
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_INIT_WITH_CUSTOMER_ADAPTER
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_INIT_WITH_CUSTOMER_SESSION
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_LOAD_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_LOAD_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_PAYMENT_METHOD_SELECTED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_DONE_TAPPED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_EDIT_TAPPED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_REMOVE_PM_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_REMOVE_PM_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SHOW_EDITABLE_PAYMENT_OPTION
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SHOW_PAYMENT_OPTION_BRANDS
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_UPDATE_PAYMENT_METHOD
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_UPDATE_PAYMENT_METHOD_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_ERROR_MESSAGE
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_HAS_DEFAULT_PAYMENT_METHOD
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_PAYMENT_METHOD_TYPE
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_SELECTED_LPM
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_SYNC_DEFAULT_ENABLED
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.analytics.DefaultCustomerSheetEventReporter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CustomerSheetEventReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        publishableKeyProvider = { ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY },
        networkTypeProvider = { "5G" },
    )
    private val eventReporter = DefaultCustomerSheetEventReporter(
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = analyticsRequestFactory,
        workContext = testDispatcher,
    )

    @Test
    fun `onInit should fire analytics request with expected event value`() {
        eventReporter.onInit(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerAdapter,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_INIT_WITH_CUSTOMER_ADAPTER
            }
        )
    }

    @Test
    fun `onInit with customer session should fire analytics request with expected event value`() {
        eventReporter.onInit(
            configuration = CustomerSheetFixtures.MINIMUM_CONFIG,
            integrationType = CustomerSheetIntegration.Type.CustomerSession,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_INIT_WITH_CUSTOMER_SESSION
            }
        )
    }

    @Test
    fun `onLoadSucceeded should fire analytics request with expected event value`() {
        val customerSheetSession = CustomerSheetFixtures.createCustomerSheetSession(
            hasCustomerSession = true,
            isPaymentMethodSyncDefaultEnabled = true,
            hasDefaultPaymentMethod = true
        )

        eventReporter.onLoadSucceeded(customerSheetSession)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_LOAD_SUCCEEDED &&
                    req.params[FIELD_SYNC_DEFAULT_ENABLED] == true &&
                    req.params[FIELD_HAS_DEFAULT_PAYMENT_METHOD] == true
            }
        )
    }

    @Test
    fun `onLoadSucceeded with sync disabled should not include has_default_payment_method`() {
        val customerSheetSession = CustomerSheetFixtures.createCustomerSheetSession(
            hasCustomerSession = true,
            isPaymentMethodSyncDefaultEnabled = false,
            hasDefaultPaymentMethod = true
        )

        eventReporter.onLoadSucceeded(customerSheetSession)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_LOAD_SUCCEEDED &&
                    req.params[FIELD_SYNC_DEFAULT_ENABLED] == false &&
                    !req.params.containsKey(FIELD_HAS_DEFAULT_PAYMENT_METHOD)
            }
        )
    }

    @Test
    fun `onLoadFailed should fire analytics request with expected event value`() {
        val error = RuntimeException("Test error message")

        eventReporter.onLoadFailed(error)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_LOAD_FAILED &&
                    req.params[FIELD_ERROR_MESSAGE] == "Test error message"
            }
        )
    }

    @Test
    fun `onScreenPresented should fire analytics request with expected event value`() {
        eventReporter.onScreenPresented(screen = CustomerSheetEventReporter.Screen.AddPaymentMethod)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED
            }
        )

        eventReporter.onScreenPresented(screen = CustomerSheetEventReporter.Screen.SelectPaymentMethod)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED
            }
        )

        eventReporter.onScreenPresented(screen = CustomerSheetEventReporter.Screen.EditPaymentMethod)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SHOW_EDITABLE_PAYMENT_OPTION
            }
        )
    }

    @Test
    fun `onScreenHidden should fire analytics request with expected event value`() {
        eventReporter.onScreenHidden(screen = CustomerSheetEventReporter.Screen.EditPaymentMethod)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_HIDE_EDITABLE_PAYMENT_OPTION
            }
        )
    }

    @Test
    fun `onSelectPaymentMethod should fire analytics request with expected event value`() {
        eventReporter.onPaymentMethodSelected(PaymentMethod.Type.Card.code)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_PAYMENT_METHOD_SELECTED &&
                    req.params[FIELD_SELECTED_LPM] == PaymentMethod.Type.Card.code
            }
        )
    }

    @Test
    fun `onConfirmPaymentMethodSucceeded should fire analytics request with expected event value`() {
        eventReporter.onConfirmPaymentMethodSucceeded(
            type = PaymentMethod.Type.Card.code,
            syncDefaultEnabled = null
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code
            }
        )
    }

    @Test
    fun `onConfirmPaymentMethodSucceeded with syncDefaultEnabled should fire analytics request with expected value`() {
        eventReporter.onConfirmPaymentMethodSucceeded(
            type = PaymentMethod.Type.Card.code,
            syncDefaultEnabled = true
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code &&
                    req.params[FIELD_SYNC_DEFAULT_ENABLED] == true
            }
        )
    }

    @Test
    fun `onConfirmPaymentMethodFailed should fire analytics request with expected event value`() {
        eventReporter.onConfirmPaymentMethodFailed(
            type = PaymentMethod.Type.Card.code,
            syncDefaultEnabled = null
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code
            }
        )
    }

    @Test
    fun `onConfirmPaymentMethodFailed with syncDefaultEnabled should fire analytics request with expected value`() {
        eventReporter.onConfirmPaymentMethodFailed(
            type = PaymentMethod.Type.Card.code,
            syncDefaultEnabled = true
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code &&
                    req.params[FIELD_SYNC_DEFAULT_ENABLED] == true
            }
        )
    }

    @Test
    fun `onEditTapped should fire analytics request with expected event value`() {
        eventReporter.onEditTapped()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_EDIT_TAPPED
            }
        )
    }

    @Test
    fun `onEditCompleted should fire analytics request with expected event value`() {
        eventReporter.onEditCompleted()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_DONE_TAPPED
            }
        )
    }

    @Test
    fun `onRemovePaymentMethodSucceeded should fire analytics request with expected event value`() {
        eventReporter.onRemovePaymentMethodSucceeded()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_REMOVE_PM_SUCCEEDED
            }
        )
    }

    @Test
    fun `onRemovePaymentMethodFailed should fire analytics request with expected event value`() {
        eventReporter.onRemovePaymentMethodFailed()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_REMOVE_PM_FAILED
            }
        )
    }

    @Test
    fun `onAttachPaymentMethodSucceeded should fire analytics request with expected event value`() {
        eventReporter.onAttachPaymentMethodSucceeded(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_SUCCEEDED
            }
        )

        eventReporter.onAttachPaymentMethodSucceeded(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_SUCCEEDED
            }
        )
    }

    @Test
    fun `onAttachPaymentMethodCanceled should fire analytics request with expected event value`() {
        eventReporter.onAttachPaymentMethodCanceled(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_CANCELED
            }
        )

        eventReporter.onAttachPaymentMethodCanceled(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
        )
        verifyNoMoreInteractions(analyticsRequestExecutor)
    }

    @Test
    fun `onAttachPaymentMethodFailed should fire analytics request with expected event value`() {
        eventReporter.onAttachPaymentMethodFailed(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_FAILED
            }
        )

        eventReporter.onAttachPaymentMethodFailed(
            style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_FAILED
            }
        )
    }

    @Test
    fun `onShowPaymentOptionBrands() should fire analytics request with expected event value`() {
        eventReporter.onShowPaymentOptionBrands(
            source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.Visa
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SHOW_PAYMENT_OPTION_BRANDS &&
                    req.params["cbc_event_source"] == "edit" &&
                    req.params["selected_card_brand"] == "visa"
            }
        )
    }

    @Test
    fun `onHidePaymentOptionBrands() should fire analytics request with expected event value`() {
        eventReporter.onHidePaymentOptionBrands(
            source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.CartesBancaires,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_HIDE_PAYMENT_OPTION_BRANDS &&
                    req.params["cbc_event_source"] == "edit" &&
                    req.params["selected_card_brand"] == "cartes_bancaires"
            }
        )
    }

    @Test
    fun `onUpdatePaymentMethodSucceeded() should fire analytics request with expected event value`() {
        eventReporter.onUpdatePaymentMethodSucceeded(
            selectedBrand = CardBrand.CartesBancaires,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_UPDATE_PAYMENT_METHOD &&
                    req.params["selected_card_brand"] == "cartes_bancaires"
            }
        )
    }

    @Test
    fun `onUpdatePaymentMethodFailed() should fire analytics request with expected event value`() {
        eventReporter.onUpdatePaymentMethodFailed(
            selectedBrand = CardBrand.CartesBancaires,
            error = Exception("No network available!")
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_UPDATE_PAYMENT_METHOD_FAILED &&
                    req.params["selected_card_brand"] == "cartes_bancaires" &&
                    req.params["error_message"] == "No network available!"
            }
        )
    }

    @Test
    fun `onCardNumberCompleted() should fire analytics request with expected event value`() {
        eventReporter.onCardNumberCompleted()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_CARD_NUMBER_COMPLETED
            }
        )
    }

    @Test
    fun `onDisallowedCardBrandEntered(brand) should fire analytics request with expected event value`() {
        eventReporter.onDisallowedCardBrandEntered(CardBrand.AmericanExpress)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_disallowed_card_brand" &&
                    req.params["brand"] == "amex"
            }
        )
    }

    @Test
    fun `onAnalyticsEvent() should fire analytics request with expected event value`() {
        eventReporter.onAnalyticsEvent(PaymentAnalyticsEvent.FileCreate)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PaymentAnalyticsEvent.FileCreate.eventName
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanStarted should fire analytics request with expected event value`() {
        val event = CustomerSheetEvent.CardScanStarted("google_pay")
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_scan_started" &&
                    req.params["implementation"] == "google_pay"
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanSucceeded should fire analytics request with expected event value`() {
        val event = CustomerSheetEvent.CardScanSucceeded("google_pay", 2.seconds)
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_success" &&
                    req.params["implementation"] == "google_pay" &&
                    req.params["duration"] == 2f
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanFailed should fire analytics request with expected event value`() {
        val testError = IllegalStateException("Card scan failed")
        val event = CustomerSheetEvent.CardScanFailed("google_pay", 1.seconds, testError)
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_failed" &&
                    req.params["implementation"] == "google_pay" &&
                    req.params["duration"] == 1f &&
                    req.params["error_message"] == "IllegalStateException"
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanFailed and null error should fire analytics request with null error_message`() {
        val event = CustomerSheetEvent.CardScanFailed("google_pay", 1.seconds, null)
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_failed" &&
                    req.params["implementation"] == "google_pay" &&
                    req.params["duration"] == 1f &&
                    req.params["error_message"] == null
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanCancelled should fire analytics request with expected event value`() {
        val event = CustomerSheetEvent.CardScanCancelled("google_pay", 3.seconds)
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_cancel" &&
                    req.params["implementation"] == "google_pay" &&
                    req.params["duration"] == 3f
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanApiCheckSucceeded should fire analytics request with expected event value`() {
        val event = CustomerSheetEvent.CardScanApiCheckSucceeded("google_pay")
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_api_check_succeeded" &&
                    req.params["implementation"] == "google_pay"
            }
        )
    }

    @Test
    fun `onCardScanEvent() with CardScanApiCheckFailed should fire analytics request with expected event value`() {
        val testError = IllegalStateException("API not available")
        val event = CustomerSheetEvent.CardScanApiCheckFailed("google_pay", testError)
        eventReporter.onCardScanEvent(event)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_api_check_failed" &&
                    req.params["implementation"] == "google_pay" &&
                    req.params["error_message"] == "IllegalStateException"
            }
        )
    }

    @Test
    fun `onCardScanEvent() should work with different implementation names`() {
        eventReporter.onCardScanEvent(CustomerSheetEvent.CardScanStarted("bouncer"))
        eventReporter.onCardScanEvent(CustomerSheetEvent.CardScanSucceeded("stripe", 1.seconds))
        eventReporter.onCardScanEvent(CustomerSheetEvent.CardScanFailed("custom", 2.seconds, null))
        eventReporter.onCardScanEvent(CustomerSheetEvent.CardScanCancelled("test", 1.seconds))
        eventReporter.onCardScanEvent(CustomerSheetEvent.CardScanApiCheckSucceeded("ml_kit"))

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_scan_started" &&
                    req.params["implementation"] == "bouncer"
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_success" &&
                    req.params["implementation"] == "stripe"
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_failed" &&
                    req.params["implementation"] == "custom"
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_cancel" &&
                    req.params["implementation"] == "test"
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "cs_cardscan_api_check_succeeded" &&
                    req.params["implementation"] == "ml_kit"
            }
        )
    }
}
