package com.stripe.android.customersheet

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_CANCELED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_DONE_TAPPED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_EDIT_TAPPED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_REMOVE_PM_FAILED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_REMOVE_PM_SUCCEEDED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED
import com.stripe.android.customersheet.analytics.CustomerSheetEvent.Companion.FIELD_PAYMENT_METHOD_TYPE
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.analytics.DefaultCustomerSheetEventReporter
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner

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
    }

    @Test
    fun `onConfirmPaymentMethodSucceeded should fire analytics request with expected event value`() {
        eventReporter.onConfirmPaymentMethodSucceeded(PaymentMethod.Type.Card)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code
            }
        )
    }

    @Test
    fun `onConfirmPaymentMethodFailed should fire analytics request with expected event value`() {
        eventReporter.onConfirmPaymentMethodFailed(PaymentMethod.Type.Card)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED &&
                    req.params[FIELD_PAYMENT_METHOD_TYPE] == PaymentMethod.Type.Card.code
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
}
