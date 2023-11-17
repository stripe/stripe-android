package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        PaymentLauncherModule::class,
        StripeRepositoryModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentLauncherViewModelFactoryComponent {

    val viewModelSubcomponentBuilder: PaymentLauncherViewModelSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun enableLogging(@Named(ENABLE_LOGGING) enableLogging: Boolean): Builder

        @BindsInstance
        fun publishableKeyProvider(@Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String): Builder

        @BindsInstance
        fun stripeAccountIdProvider(@Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun includePaymentSheetAuthenticators(
            @Named(INCLUDE_PAYMENT_SHEET_AUTHENTICATORS) includePaymentSheetAuthenticators: Boolean
        ): Builder

        fun build(): PaymentLauncherViewModelFactoryComponent
    }
}
