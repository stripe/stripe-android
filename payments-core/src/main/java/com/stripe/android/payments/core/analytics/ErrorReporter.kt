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
            productUsage: Set<String>,
        ): ErrorReporter {
            return DaggerDefaultErrorReporterComponent
                .builder()
                .context(context.applicationContext)
                .productUsage(productUsage)
                .build()
                .errorReporter
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class ErrorEvent(override val eventName: String) : AnalyticsEvent {
        GET_SAVED_PAYMENT_METHODS_FAILURE(
            eventName = "elements.customer_repository.get_saved_payment_methods_failure"
        ),
        MISSING_CARDSCAN_DEPENDENCY(
            eventName = "unexpected.cardscan.missing_dependency"
        ),
        MISSING_HOSTED_VOUCHER_URL(
            eventName = "unexpected.payments.missing_hosted_voucher_url"
        ),
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        fun getAdditionalParamsFromError(error: Throwable): Map<String, String> {
            return when (error) {
                is StripeException -> getAdditionalParamsFromStripeException(error)
                else -> getAdditionalParamsFromStripeException(StripeException.create(error))
            }
        }
        fun getAdditionalParamsFromStripeException(stripeException: StripeException): Map<String, String> {
            return mapOf(
                "analytics_value" to stripeException.analyticsValue(),
                "status_code" to stripeException.statusCode.toString(),
                "request_id" to stripeException.requestId,
                "error_type" to stripeException.stripeError?.type,
                "error_code" to stripeException.stripeError?.code,
            ).filterNotNullValues()
        }

        private fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
            mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    }
}
