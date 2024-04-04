package com.stripe.android.payments.core.analytics

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.utils.filterNotNullValues
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ErrorReporter {

    fun report(
        errorEvent: ErrorEvent,
        stripeException: StripeException? = null,
        additionalNonPiiParams: Map<String, String> = emptyMap(),
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Prefer using an injected version of [ErrorReporter].
         *
         * This should only be used if you don't already have access to a dagger component.
         */
        fun createFallbackInstance(
            context: Context,
            productUsage: Set<String> = emptySet(),
        ): ErrorReporter {
            return DaggerDefaultErrorReporterComponent
                .builder()
                .context(context.applicationContext)
                .productUsage(productUsage)
                .build()
                .errorReporter
        }

        fun getAdditionalParamsFromError(error: Throwable): Map<String, String> {
            return when (error) {
                is StripeException -> getAdditionalParamsFromStripeException(error)
                else -> getAdditionalParamsFromStripeException(StripeException.create(error))
            }
        }

        fun getAdditionalParamsFromStripeException(stripeException: StripeException): Map<String, String> {
            val statusCode =
                if (stripeException.statusCode == StripeException.DEFAULT_STATUS_CODE) {
                    null
                } else {
                    stripeException.statusCode
                }
            return mapOf(
                "analytics_value" to stripeException.analyticsValue(),
                "status_code" to statusCode?.toString(),
                "request_id" to stripeException.requestId,
                "error_type" to stripeException.stripeError?.type,
                "error_code" to stripeException.stripeError?.code,
            ).filterNotNullValues()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    interface ErrorEvent : AnalyticsEvent

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ExpectedErrorEvent(override val eventName: String) : ErrorEvent {
        AUTH_WEB_VIEW_FAILURE(
            eventName = "payments.auth_web_view.failure"
        ),
        AUTH_WEB_VIEW_NULL_ARGS(
            eventName = "payments.auth_web_view.null_args"
        ),
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        ),
        CUSTOMER_SHEET_LOAD_FAILURE(
            eventName = "elements.customer_sheet.load_failure"
        ),
        PLACES_FIND_AUTOCOMPLETE_ERROR(
            eventName = "address_element.find_autocomplete.error"
        ),
        PLACES_FETCH_PLACE_ERROR(
            eventName = "address_element.fetch_place.error"
        ),
        LINK_CREATE_CARD_FAILURE(
            eventName = "link.create_new_card.create_payment_details_failure"
        ),
        LINK_SHARE_CARD_FAILURE(
            eventName = "link.create_new_card.share_payment_details_failure"
        ),
        LINK_LOG_OUT_FAILURE(
            eventName = "link.log_out.failure"
        ),
        PAYMENT_LAUNCHER_CONFIRMATION_NULL_ARGS(
            eventName = "payments.paymentlauncherconfirmation.null_args"
        ),
        BROWSER_LAUNCHER_ACTIVITY_NOT_FOUND(
            eventName = "payments.browserlauncher.activity_not_found"
        ),
        BROWSER_LAUNCHER_NULL_ARGS(
            eventName = "payments.browserlauncher.null_args"
        ),
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class UnexpectedErrorEvent(val partialEventName: String) : ErrorEvent {
        AUTH_WEB_VIEW_BLANK_CLIENT_SECRET(
            partialEventName = "payments.auth_web_view.blank_client_secret"
        ),
        MISSING_CARDSCAN_DEPENDENCY(
            partialEventName = "cardscan.missing_dependency"
        ),
        MISSING_HOSTED_VOUCHER_URL(
            partialEventName = "payments.missing_hosted_voucher_url"
        ),
        MISSING_POLLING_AUTHENTICATOR(
            partialEventName = "payments.missing_polling_authenticator"
        ),
        LINK_INVALID_SESSION_STATE(
            partialEventName = "link.signup.failure.invalidSessionState"
        ),
        GOOGLE_PAY_UNEXPECTED_CONFIRM_RESULT(
            partialEventName = "google_pay.confirm.unexpected_result"
        ),
        GOOGLE_PAY_MISSING_INTENT_DATA(
            partialEventName = "google_pay.on_result.missing_data"
        ),
        FIND_AUTOCOMPLETE_PREDICTIONS_WITHOUT_DEPENDENCY(
            partialEventName = "address_element.find_autocomplete.without_dependency"
        ),
        FETCH_PLACE_WITHOUT_DEPENDENCY(
            partialEventName = "address_element.fetch_place.without_dependency",
        ),
        LINK_ATTACH_CARD_WITH_NULL_ACCOUNT(
            partialEventName = "link.create_new_card.missing_link_account"
        ),
        PAYMENT_SHEET_AUTHENTICATORS_NOT_FOUND(
            partialEventName = "paymentsheet.authenticators.not_found"
        ),
        ;

        override val eventName: String
            get() = "unexpected.$partialEventName"
    }
}

@Component(modules = [DefaultErrorReporterModule::class])
internal interface DefaultErrorReporterComponent {
    val errorReporter: ErrorReporter

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): DefaultErrorReporterComponent
    }
}

@Module
internal interface DefaultErrorReporterModule {
    @Binds
    fun bindRealErrorReporter(
        errorReporter: RealErrorReporter
    ): ErrorReporter

    @Binds
    fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    @Binds
    fun providePaymentAnalyticsRequestFactory(
        requestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        fun provideLogger(): Logger {
            return Logger.getInstance(BuildConfig.DEBUG)
        }

        @Provides
        @IOContext
        fun provideIoContext(): CoroutineContext {
            return Dispatchers.IO
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(context: Context): () -> String {
            return { PaymentConfiguration.getInstance(context).publishableKey }
        }
    }
}
