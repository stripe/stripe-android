package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.authentication.DefaultPaymentAuthenticatorRegistry
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [Component] for com.stripe.android.payments.core.authentication.
 *
 * It holds the dagger graph for [DefaultPaymentAuthenticatorRegistry], with
 * more dependencies daggerized and a higher level [Component]s created, this class will be merged
 * into it.
 */
@Singleton
@Component(
    modules = [
        AuthenticationModule::class,
        Stripe3DSAuthenticatorModule::class,
        WeChatPayAuthenticatorModule::class,
        CoreCommonModule::class,
        StripeRepositoryModule::class,
    ]
)
internal interface AuthenticationComponent {
    val registry: DefaultPaymentAuthenticatorRegistry

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun analyticsRequestFactory(paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun workContext(@IOContext workContext: CoroutineContext): Builder

        @BindsInstance
        fun uiContext(@UIContext uiContext: CoroutineContext): Builder

        @BindsInstance
        fun threeDs1IntentReturnUrlMap(
            threeDs1IntentReturnUrlMap: MutableMap<String, String>
        ): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun isInstantApp(@Named(IS_INSTANT_APP) isInstantApp: Boolean): Builder

        @BindsInstance
        fun includePaymentSheetAuthenticators(
            @Named(INCLUDE_PAYMENT_SHEET_AUTHENTICATORS) includePaymentSheetAuthenticators: Boolean
        ): Builder

        fun build(): AuthenticationComponent
    }
}
